package com.web.exception; // Adjusted package name

import com.web.common.ApiResponse; // Adjusted import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError; // For more detailed validation errors
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest; // Corrected import for Spring Boot 3+
import java.util.HashMap; // For collecting multiple validation errors
import java.util.Map; // For collecting multiple validation errors


/**
 * 全局异常拦截器，捕获所有未处理的异常，并返回统一响应格式
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** 处理所有未知异常 */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Void> handleException(HttpServletRequest req, Exception e) {
    logger.error("Internal Server Error: URI={}, message={}", req.getRequestURI(), e.getMessage(), e);
    return ApiResponse.error(-1, "Internal server error: " + e.getMessage()); // Return actual message in dev might be good
  }

  /** 处理参数校验异常 (MethodArgumentNotValidException) */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });
    logger.warn("Validation Error: {}", errors, ex);
    return ApiResponse.error(1002, "Validation failed", errors);
  }

  // Example: Custom business exception
  // Assume BusinessException exists: public class BusinessException extends RuntimeException { ... }
  /*
  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST) // Or another appropriate status
  public ApiResponse<Void> handleBusinessException(BusinessException ex) {
      logger.warn("Business Exception: {}", ex.getMessage());
      return ApiResponse.error(ex.getCode(), ex.getMessage()); // Assuming BusinessException has a code
  }
  */
}
