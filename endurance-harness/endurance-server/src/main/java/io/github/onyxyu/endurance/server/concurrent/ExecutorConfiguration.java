package io.github.onyxyu.endurance.server.concurrent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
@EnableAsync
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExecutorProperties.class)
public class ExecutorConfiguration {
    @Bean(name = {"ioExecutor", "applicationTaskExecutor", "taskExecutor"})
    ThreadPoolTaskExecutor ioExecutor(ExecutorProperties executorProperties,
                                      MeterRegistry meterRegistry,
                                      ObservationRegistry observationRegistry) {
        return createTaskExecutor(
                "ioExecutor",
                executorProperties.io(),
                executorProperties.shutdownSeconds(),
                meterRegistry,
                observationRegistry
        );
    }

    @Bean
    ThreadPoolTaskExecutor cpuExecutor(ExecutorProperties executorProperties,
                                       MeterRegistry meterRegistry,
                                       ObservationRegistry observationRegistry) {
        return createTaskExecutor(
                "cpuExecutor",
                executorProperties.cpu(),
                executorProperties.shutdownSeconds(),
                meterRegistry,
                observationRegistry
        );
    }

    @Bean
    ThreadPoolTaskScheduler timeoutScheduler(ExecutorProperties executorProperties, Clock applicationClock) {
        ThreadPoolTaskScheduler taskTimeoutScheduler = new ThreadPoolTaskScheduler();
        taskTimeoutScheduler.setPoolSize(1);
        taskTimeoutScheduler.setThreadNamePrefix("endurance-timeout-");
        taskTimeoutScheduler.setClock(applicationClock);
        taskTimeoutScheduler.setRemoveOnCancelPolicy(true);
        taskTimeoutScheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        taskTimeoutScheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        taskTimeoutScheduler.setWaitForTasksToCompleteOnShutdown(false);
        taskTimeoutScheduler.setAwaitTerminationSeconds(executorProperties.shutdownSeconds());
        return taskTimeoutScheduler;
    }

    @Bean
    AsyncConfigurer asyncConfigurer(@Qualifier("ioExecutor") ThreadPoolTaskExecutor ioThreadPoolTaskExecutor) {
        return new AsyncConfigurer() {
            @Override
            public Executor getAsyncExecutor() {
                return ioThreadPoolTaskExecutor;
            }

            @Override
            public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
                return (failure, method, arguments) -> log.error("Async method failed: {}", method.getName(), failure);
            }
        };
    }

    private ThreadPoolTaskExecutor createTaskExecutor(String executorName,
                                                      ExecutorProperties.Pool poolProperties,
                                                      int shutdownTimeoutSeconds,
                                                      MeterRegistry meterRegistry,
                                                      ObservationRegistry observationRegistry) {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(poolProperties.coreSize());
        threadPoolTaskExecutor.setMaxPoolSize(poolProperties.maxSize());
        threadPoolTaskExecutor.setQueueCapacity(poolProperties.queueCapacity());
        threadPoolTaskExecutor.setKeepAliveSeconds(poolProperties.keepAliveSeconds());
        threadPoolTaskExecutor.setThreadNamePrefix("endurance-" + executorName + "-");

        // 关闭时停止接收任务
        threadPoolTaskExecutor.setStrictEarlyShutdown(true);
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(false);
        threadPoolTaskExecutor.setAwaitTerminationSeconds(shutdownTimeoutSeconds);

        // 饱和时明确拒绝
        Counter rejectedTaskCounter = meterRegistry.counter("endurance.executor.rejected", "name", executorName);
        threadPoolTaskExecutor.setRejectedExecutionHandler((task, poolExecutor) -> {
            rejectedTaskCounter.increment();
            throw new RejectedExecutionException("Executor capacity exhausted: " + executorName);
        });
        threadPoolTaskExecutor
                .setTaskDecorator(createContextPropagatingTaskDecorator(executorName, observationRegistry));
        return threadPoolTaskExecutor;
    }

    private TaskDecorator createContextPropagatingTaskDecorator(String executorName,
                                                                ObservationRegistry observationRegistry) {
        ContextPropagatingTaskDecorator contextPropagatingTaskDecorator = new ContextPropagatingTaskDecorator();
        return task -> {
            // 捕获提交方上下文
            Map<String, String> capturedMdcContext = MDC.getCopyOfContextMap();
            return contextPropagatingTaskDecorator.decorate(() -> {
                Map<String, String> previousMdcContext = MDC.getCopyOfContextMap();
                try {
                    if (capturedMdcContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(capturedMdcContext);
                    }
                    Observation.createNotStarted("endurance.executor.task", observationRegistry)
                            .lowCardinalityKeyValue("name", executorName).observe(task);
                } finally {
                    // 恢复工作线程上下文
                    if (previousMdcContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(previousMdcContext);
                    }
                }
            });
        };
    }
}
