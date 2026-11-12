package io.github.onyxyu.endurance.common.error;

import java.util.Objects;

/**
 * 可预期的应用异常；message 必须是可对外公开的信息。
 */
public class ApplicationException extends RuntimeException {
    private final ErrorCode errorCode;

    public ApplicationException(ErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    public ApplicationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode);
        if (errorCode == ErrorCode.SUCCESS) {
            throw new IllegalArgumentException("An exception cannot use the success code");
        }
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
