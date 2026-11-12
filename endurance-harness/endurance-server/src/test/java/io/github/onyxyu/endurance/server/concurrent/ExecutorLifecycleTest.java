package io.github.onyxyu.endurance.server.concurrent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.onyxyu.endurance.api.concurrent.TaskPool;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class ExecutorLifecycleTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ExecutorConfiguration.class, Tasks.class, Dependencies.class)
            .withPropertyValues("endurance.security.enabled=false", "endurance.postgres.enabled=false", "endurance.redis.enabled=false",
                    "endurance.executors.io.core-size=1", "endurance.executors.io.max-size=1",
                    "endurance.executors.io.queue-capacity=1", "endurance.executors.io.keep-alive-seconds=10",
                    "endurance.executors.cpu.core-size=1", "endurance.executors.cpu.max-size=1",
                    "endurance.executors.cpu.queue-capacity=1", "endurance.executors.cpu.keep-alive-seconds=10",
                    "endurance.executors.shutdown-seconds=1", "endurance.executors.task-timeout-seconds=30");

    @Test
    void rejectsInvalidPoolRange() {
        runner.withPropertyValues("endurance.executors.io.core-size=2").run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    void shutdownCancelsRunningAndQueuedPromisesAndTerminatesPools() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            Tasks tasks = context.getBean(Tasks.class);
            CountDownLatch started = new CountDownLatch(1);
            CompletableFuture<Integer> running = tasks.submit(TaskPool.IO, () -> {
                started.countDown();
                new CountDownLatch(1).await();
                return 1;
            });
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            CompletableFuture<Integer> queued = tasks.submit(TaskPool.IO, () -> 2);
            ThreadPoolExecutor pool = context.getBean("ioExecutor", ThreadPoolTaskExecutor.class).getThreadPoolExecutor();
            context.close();
            assertThat(running.isCancelled()).isTrue();
            assertThat(queued.isCancelled()).isTrue();
            assertThat(pool.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> tasks.submit(TaskPool.IO, () -> 3)).isInstanceOf(RejectedExecutionException.class);
        });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dependencies {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        ObservationRegistry observationRegistry() {
            return ObservationRegistry.create();
        }
    }
}
