package io.github.onyxyu.endurance.server.web.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * 所有普通 JSON API 共用的响应；静态资源、Actuator、SSE 不适用此包装。
 */
public record ApiResponse<T>(boolean success, String code, String message, T data, List<FieldViolation> errors,
        String traceId, Instant timestamp) {
    public ApiResponse {
        errors = List.copyOf(errors);
    }

    public record FieldViolation(String field, String message) {
    }
}
