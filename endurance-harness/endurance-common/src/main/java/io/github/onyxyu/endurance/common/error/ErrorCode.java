package io.github.onyxyu.endurance.common.error;

/** 稳定的机器可读错误码；不依赖 HTTP 或任何框架类型。 */
public enum ErrorCode {
    SUCCESS("OK", "Success"),
    INVALID_ARGUMENT("REQUEST_INVALID", "Invalid request"),
    UNAUTHENTICATED("AUTH_REQUIRED", "Authentication required"),
    FORBIDDEN("ACCESS_DENIED", "Access denied"),
    NOT_FOUND("RESOURCE_NOT_FOUND", "Resource not found"),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "Method not allowed"),
    UNSUPPORTED_MEDIA_TYPE("MEDIA_TYPE_UNSUPPORTED", "Unsupported media type"),
    NOT_ACCEPTABLE("RESPONSE_NOT_ACCEPTABLE", "Response media type not acceptable"),
    CONFLICT("RESOURCE_CONFLICT", "Resource conflict"),
    TOO_MANY_REQUESTS("RATE_LIMITED", "Too many requests"),
    TASK_REJECTED("TASK_REJECTED", "Task capacity exhausted"),
    TIMEOUT("OPERATION_TIMEOUT", "Operation timed out"),
    UNAVAILABLE("SERVICE_UNAVAILABLE", "Service unavailable"),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() { return code; }
    public String message() { return message; }
}
