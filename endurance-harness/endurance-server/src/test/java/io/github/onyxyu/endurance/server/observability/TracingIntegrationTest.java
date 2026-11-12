package io.github.onyxyu.endurance.server.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.onyxyu.endurance.common.json.Json;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"endurance.security.enabled=false", "endurance.postgres.enabled=false", "endurance.redis.enabled=false"})
class TracingIntegrationTest {
    @LocalServerPort
    int port;
    @Autowired
    Json json;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void continuesW3cParentAndCorrelatesResponse() throws Exception {
        String trace = "0123456789abcdef0123456789abcdef";
        HttpResponse<String> response = get("00-" + trace + "-0123456789abcdef-01");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Trace-Id")).hasValue(trace);
        assertThat(response.headers().firstValue("X-Span-Id")).hasValueSatisfying(value ->
                assertThat(value).matches("[0-9a-f]{16}").isNotEqualTo("0123456789abcdef"));
        assertThat(json.tree(response.body()).path("traceId").asString()).isEqualTo(trace);
    }

    @Test
    void invalidParentIsReplacedAndIndependentRequestsDoNotShareTrace() throws Exception {
        HttpResponse<String> first = get("invalid-parent");
        HttpResponse<String> second = get(null);
        assertThat(first.headers().firstValue("X-Trace-Id")).hasValueSatisfying(value -> assertThat(value).matches("[0-9a-f]{32}"));
        assertThat(first.headers().firstValue("X-Trace-Id")).isNotEqualTo(second.headers().firstValue("X-Trace-Id"));
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void log4jRoutesCategoriesToSeparateRollingFiles() throws Exception {
        assertThat(LoggerFactory.getILoggerFactory().getClass().getName()).contains("Log4j");
        String marker = UUID.randomUUID().toString();
        LoggerFactory.getLogger("endurance.acceptance").info("info-{}", marker);
        LoggerFactory.getLogger("endurance.acceptance").error("error-{}", marker);
        LoggerFactory.getLogger("endurance.socket").info("socket-{}", marker);
        Path directory = Path.of("target/test-logs");
        assertThat(Files.readString(directory.resolve("info.log"))).contains("info-" + marker)
                .doesNotContain("error-" + marker, "socket-" + marker);
        assertThat(Files.readString(directory.resolve("error.log"))).contains("error-" + marker).doesNotContain("info-" + marker);
        assertThat(Files.readString(directory.resolve("socket.log"))).contains("socket-" + marker).doesNotContain("info-" + marker);
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        for (String name : new String[]{"Info", "Error", "Socket"}) {
            RollingFileAppender appender = (RollingFileAppender) context.getConfiguration().getAppender(name);
            CompositeTriggeringPolicy policies = (CompositeTriggeringPolicy) appender.getTriggeringPolicy();
            assertThat(policies.getTriggeringPolicies()).anyMatch(policy ->
                    policy instanceof TimeBasedTriggeringPolicy);
        }
    }

    private HttpResponse<String> get(String parent) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/system/info"));
        if (parent != null) {
            builder.header("traceparent", parent);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
