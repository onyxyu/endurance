package io.github.onyxyu.endurance.server.security;

import io.github.onyxyu.endurance.common.error.ErrorCode;
import io.github.onyxyu.endurance.server.support.json.Json;
import io.github.onyxyu.endurance.server.web.Responses;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** 过滤器层不能依赖 MVC Advice，直接复用同一响应工厂和 Mapper。 */
@Component
public class SecurityErrorWriter {
    private final Responses responses;
    private final Json json;

    public SecurityErrorWriter(Responses responses, Json json) {
        this.responses = responses;
        this.json = json;
    }

    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        if (response.isCommitted()) { return; }
        var error = responses.error(code);
        response.setStatus(error.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json.write(error.getBody()));
    }
}
