package io.github.onyxyu.endurance.server.security;

import io.github.onyxyu.endurance.common.error.ErrorCode;
import io.github.onyxyu.endurance.common.json.Json;
import io.github.onyxyu.endurance.server.web.Responses;
import io.github.onyxyu.endurance.server.web.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * 过滤器层不能依赖 MVC Advice，直接复用同一响应工厂和 Mapper。
 */
@Component
public class SecurityErrorWriter {
    private final Responses apiResponses;
    private final Json jsonConverter;

    public SecurityErrorWriter(Responses apiResponses, Json jsonConverter) {
        this.apiResponses = apiResponses;
        this.jsonConverter = jsonConverter;
    }

    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        ResponseEntity<ApiResponse<Void>> errorResponse = apiResponses.error(code);
        response.setStatus(errorResponse.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonConverter.write(errorResponse.getBody()));
    }
}
