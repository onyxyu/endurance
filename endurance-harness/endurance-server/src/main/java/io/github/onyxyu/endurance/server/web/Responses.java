package io.github.onyxyu.endurance.server.web;

import io.github.onyxyu.endurance.common.error.ErrorCode;
import io.github.onyxyu.endurance.server.web.dto.response.ApiResponse.FieldViolation;
import io.github.onyxyu.endurance.server.web.dto.response.ApiResponse;
import io.github.onyxyu.endurance.server.web.error.HttpErrorStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * 构造器注入后使用 responses.ok(data)/responses.error(code)，不使用静态容器查找。
 */
@Component
public class Responses {
    private final Clock clock;

    public Responses(Clock clock) {
        this.clock = clock;
    }

    public <T> ApiResponse<T> ok(T data) {
        return body(ErrorCode.SUCCESS, ErrorCode.SUCCESS.message(), data, List.of());
    }

    public ResponseEntity<ApiResponse<Void>> error(ErrorCode code) {
        return error(code, code.message(), List.of());
    }

    public ResponseEntity<ApiResponse<Void>> error(ErrorCode code, String message, List<FieldViolation> errors) {
        return ResponseEntity.status(HttpErrorStatus.of(code)).contentType(MediaType.APPLICATION_JSON).body(body(code, message, null, errors));
    }

    private <T> ApiResponse<T> body(ErrorCode code, String message, T data, List<FieldViolation> errors) {
        return new ApiResponse<>(code == ErrorCode.SUCCESS, code.code(), message, data,
                errors, MDC.get("traceId"), Instant.now(clock));
    }
}
