package io.github.onyxyu.endurance.server.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.onyxyu.endurance.common.error.ApplicationException;
import io.github.onyxyu.endurance.common.error.ErrorCode;
import io.github.onyxyu.endurance.server.support.json.Json;
import io.github.onyxyu.endurance.server.support.json.JsonConversionException;
import io.github.onyxyu.endurance.server.web.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.type.TypeReference;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"endurance.security.enabled=false", "endurance.postgres.enabled=false", "endurance.redis.enabled=false"})
@Import({HttpFoundationIntegrationTest.Endpoints.class, HttpFoundationIntegrationTest.TestAccess.class})
class HttpFoundationIntegrationTest {
    @LocalServerPort int port;
    @Autowired Json json;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void successAndValidationHaveTheSameEnvelope() throws Exception {
        var success = request("POST", "/echo", "{\"name\":\"hello\"}");
        assertThat(success.statusCode()).isEqualTo(200);
        assertThat(json.tree(success.body()).path("data").path("name").asString()).isEqualTo("hello");
        var invalid = request("POST", "/echo", "{\"name\":\"\"}");
        assertError(invalid, 400, "REQUEST_INVALID");
        assertThat(json.tree(invalid.body()).path("errors").get(0).path("field").asString()).isEqualTo("name");
    }

    @Test
    void frameworkAndApplicationFailuresKeepTheirHttpSemantics() throws Exception {
        assertError(request("POST", "/echo", "{broken"), 400, "REQUEST_INVALID");
        assertError(request("GET", "/number?value=0", null), 400, "REQUEST_INVALID");
        assertError(request("GET", "/number?value=abc", null), 400, "REQUEST_INVALID");
        assertError(request("GET", "/number", null), 400, "REQUEST_INVALID");
        var method = request("GET", "/echo", null);
        assertError(method, 405, "METHOD_NOT_ALLOWED");
        assertThat(method.headers().firstValue("Allow")).hasValueSatisfying(value -> assertThat(value).contains("POST"));
        assertError(request("GET", "/conflict", null), 409, "RESOURCE_CONFLICT");
        assertError(request("GET", "/missing", null), 404, "RESOURCE_NOT_FOUND");
        var failure = request("GET", "/failure", null);
        assertError(failure, 500, "INTERNAL_ERROR");
        assertThat(failure.body()).doesNotContain("private-secret", "IllegalStateException", "stackTrace");
    }

    @Test
    void jsonUtilitySharesSpringMapperAndPreservesGenericTypesAndTime() {
        var instant = Instant.parse("2026-11-09T00:00:00Z");
        assertThat(json.read(json.write(new Payload("test", instant)), Payload.class)).isEqualTo(new Payload("test", instant));
        assertThat(json.write(instant)).contains("2026-11-09T00:00:00Z");
        var generic = json.read("{\"items\":[{\"name\":\"a\",\"at\":null}]}",
                new TypeReference<Map<String, List<Payload>>>() {});
        assertThat(generic.get("items").getFirst().name()).isEqualTo("a");
        assertThat(json.readList("[{\"name\":\"a\"}]", Payload.class)).hasSize(1);
        assertThat(json.readMap("{\"name\":\"a\"}")).containsEntry("name", "a");
        assertThat(json.convert(Map.of("name", "b"), Payload.class).name()).isEqualTo("b");
        assertThat(json.convert(List.of(Map.of("name", "c")), new TypeReference<List<Payload>>() {}).getFirst().name()).isEqualTo("c");
        assertThat(json.fromTree(json.toTree(new Payload("tree", instant)), Payload.class).at()).isEqualTo(instant);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> json.read("{secret", Payload.class))
                .isInstanceOf(JsonConversionException.class).hasMessage("JSON conversion failed");
    }

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/_test" + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertError(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        var body = json.tree(response.body());
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("code").asString()).isEqualTo(code);
        assertThat(body.has("errors")).isTrue();
        assertThat(body.path("timestamp").asString()).endsWith("Z");
    }

    record Input(@NotBlank String name) {}
    record Payload(String name, Instant at) {}

    @RestController
    @RequestMapping("/api/_test")
    static class Endpoints {
        private final Responses responses;
        Endpoints(Responses responses) { this.responses = responses; }
        @PostMapping("/echo") ApiResponse<Input> echo(@Valid @RequestBody Input input) { return responses.ok(input); }
        @GetMapping("/number") ApiResponse<Integer> number(@RequestParam @Min(1) int value) { return responses.ok(value); }
        @GetMapping("/conflict") void conflict() { throw new ApplicationException(ErrorCode.CONFLICT); }
        @GetMapping("/failure") void failure() { throw new IllegalStateException("private-secret"); }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestAccess {
        @Bean @Order(0)
        SecurityFilterChain testOnlyAccess(HttpSecurity http) throws Exception {
            return http.securityMatcher("/api/_test/**").csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(requests -> requests.anyRequest().permitAll()).build();
        }
    }
}
