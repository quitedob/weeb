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
import java.util.UUID;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回标准格式的错误信息
 * 提供分类异常处理、事件追踪和详细错误日志
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 异常计数器（用于监控）
    private static final java.util.concurrent.atomic.AtomicLong exceptionCounter = new java.util.concurrent.atomic.AtomicLong(0);

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
     * 处理HTTP方法不支持异常
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupportedException(
            org.springframework.web.HttpRequestMethodNotSupportedException e, WebRequest request) {
        String eventId = logException("HTTP方法不支持", e, request);
        String path = getPath(request);

        log.warn("HTTP方法不支持 eventId={}, path={}, method={}, supported={}",
                eventId, path, e.getMethod(), e.getSupportedMethods());

        ApiResponse<Object> response = ApiResponse.error(ApiResponse.ErrorCode.METHOD_NOT_ALLOWED,
                "请求方法不支持")
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * 处理数据访问异常
     */
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccessException(
            org.springframework.dao.DataAccessException e, WebRequest request) {
        String eventId = logException("数据访问异常", e, request);
        String path = getPath(request);

        log.error("数据库操作失败 eventId={}, path={}", eventId, path, e);

        ApiResponse<Object> response = ApiResponse.systemError("数据操作失败，请稍后重试")
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理数据库约束异常
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(
            org.springframework.dao.DataIntegrityViolationException e, WebRequest request) {
        String eventId = logException("数据完整性异常", e, request);
        String path = getPath(request);

        log.error("数据完整性约束违反 eventId={}, path={}", eventId, path, e);

        String message = "数据操作失败，违反完整性约束";
        if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
            message = "数据已存在，请检查唯一性约束";
        }

        ApiResponse<Object> response = ApiResponse.error(message)
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * 处理SQL异常
     */
    @ExceptionHandler(org.springframework.jdbc.BadSqlGrammarException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadSqlGrammarException(
            org.springframework.jdbc.BadSqlGrammarException e, WebRequest request) {
        String eventId = logException("SQL语法异常", e, request);
        String path = getPath(request);

        log.error("SQL语法错误 eventId={}, path={}", eventId, path, e);

        ApiResponse<Object> response = ApiResponse.systemError("数据查询异常，请稍后重试")
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理JSON处理异常
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException e, WebRequest request) {
        String eventId = logException("消息解析异常", e, request);
        String path = getPath(request);

        log.warn("请求体格式错误 eventId={}, path={}", eventId, path, e);

        ApiResponse<Object> response = ApiResponse.badRequest("请求体格式错误，请检查数据格式")
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理缺少参数异常
     */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameterException(
            org.springframework.web.bind.MissingServletRequestParameterException e, WebRequest request) {
        String eventId = logException("缺少请求参数异常", e, request);
        String path = getPath(request);

        log.warn("缺少必需的请求参数 eventId={}, path={}, parameter={}",
                eventId, path, e.getParameterName());

        ApiResponse<Object> response = ApiResponse.badRequest("缺少必需的请求参数: " + e.getParameterName())
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理类型转换异常
     */
    @ExceptionHandler(org.springframework.beans.TypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatchException(
            org.springframework.beans.TypeMismatchException e, WebRequest request) {
        String eventId = logException("类型转换异常", e, request);
        String path = getPath(request);

        log.warn("参数类型转换错误 eventId={}, path={}, property={}, requiredType={}, value={}",
                eventId, path, e.getPropertyName(), e.getRequiredType().getSimpleName(), e.getValue());

        ApiResponse<Object> response = ApiResponse.badRequest(String.format(
                "参数 '%s' 类型错误，期望类型: %s", e.getPropertyName(), e.getRequiredType().getSimpleName()))
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 通用异常处理 - 捕获所有未处理的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUncaughtException(Exception e, WebRequest request) {
        String eventId = logException("未捕获异常", e, request);
        String path = getPath(request);

        log.error("未处理的异常 eventId={}, path={}, exceptionType={}",
                eventId, path, e.getClass().getName(), e);

        ApiResponse<Object> response = ApiResponse.systemError("系统内部错误，请联系管理员")
                .withPath(path);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 记录异常日志并生成事件ID
     */
    private String logException(String exceptionType, Exception e, WebRequest request) {
        long count = exceptionCounter.incrementAndGet();
        String eventId = UUID.randomUUID().toString();
        String path = getPath(request);

        log.error("异常统计 #{}: {} - eventId={}, path={}, message={}",
                count, exceptionType, eventId, path, e.getMessage(), e);

        return eventId;
    }

    /**
     * 获取请求路径
     */
    private String getPath(WebRequest request) {
        if (request.getDescription(false) != null) {
            return request.getDescription(false).replace("uri=", "");
        }
        return "unknown";
    }
}
