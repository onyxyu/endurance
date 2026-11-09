package io.github.onyxyu.endurance.server.concurrent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.Async;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "endurance.executors.io.core-size=1", "endurance.executors.io.max-size=1",
        "endurance.executors.io.queue-capacity=1"})
@Import(ExecutorIntegrationTest.ProbeConfiguration.class)
class ExecutorIntegrationTest {
    @Autowired Tasks tasks;
    @Autowired Tracer tracer;
    @Autowired ObservationRegistry observations;
    @Autowired MeterRegistry meters;
    @Autowired AsyncProbe async;

    @Test
    void propagatesParentTraceAndMdcAndCreatesChildSpans() throws Exception {
        Observation parent = Observation.start("test.parent", observations);
        String trace;
        try (Observation.Scope ignored = parent.openScope(); MDC.MDCCloseable note = MDC.putCloseable("requestNote", "first")) {
            trace = tracer.currentSpan().context().traceId();
            String span = tracer.currentSpan().context().spanId();
            var task = tasks.submit(TaskPool.IO, this::context).get(2, TimeUnit.SECONDS);
            assertThat(task).containsEntry("trace", trace).containsEntry("note", "first");
            assertThat(task.get("span")).isNotEqualTo(span);
            assertThat(task.get("thread")).startsWith("endurance-ioExecutor-");
            var annotated = async.context().get(2, TimeUnit.SECONDS);
            assertThat(annotated).containsEntry("trace", trace).containsEntry("note", "first");
            assertThat(annotated.get("thread")).startsWith("endurance-cpuExecutor-");
        } finally { parent.stop(); }
        assertThat(tasks.submit(TaskPool.IO, () -> MDC.get("requestNote")).get(2, TimeUnit.SECONDS)).isNull();
        assertThat(tasks.submit(TaskPool.IO, () -> MDC.get("traceId")).get(2, TimeUnit.SECONDS)).isNotEqualTo(trace);
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void boundedPoolRejectsAndReportsQueueAndCapacity() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var running = tasks.submit(TaskPool.IO, () -> { started.countDown(); release.await(); return 1; });
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
        try {
            var queued = tasks.submit(TaskPool.IO, () -> 2);
            double before = meters.get("endurance.executor.rejected").tag("name", "ioExecutor").counter().count();
            assertThatThrownBy(() -> tasks.submit(TaskPool.IO, () -> 3)).isInstanceOf(RejectedExecutionException.class);
            assertThat(meters.get("endurance.executor.rejected").tag("name", "ioExecutor").counter().count()).isEqualTo(before + 1);
            assertThat(meters.get("executor.pool.core").tag("name", "ioExecutor").gauge().value()).isEqualTo(1);
            assertThat(meters.get("executor.pool.max").tag("name", "ioExecutor").gauge().value()).isEqualTo(1);
            assertThat(meters.get("executor.queued").tag("name", "ioExecutor").gauge().value()).isEqualTo(1);
            release.countDown();
            assertThat(queued.get(2, TimeUnit.SECONDS)).isEqualTo(2);
        } finally { release.countDown(); running.get(2, TimeUnit.SECONDS); }
    }

    @Test
    void deadlineAndCancellationInterruptTheActualWork() throws Exception {
        var interrupted = new CountDownLatch(1);
        var timed = tasks.submit(TaskPool.IO, () -> {
            try { new CountDownLatch(1).await(); }
            catch (InterruptedException expected) { interrupted.countDown(); throw expected; }
            return 1;
        }, Duration.ofMillis(150));
        assertThatThrownBy(() -> timed.get(2, TimeUnit.SECONDS)).hasCauseInstanceOf(TimeoutException.class);
        assertThat(interrupted.await(2, TimeUnit.SECONDS)).isTrue();
        var started = new CountDownLatch(1);
        var cancelled = new CountDownLatch(1);
        var work = tasks.submit(TaskPool.IO, () -> {
            started.countDown();
            try { new CountDownLatch(1).await(); }
            catch (InterruptedException expected) { cancelled.countDown(); throw expected; }
            return 1;
        });
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(work.cancel(true)).isTrue();
        assertThat(cancelled.await(2, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> tasks.submit(TaskPool.CPU, () -> { throw new IllegalStateException("expected"); }).get())
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    private Map<String, String> context() {
        return Map.of("trace", MDC.get("traceId"), "span", MDC.get("spanId"),
                "note", MDC.get("requestNote"), "thread", Thread.currentThread().getName());
    }

    static class AsyncProbe {
        @Async("cpuExecutor")
        public CompletableFuture<Map<String, String>> context() {
            return CompletableFuture.completedFuture(Map.of("trace", MDC.get("traceId"), "note", MDC.get("requestNote"),
                    "thread", Thread.currentThread().getName()));
        }
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {
        @Bean AsyncProbe asyncProbe() { return new AsyncProbe(); }
    }
}
