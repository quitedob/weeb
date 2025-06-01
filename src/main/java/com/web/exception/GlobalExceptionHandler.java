package com.web.exception;

import com.web.util.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 改进版全局异常处理类，提供更安全、更详细的异常处理和日志记录。
 *
 * @author: dwh
 **/
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获未处理的通用异常。
     *
     * @param e 捕获的异常
     * @param request 当前 HTTP 请求对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(value = Exception.class)
    public Object handleException(Exception e, HttpServletRequest request) {
        // 打印异常详细日志
        log.error("未处理异常 -> {}", e.getClass(), e);
        log.error("URL -> {}", request.getRequestURL());
        log.error("HTTP Method -> {}", request.getMethod());

        // 返回通用的失败响应，隐藏敏感信息
        return ResultUtil.Fail("系统内部错误，请稍后重试。");
    }

    /**
     * 捕获参数校验异常。
     *
     * @param e 捕获的校验异常
     * @param request 当前 HTTP 请求对象
     * @return 统一格式的错误响应，包含校验错误信息
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Object validationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        // 打印校验异常的详细信息
        String errorMessage = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        log.error("参数校验异常 -> {}", e.getClass(), e);
        log.error("URL -> {}", request.getRequestURL());
        log.error("HTTP Method -> {}", request.getMethod());
        log.error("Validation Error -> {}", errorMessage);

        // 返回校验错误信息
        return ResultUtil.Fail(errorMessage);
    }

    /**
     * 捕获自定义异常。
     *
     * @param e 捕获的自定义异常
     * @param request 当前 HTTP 请求对象
     * @return 自定义格式的错误响应，包含错误码和错误信息
     */
    @ExceptionHandler(value = com.web.exception.WeebException.class)
    public Object LinyuException(WeebException e, HttpServletRequest request) {
        // 打印自定义异常的详细信息
        log.error("自定义异常 -> {}", e.getClass(), e);
        log.error("URL -> {}", request.getRequestURL());
        log.error("HTTP Method -> {}", request.getMethod());
        log.error("Custom Error Code -> {}", e.getCode());
        log.error("Custom Error Message -> {}", e.getMessage());

        // 如果异常包含附加参数，则记录参数
        if (null != e.paramToString()) {
            log.error("Exception Params -> {}", e.paramToString());
        }

        // 清空异常中的附加参数
        e.empty();

        // 返回自定义的错误响应
        return ResultUtil.Result(e.getCode(), e.getMessage());
    }
}
