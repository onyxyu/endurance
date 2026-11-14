package io.github.onyxyu.endurance.server.concurrent;

import io.github.onyxyu.endurance.api.concurrent.TaskDispatcher;
import io.github.onyxyu.endurance.api.concurrent.TaskPool;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

/**
 * 有界、可观测的任务提交入口，返回值可取消；超时包括排队时间。
 */
@Component
public class Tasks implements TaskDispatcher {

    private final ThreadPoolTaskExecutor ioThreadPoolTaskExecutor;
    private final ThreadPoolTaskExecutor cpuThreadPoolTaskExecutor;
    private final ThreadPoolTaskScheduler taskTimeoutScheduler;
    private final ExecutorProperties executorProperties;
    private final ObservationRegistry observationRegistry;
    private final Clock applicationClock;
    private final Set<CompletableFuture<?>> pendingTaskFutures = ConcurrentHashMap.newKeySet();
    private boolean acceptingTasks = true;

    public Tasks(@Qualifier("ioExecutor") ThreadPoolTaskExecutor ioThreadPoolTaskExecutor,
                 @Qualifier("cpuExecutor") ThreadPoolTaskExecutor cpuThreadPoolTaskExecutor,
                 ThreadPoolTaskScheduler taskTimeoutScheduler,
                 ExecutorProperties executorProperties,
                 ObservationRegistry observationRegistry,
                 Clock applicationClock) {
        this.ioThreadPoolTaskExecutor = ioThreadPoolTaskExecutor;
        this.cpuThreadPoolTaskExecutor = cpuThreadPoolTaskExecutor;
        this.taskTimeoutScheduler = taskTimeoutScheduler;
        this.executorProperties = executorProperties;
        this.observationRegistry = observationRegistry;
        this.applicationClock = applicationClock;
    }

    @Override
    public <T> CompletableFuture<T> submit(TaskPool taskPool, Callable<T> taskCallable) {
        return submit(taskPool, taskCallable, Duration.ofSeconds(executorProperties.taskTimeoutSeconds()));
    }

    @Override
    public CompletableFuture<Void> execute(TaskPool taskPool, Runnable taskRunnable) {
        return submit(taskPool, () -> {
            taskRunnable.run();
            return null;
        });
    }

    @Override
    public synchronized <T> CompletableFuture<T> submit(TaskPool taskPool,
                                                        Callable<T> taskCallable,
                                                        Duration taskTimeout) {
        // 校验提交条件
        Objects.requireNonNull(taskPool);
        Objects.requireNonNull(taskCallable);
        Objects.requireNonNull(taskTimeout);
        if (taskTimeout.isNegative() || taskTimeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        if (!acceptingTasks) {
            throw new RejectedExecutionException("Application is shutting down");
        }

        // 向下传递取消
        CompletableFuture<T> taskResult = new CompletableFuture<T>();
        AtomicReference<Future<?>> submittedTaskReference = new AtomicReference<Future<?>>();
        pendingTaskFutures.add(taskResult);

        taskResult.whenComplete((taskValue, taskFailure) -> {
            pendingTaskFutures.remove(taskResult);
            if (taskResult.isCancelled() && submittedTaskReference.get() != null) {
                submittedTaskReference.get().cancel(true);
            }
        });

        try {
            // 提交到指定池
            Future<?> submittedTask = (taskPool == TaskPool.IO ? ioThreadPoolTaskExecutor : cpuThreadPoolTaskExecutor)
                    .submit(() -> {
                        if (taskResult.isDone()) {
                            return;
                        }
                        try {
                            taskResult.complete(taskCallable.call());
                        } catch (Throwable taskFailure) {
                            Observation currentObservation = observationRegistry.getCurrentObservation();
                            if (currentObservation != null) {
                                currentObservation.error(taskFailure);
                            }
                            taskResult.completeExceptionally(taskFailure);
                            if (taskFailure instanceof Error error) {
                                throw error;
                            }
                        }
                    });
            submittedTaskReference.set(submittedTask);

            // 到期中断任务
            ScheduledFuture<?> timeoutFuture = taskTimeoutScheduler.schedule(() -> {
                if (taskResult.completeExceptionally(new TimeoutException("Task deadline exceeded"))) {
                    submittedTask.cancel(true);
                }
            }, Instant.now(applicationClock).plus(taskTimeout));
            taskResult.whenComplete((taskValue, taskFailure) -> timeoutFuture.cancel(false));

            return taskResult;
        } catch (RuntimeException taskFailure) {
            if (submittedTaskReference.get() != null) {
                submittedTaskReference.get().cancel(true);
            }
            taskResult.completeExceptionally(taskFailure);
            throw taskFailure;
        }
    }

    /**
     * 在 Spring 等待执行器停机前结束 Future，避免排队任务永久悬挂。
     */
    @PreDestroy
    @EventListener(ContextClosedEvent.class)
    public synchronized void close() {
        acceptingTasks = false;
        pendingTaskFutures.forEach(pendingTask -> pendingTask.cancel(true));
    }
}
