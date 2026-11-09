package io.github.onyxyu.endurance.server.concurrent;

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
    ThreadPoolTaskExecutor ioExecutor(ExecutorProperties properties, MeterRegistry meters, ObservationRegistry observations) {
        return executor("ioExecutor", properties.io(), properties.shutdownSeconds(), meters, observations);
    }

    @Bean
    ThreadPoolTaskExecutor cpuExecutor(ExecutorProperties properties, MeterRegistry meters, ObservationRegistry observations) {
        return executor("cpuExecutor", properties.cpu(), properties.shutdownSeconds(), meters, observations);
    }

    @Bean
    ThreadPoolTaskScheduler timeoutScheduler(ExecutorProperties properties, Clock clock) {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("endurance-timeout-");
        scheduler.setClock(clock);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.setAwaitTerminationSeconds(properties.shutdownSeconds());
        return scheduler;
    }

    @Bean
    AsyncConfigurer asyncConfigurer(@Qualifier("ioExecutor") ThreadPoolTaskExecutor executor) {
        return new AsyncConfigurer() {
            @Override public Executor getAsyncExecutor() { return executor; }
            @Override public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
                return (failure, method, arguments) -> log.error("Async method failed: {}", method.getName(), failure);
            }
        };
    }

    private ThreadPoolTaskExecutor executor(String name, ExecutorProperties.Pool pool, int shutdownSeconds,
            MeterRegistry meters, ObservationRegistry observations) {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(pool.coreSize());
        executor.setMaxPoolSize(pool.maxSize());
        executor.setQueueCapacity(pool.queueCapacity());
        executor.setKeepAliveSeconds(pool.keepAliveSeconds());
        executor.setThreadNamePrefix("endurance-" + name + "-");
        executor.setStrictEarlyShutdown(true);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(shutdownSeconds);
        var rejected = meters.counter("endurance.executor.rejected", "name", name);
        executor.setRejectedExecutionHandler((task, poolExecutor) -> {
            rejected.increment();
            throw new RejectedExecutionException("Executor capacity exhausted: " + name);
        });
        executor.setTaskDecorator(decorator(name, observations));
        return executor;
    }

    private TaskDecorator decorator(String name, ObservationRegistry observations) {
        var propagation = new ContextPropagatingTaskDecorator();
        return task -> {
            Map<String, String> captured = MDC.getCopyOfContextMap();
            return propagation.decorate(() -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                try {
                    if (captured == null) { MDC.clear(); } else { MDC.setContextMap(captured); }
                    Observation.createNotStarted("endurance.executor.task", observations)
                            .lowCardinalityKeyValue("name", name).observe(task);
                } finally {
                    if (previous == null) { MDC.clear(); } else { MDC.setContextMap(previous); }
                }
            });
        };
    }
}
