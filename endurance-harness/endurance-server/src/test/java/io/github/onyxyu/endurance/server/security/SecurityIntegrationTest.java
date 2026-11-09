package io.github.onyxyu.endurance.server.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.onyxyu.endurance.server.support.json.Json;
import io.github.onyxyu.endurance.server.web.Responses;
import io.github.onyxyu.endurance.server.web.dto.ApiResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "endurance.postgres.enabled=false", "endurance.redis.enabled=false", "endurance.security.enabled=true",
        "endurance.security.bearer-token=verification-only-token-0123456789abcdef0123456789",
        "endurance.security.allowed-origins[0]=http://localhost:5173"})
@Import(SecurityIntegrationTest.Endpoints.class)
class SecurityIntegrationTest {
    private static final String TOKEN = "verification-only-token-0123456789abcdef0123456789";
    @LocalServerPort int port;
    @Autowired Json json;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void publicAndProtectedEndpointsUseExpectedAuthenticationBoundaries() throws Exception {
        assertThat(send("GET", "/actuator/health", null, null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/system/info", null, null).statusCode()).isEqualTo(200);
        error(send("GET", "/api/security-test/who", null, null), 401, "AUTH_REQUIRED");
        var invalid = send("GET", "/api/security-test/who", "Bearer wrong", null);
        error(invalid, 401, "AUTH_REQUIRED");
        assertThat(invalid.headers().firstValue("WWW-Authenticate")).hasValueSatisfying(value -> assertThat(value).startsWith("Bearer"));
        var authorized = send("GET", "/api/security-test/who", "Bearer " + TOKEN, null);
        assertThat(authorized.statusCode()).isEqualTo(200);
        assertThat(json.tree(authorized.body()).path("data").asString()).isEqualTo("service");
        assertThat(authorized.headers().allValues("Set-Cookie")).isEmpty();
        // 相同 HTTP 客户端后续请求不会继承前一次身份。
        error(send("GET", "/api/security-test/who", null, null), 401, "AUTH_REQUIRED");
        error(send("GET", "/api/security-test/who?access_token=" + TOKEN, null, null), 401, "AUTH_REQUIRED");
        error(send("GET", "/api/security-test/admin", "Bearer " + TOKEN, null), 403, "ACCESS_DENIED");
        error(send("GET", "/actuator/env", "Bearer " + TOKEN, null), 403, "ACCESS_DENIED");
        error(send("GET", "/api/unknown", "Bearer " + TOKEN, null), 404, "RESOURCE_NOT_FOUND");
    }

    @Test
    void bearerPostAndProtectedMetricsWorkWithoutSessionOrCsrfCookie() throws Exception {
        var post = send("POST", "/api/security-test/echo", "Bearer " + TOKEN, "{\"name\":\"ok\"}");
        assertThat(post.statusCode()).isEqualTo(200);
        assertThat(json.tree(post.body()).path("data").path("name").asString()).isEqualTo("ok");
        assertThat(send("GET", "/actuator/metrics/executor.pool.core", "Bearer " + TOKEN, null).statusCode()).isEqualTo(200);
        error(send("GET", "/actuator/metrics", null, null), 401, "AUTH_REQUIRED");
    }

    @Test
    void corsPreflightOnlyAllowsExplicitOrigins() throws Exception {
        var allowed = client.send(HttpRequest.newBuilder(uri("/api/security-test/who"))
                .header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(allowed.statusCode()).isEqualTo(200);
        assertThat(allowed.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://localhost:5173");
        var denied = client.send(HttpRequest.newBuilder(uri("/api/security-test/who"))
                .header("Origin", "https://unconfigured.example").header("Access-Control-Request-Method", "GET")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        error(denied, 403, "ACCESS_DENIED");
    }

    private void error(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        var envelope = json.tree(response.body());
        assertThat(envelope.path("code").asString()).isEqualTo(code);
        assertThat(envelope.path("success").asBoolean()).isFalse();
        assertThat(envelope.path("traceId").asString()).matches("[0-9a-f]{32}")
                .isEqualTo(response.headers().firstValue("X-Trace-Id").orElseThrow());
        assertThat(response.body()).doesNotContain(TOKEN, "wrong", "BadOpaqueTokenException");
    }

    private HttpResponse<String> send(String method, String path, String authorization, String body) throws Exception {
        var request = HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (authorization != null) { request.header("Authorization", authorization); }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }

    @RestController
    @RequestMapping("/api/security-test")
    static class Endpoints {
        private final Responses responses;
        Endpoints(Responses responses) { this.responses = responses; }
        @GetMapping("/who") public ApiResponse<String> who() {
            return responses.ok(SecurityContextHolder.getContext().getAuthentication().getName());
        }
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/admin") public ApiResponse<String> admin() { return responses.ok("admin"); }
        @PostMapping("/echo") public ApiResponse<Map<String, String>> echo(@RequestBody Map<String, String> input) {
            return responses.ok(input);
        }
    }
}
