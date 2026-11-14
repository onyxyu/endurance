package io.github.onyxyu.endurance.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * 验证默认骨架无需数据库或模型凭据即可启动，并保持未实现接口关闭。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "endurance.security.enabled=false", "endurance.postgres.enabled=false", "endurance.redis.enabled=false"})
class BootstrapIntegrationTest {
    @LocalServerPort
    int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void startsWithoutExternalServicesAndExposesReadOnlyChecks() throws Exception {
        HttpResponse<String> health = get("/actuator/health");
        assertEquals(200, health.statusCode());
        assertTrue(health.body().contains("UP"));
        HttpResponse<String> info = get("/api/system/info");
        assertEquals(200, info.statusCode());
        assertTrue(info.body().contains("foundation"));
        HttpResponse<String> index = get("/");
        assertEquals(200, index.statusCode());
        assertTrue(index.body().contains("Endurance Harness"));
    }

    @Test
    void keepsUnimplementedWriteEndpointsClosed() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/sessions"))
                .POST(HttpRequest.BodyPublishers.ofString("{}")).header("Content-Type", "application/json").build();
        assertEquals(401, httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return httpClient.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
