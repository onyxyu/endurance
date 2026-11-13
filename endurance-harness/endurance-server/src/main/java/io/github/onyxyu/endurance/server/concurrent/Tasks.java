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
    private final ThreadPoolTaskExecutor io;
    private final ThreadPoolTaskExecutor cpu;
    private final ThreadPoolTaskScheduler scheduler;
    private final ExecutorProperties properties;
    private final ObservationRegistry observations;
    private final Clock clock;
    private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();
    private boolean accepting = true;

    public Tasks(@Qualifier("ioExecutor") ThreadPoolTaskExecutor io,
            @Qualifier("cpuExecutor") ThreadPoolTaskExecutor cpu, ThreadPoolTaskScheduler scheduler,
            ExecutorProperties properties, ObservationRegistry observations, Clock clock) {
        this.io = io;
        this.cpu = cpu;
        this.scheduler = scheduler;
        this.properties = properties;
        this.observations = observations;
        this.clock = clock;
    }

    @Override
    public <T> CompletableFuture<T> submit(TaskPool pool, Callable<T> work) {
        return submit(pool, work, Duration.ofSeconds(properties.taskTimeoutSeconds()));
    }

    @Override
    public CompletableFuture<Void> execute(TaskPool pool, Runnable work) {
        return submit(pool, () -> {
            work.run();
            return null;
        });
    }

    @Override
    public synchronized <T> CompletableFuture<T> submit(TaskPool pool, Callable<T> work, Duration timeout) {
        // 校验提交条件
        Objects.requireNonNull(pool);
        Objects.requireNonNull(work);
        Objects.requireNonNull(timeout);
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        if (!accepting) {
            throw new RejectedExecutionException("Application is shutting down");
        }

        // 向下传递取消
        CompletableFuture<T> result = new CompletableFuture<T>();
        AtomicReference<Future<?>> submitted = new AtomicReference<Future<?>>();
        pending.add(result);

        result.whenComplete((value, failure) -> {
            pending.remove(result);
            if (result.isCancelled() && submitted.get() != null) {
                submitted.get().cancel(true);
            }
        });

        try {
            // 提交到指定池
            Future<?> future = (pool == TaskPool.IO ? io : cpu).submit(() -> {
                if (result.isDone()) {
                    return;
                }
                try {
                    result.complete(work.call());
                } catch (Throwable failure) {
                    Observation current = observations.getCurrentObservation();
                    if (current != null) {
                        current.error(failure);
                    }
                    result.completeExceptionally(failure);
                    if (failure instanceof Error error) {
                        throw error;
                    }
                }
            });
            submitted.set(future);

            // 到期中断任务
            ScheduledFuture<?> deadline = scheduler.schedule(() -> {
                if (result.completeExceptionally(new TimeoutException("Task deadline exceeded"))) {
                    future.cancel(true);
                }
            }, Instant.now(clock).plus(timeout));
            result.whenComplete((value, failure) -> deadline.cancel(false));

            return result;
        } catch (RuntimeException failure) {
            if (submitted.get() != null) {
                submitted.get().cancel(true);
            }
            result.completeExceptionally(failure);
            throw failure;
        }
    }

    /**
     * 在 Spring 等待执行器停机前结束 Future，避免排队任务永久悬挂。
     */
    @PreDestroy
    @EventListener(ContextClosedEvent.class)
    public synchronized void close() {
        accepting = false;
        pending.forEach(task -> task.cancel(true));
    }
}
