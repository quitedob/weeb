package com.web.exception;

import com.web.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回标准格式的错误信息
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(WeebException.class)
    public ResponseEntity<ApiResponse<Object>> handleWeebException(WeebException e, WebRequest request) {
        String eventId = java.util.UUID.randomUUID().toString();
        String path = request.getDescription(false).replace("uri=", "");

        log.warn("业务异常 eventId={}, path={}, message={}", eventId, path, e.getMessage());

        ApiResponse<Object> response = ApiResponse.error(ApiResponse.ErrorCode.SYSTEM_ERROR,
                "业务异常: " + e.getMessage())
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理参数验证异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException e, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String eventId = java.util.UUID.randomUUID().toString();
        String path = request.getDescription(false).replace("uri=", "");

        log.warn("参数验证失败 eventId={}, path={}, errors={}", eventId, path, errors);

        ApiResponse<Map<String, String>> response = ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR,
                "参数验证失败", errors)
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBindException(BindException e, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String eventId = java.util.UUID.randomUUID().toString();
        String path = request.getDescription(false).replace("uri=", "");

        log.warn("数据绑定失败 eventId={}, path={}, errors={}", eventId, path, errors);

        ApiResponse<Map<String, String>> response = ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR,
                "数据绑定失败", errors)
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(ConstraintViolationException e, WebRequest request) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        Map<String, String> errors = new HashMap<>();

        violations.forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });

        String eventId = java.util.UUID.randomUUID().toString();
        String path = request.getDescription(false).replace("uri=", "");

        log.warn("约束验证失败 eventId={}, path={}, errors={}", eventId, path, errors);

        ApiResponse<Map<String, String>> response = ApiResponse.error(ApiResponse.ErrorCode.PARAM_ERROR,
                "约束验证失败", errors)
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException e, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        log.warn("认证失败: path={}, message={}", path, e.getMessage());

        ApiResponse<Object> response = ApiResponse.unauthorized("认证失败，请重新登录")
                .withPath(path);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 处理访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException e, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        log.warn("访问被拒绝: path={}, message={}", path, e.getMessage());

        ApiResponse<Object> response = ApiResponse.forbidden("访问被拒绝，权限不足")
                .withPath(path);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Object>> handleNullPointerException(NullPointerException e, WebRequest request) {
        String eventId = java.util.UUID.randomUUID().toString();
        String path = request.getDescription(false).replace("uri=", "");

        log.error("空指针异常 eventId={}, path={}", eventId, path, e);

        ApiResponse<Object> response = ApiResponse.systemError("系统内部错误，请稍后重试")
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException e, WebRequest request) {
        String eventId = java.util.UUID.randomUUID().toString();
        String path = request.getDescription(false).replace("uri=", "");

        log.error("运行时异常 eventId={}, path={}", eventId, path, e);

        ApiResponse<Object> response = ApiResponse.systemError("系统运行异常，请稍后重试")
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException e, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");

        log.warn("非法参数: path={}, message={}", path, e.getMessage());

        ApiResponse<Object> response = ApiResponse.badRequest(e.getMessage())
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e, WebRequest request) {
        String eventId = java.util.UUID.randomUUID().toString();
        String path = request.getDescription(false).replace("uri=", "");

        log.error("未处理的异常 eventId={}, path={}, type={}", eventId, path, e.getClass().getSimpleName(), e);

        ApiResponse<Object> response = ApiResponse.systemError("系统异常，请联系管理员")
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理HTTP方法不支持异常
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupportedException(
            org.springframework.web.HttpRequestMethodNotSupportedException e, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");

        log.warn("HTTP方法不支持: path={}, method={}, supported={}",
                path, e.getMethod(), e.getSupportedMethods());

        ApiResponse<Object> response = ApiResponse.error(ApiResponse.ErrorCode.METHOD_NOT_ALLOWED,
                "请求方法不支持")
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }
}
