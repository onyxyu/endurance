package io.github.onyxyu.endurance.server.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** 使用本地 OTLP 接收器验证真实导出，不依赖任何观测平台。 */
@org.springframework.test.context.TestExecutionListeners(
        listeners = OtlpExportIntegrationTest.CollectorCleanup.class,
        mergeMode = org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"endurance.postgres.enabled=false", "endurance.redis.enabled=false"})
class OtlpExportIntegrationTest {
    private static final List<byte[]> TRACES = new CopyOnWriteArrayList<>();
    private static final List<byte[]> METRICS = new CopyOnWriteArrayList<>();
    private static final HttpServer COLLECTOR = collector();
    @LocalServerPort int port;

    @DynamicPropertySource
    static void exporter(DynamicPropertyRegistry properties) {
        String base = "http://127.0.0.1:" + COLLECTOR.getAddress().getPort();
        properties.add("management.tracing.export.otlp.enabled", () -> true);
        properties.add("management.opentelemetry.tracing.export.otlp.endpoint", () -> base + "/v1/traces");
        properties.add("management.opentelemetry.tracing.export.schedule-delay", () -> "100ms");
        properties.add("management.otlp.metrics.export.enabled", () -> true);
        properties.add("management.otlp.metrics.export.url", () -> base + "/v1/metrics");
        properties.add("management.otlp.metrics.export.step", () -> "1s");
    }

    @Test
    void exportsMatchingTraceAndMetricsUsingHttpProtobuf() throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + "/api/system/info")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        String traceId = response.headers().firstValue("X-Trace-Id").orElseThrow();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(TRACES).anyMatch(payload -> HexFormat.of().formatHex(payload).contains(traceId));
            assertThat(METRICS).anyMatch(payload -> payload.length > 0);
        });
    }

    public static class CollectorCleanup extends org.springframework.test.context.support.AbstractTestExecutionListener {
        @Override public int getOrder() { return Integer.MIN_VALUE; }
        @Override public void afterTestClass(org.springframework.test.context.TestContext context) {
            COLLECTOR.stop(0);
        }
    }

    private static HttpServer collector() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            for (String path : List.of("/v1/traces", "/v1/metrics")) {
                server.createContext(path, exchange -> {
                    if (exchange.getRequestHeaders().getFirst("Content-Type").contains("protobuf")) {
                        (path.endsWith("traces") ? TRACES : METRICS).add(exchange.getRequestBody().readAllBytes());
                    }
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                });
            }
            server.start();
            return server;
        } catch (java.io.IOException exception) { throw new ExceptionInInitializerError(exception); }
    }
}
