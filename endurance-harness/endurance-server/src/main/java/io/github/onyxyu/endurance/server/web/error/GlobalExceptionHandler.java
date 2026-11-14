package io.github.onyxyu.endurance.server.web.error;

import io.github.onyxyu.endurance.common.error.ApplicationException;
import io.github.onyxyu.endurance.common.error.ErrorCode;
import io.github.onyxyu.endurance.server.web.Responses;
import io.github.onyxyu.endurance.server.web.dto.response.ApiResponse;
import io.github.onyxyu.endurance.server.web.dto.response.ApiResponse.FieldViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Responses apiResponses;

    public GlobalExceptionHandler(Responses apiResponses) {
        this.apiResponses = apiResponses;
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> forbidden(Exception exception) {
        return apiResponses.error(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiResponse<Void>> unauthorized(Exception exception) {
        return apiResponses.error(ErrorCode.UNAUTHENTICATED);
    }

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ApiResponse<Void>> application(ApplicationException exception) {
        return apiResponses.error(exception.errorCode(), exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException exception) {
        List<FieldViolation> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage())).toList();
        return apiResponses.error(ErrorCode.INVALID_ARGUMENT, ErrorCode.INVALID_ARGUMENT.message(), errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> constraint(ConstraintViolationException exception) {
        List<FieldViolation> errors = exception.getConstraintViolations().stream()
                .map(error -> new FieldViolation(error.getPropertyPath().toString(), error.getMessage())).toList();
        return apiResponses.error(ErrorCode.INVALID_ARGUMENT, ErrorCode.INVALID_ARGUMENT.message(), errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiResponse<Void>> methodValidation(HandlerMethodValidationException exception) {
        if (exception.isForReturnValue()) {
            return unexpected(exception);
        }
        List<FieldViolation> errors = exception.getParameterValidationResults().stream()
                .flatMap(
                        result -> result.getResolvableErrors().stream()
                                .map(
                                        error -> new FieldViolation(
                                                result.getMethodParameter().getParameterName() == null
                                                        ? "argument"
                                                        : result.getMethodParameter().getParameterName(),
                                                error.getDefaultMessage()
                                        )
                                )
                )
                .toList();
        return apiResponses.error(ErrorCode.INVALID_ARGUMENT, ErrorCode.INVALID_ARGUMENT.message(), errors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ApiResponse<Void>> malformed(Exception exception) {
        return apiResponses.error(ErrorCode.INVALID_ARGUMENT);
    }

    @ExceptionHandler(RejectedExecutionException.class)
    ResponseEntity<ApiResponse<Void>> rejected(RejectedExecutionException exception) {
        return apiResponses.error(ErrorCode.TASK_REJECTED);
    }

    @ExceptionHandler(TimeoutException.class)
    ResponseEntity<ApiResponse<Void>> timeout(TimeoutException exception) {
        return apiResponses.error(ErrorCode.TIMEOUT);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception exception) {
        if (exception instanceof ErrorResponse error) {
            ErrorCode code = switch (error.getStatusCode().value()) {
                case 400 -> {
                    yield ErrorCode.INVALID_ARGUMENT;
                }
                case 404 -> {
                    yield ErrorCode.NOT_FOUND;
                }
                case 405 -> {
                    yield ErrorCode.METHOD_NOT_ALLOWED;
                }
                case 406 -> {
                    yield ErrorCode.NOT_ACCEPTABLE;
                }
                case 415 -> {
                    yield ErrorCode.UNSUPPORTED_MEDIA_TYPE;
                }
                case 503 -> {
                    yield ErrorCode.UNAVAILABLE;
                }
                default -> {
                    yield ErrorCode.INTERNAL_ERROR;
                }
            };
            return ResponseEntity.status(HttpErrorStatus.of(code)).headers(error.getHeaders())
                    .contentType(MediaType.APPLICATION_JSON).body(apiResponses.error(code).getBody());
        }
        // 不将异常消息、堆栈、请求体和凭据返回客户端。
        log.error("Unhandled request failure", exception);
        return apiResponses.error(ErrorCode.INTERNAL_ERROR);
    }
}
