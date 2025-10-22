package com.web.util;

import com.web.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/**
 * API响应工具类
 * 提供统一的API响应处理方法，标准化错误处理模式
 */
@Slf4j
public class ApiResponseUtil {

    /**
     * 处理服务层异常并返回统一的错误响应
     */
    public static ResponseEntity<ApiResponse<Object>> handleServiceException(
            Exception e, String operation, Object... params) {
        String eventId = UUID.randomUUID().toString();
        String logMessage = String.format("%s 操作失败 eventId=%s", operation, eventId);

        if (params.length > 0) {
            logMessage += String.format(", params=%s", java.util.Arrays.toString(params));
        }

        log.error(logMessage, e);

        // 根据异常类型决定响应状态和错误消息
        if (e instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("参数错误: " + e.getMessage()));
        }

        return ResponseEntity.internalServerError()
                .body(ApiResponse.systemError(String.format("%s失败", operation) + ": " + e.getMessage()));
    }

    /**
     * 处理业务异常并返回统一的错误响应
     */
    public static <T> ResponseEntity<ApiResponse<T>> handleBusinessException(
            RuntimeException e, String operation, Object... params) {
        String eventId = UUID.randomUUID().toString();
        String logMessage = String.format("%s 操作失败 eventId=%s", operation, eventId);

        if (params.length > 0) {
            logMessage += String.format(", params=%s", java.util.Arrays.toString(params));
        }

        log.warn(logMessage, e);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * 创建成功响应
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建成功响应（带消息）
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * 创建成功响应（仅消息）
     */
    public static ResponseEntity<ApiResponse<Object>> success(String message) {
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * 创建参数错误响应
     */
    public static ResponseEntity<ApiResponse<Object>> badRequest(String message) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 创建未找到响应
     */
    public static ResponseEntity<ApiResponse<Object>> notFound(String message) {
        return ResponseEntity.notFound()
                .build();
    }

    /**
     * 创建权限不足响应
     */
    public static ResponseEntity<ApiResponse<Object>> forbidden(String message) {
        return ResponseEntity.status(403)
                .body(ApiResponse.error(message));
    }

    /**
     * 创建未认证响应
     */
    public static ResponseEntity<ApiResponse<Object>> unauthorized(String message) {
        return ResponseEntity.status(401)
                .body(ApiResponse.error(message));
    }

    /**
     * 验证参数是否为空
     */
    public static void validateNotNull(Object obj, String paramName) {
        if (obj == null) {
            throw new IllegalArgumentException(paramName + "不能为空");
        }
    }

    /**
     * 验证字符串参数是否为空或空白
     */
    public static void validateNotBlank(String str, String paramName) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + "不能为空");
        }
    }

    /**
     * 验证ID参数是否有效
     */
    public static void validateId(Long id, String paramName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(paramName + "必须为正数");
        }
    }

    /**
     * 验证分页参数
     */
    public static void validatePageParams(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("每页大小必须在1-100之间");
        }
    }
}