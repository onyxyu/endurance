package io.github.onyxyu.endurance.server.web.error;

import io.github.onyxyu.endurance.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 应用错误到传输层状态的唯一映射。新增错误码需在这里显式归类。
 */
public final class HttpErrorStatus {
    private HttpErrorStatus() {
    }

    public static HttpStatus of(ErrorCode code) {
        return switch (code) {
            case SUCCESS -> {
                yield HttpStatus.OK;
            }
            case INVALID_ARGUMENT -> {
                yield HttpStatus.BAD_REQUEST;
            }
            case UNAUTHENTICATED -> {
                yield HttpStatus.UNAUTHORIZED;
            }
            case FORBIDDEN -> {
                yield HttpStatus.FORBIDDEN;
            }
            case NOT_FOUND -> {
                yield HttpStatus.NOT_FOUND;
            }
            case METHOD_NOT_ALLOWED -> {
                yield HttpStatus.METHOD_NOT_ALLOWED;
            }
            case UNSUPPORTED_MEDIA_TYPE -> {
                yield HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            }
            case NOT_ACCEPTABLE -> {
                yield HttpStatus.NOT_ACCEPTABLE;
            }
            case CONFLICT -> {
                yield HttpStatus.CONFLICT;
            }
            case TOO_MANY_REQUESTS -> {
                yield HttpStatus.TOO_MANY_REQUESTS;
            }
            case TASK_REJECTED, UNAVAILABLE -> {
                yield HttpStatus.SERVICE_UNAVAILABLE;
            }
            case TIMEOUT -> {
                yield HttpStatus.GATEWAY_TIMEOUT;
            }
            case INTERNAL_ERROR -> {
                yield HttpStatus.INTERNAL_SERVER_ERROR;
            }
        };
    }
}
