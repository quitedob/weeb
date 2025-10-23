package com.web.util;

import com.web.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
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
     * 创建字符串类型的成功响应（仅消息）
     */
    public static ResponseEntity<ApiResponse<String>> successString(String message) {
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * 创建Boolean类型的成功响应
     */
    public static ResponseEntity<ApiResponse<Boolean>> successBoolean(Boolean data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建Boolean类型的成功响应（带消息）
     */
    public static ResponseEntity<ApiResponse<Boolean>> successBoolean(Boolean data, String message) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * 创建Boolean类型的成功响应（仅消息）
     */
    public static ResponseEntity<ApiResponse<Boolean>> successBoolean(String message) {
        return ResponseEntity.ok(new ApiResponse<>(0, message, null));
    }

    /**
     * 创建Boolean类型的参数错误响应
     */
    public static ResponseEntity<ApiResponse<Boolean>> badRequestBoolean(String message) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 处理服务层异常并返回Boolean类型的错误响应
     */
    public static ResponseEntity<ApiResponse<Boolean>> handleServiceExceptionBoolean(
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
     * 创建Group类型的成功响应
     */
    public static ResponseEntity<ApiResponse<com.web.model.Group>> successGroup(com.web.model.Group data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建Group类型的成功响应（带消息）
     */
    public static ResponseEntity<ApiResponse<com.web.model.Group>> successGroup(com.web.model.Group data, String message) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * 创建Group类型的参数错误响应
     */
    public static ResponseEntity<ApiResponse<com.web.model.Group>> badRequestGroup(String message) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 处理服务层异常并返回Group类型的错误响应
     */
    public static ResponseEntity<ApiResponse<com.web.model.Group>> handleServiceExceptionGroup(
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
     * 创建List<Group>类型的成功响应
     */
    public static ResponseEntity<ApiResponse<java.util.List<com.web.model.Group>>> successGroupList(java.util.List<com.web.model.Group> data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建List<Group>类型的成功响应（带消息）
     */
    public static ResponseEntity<ApiResponse<java.util.List<com.web.model.Group>>> successGroupList(java.util.List<com.web.model.Group> data, String message) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * 处理服务层异常并返回List<Group>类型的错误响应
     */
    public static ResponseEntity<ApiResponse<java.util.List<com.web.model.Group>>> handleServiceExceptionGroupList(
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
     * 创建User类型的成功响应
     */
    public static ResponseEntity<ApiResponse<com.web.model.User>> successUser(com.web.model.User data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建User类型的成功响应（带消息）
     */
    public static ResponseEntity<ApiResponse<com.web.model.User>> successUser(com.web.model.User data, String message) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * 创建User类型的参数错误响应
     */
    public static ResponseEntity<ApiResponse<com.web.model.User>> badRequestUser(String message) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 创建User类型的未授权响应
     */
    public static ResponseEntity<ApiResponse<com.web.model.User>> unauthorizedUser(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    /**
     * 创建String类型的未授权响应
     */
    public static ResponseEntity<ApiResponse<String>> unauthorizedString(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    /**
     * 创建String类型的未找到响应
     */
    public static ResponseEntity<ApiResponse<String>> notFoundString(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    /**
     * 创建Map<String, Object>类型的未授权响应
     */
    public static ResponseEntity<ApiResponse<java.util.Map<java.lang.String, java.lang.Object>>> unauthorizedMap(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    /**
     * 创建Map<String, Object>类型的未找到响应
     */
    public static ResponseEntity<ApiResponse<java.util.Map<java.lang.String, java.lang.Object>>> notFoundMap(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    /**
     * 处理服务层异常并返回User类型的错误响应
     */
    public static ResponseEntity<ApiResponse<com.web.model.User>> handleServiceExceptionUser(
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
     * 创建UserWithStats类型的成功响应
     */
    public static ResponseEntity<ApiResponse<com.web.model.UserWithStats>> successUserWithStats(com.web.model.UserWithStats data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建UserWithStats类型的成功响应（带消息）
     */
    public static ResponseEntity<ApiResponse<com.web.model.UserWithStats>> successUserWithStats(com.web.model.UserWithStats data, String message) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * 创建UserWithStats类型的参数错误响应
     */
    public static ResponseEntity<ApiResponse<com.web.model.UserWithStats>> badRequestUserWithStats(String message) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 处理服务层异常并返回UserWithStats类型的错误响应
     */
    public static ResponseEntity<ApiResponse<com.web.model.UserWithStats>> handleServiceExceptionUserWithStats(
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
     * 创建List<User>类型的成功响应
     */
    public static ResponseEntity<ApiResponse<java.util.List<com.web.model.User>>> successUserList(java.util.List<com.web.model.User> data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建List<User>类型的成功响应（带消息）
     */
    public static ResponseEntity<ApiResponse<java.util.List<com.web.model.User>>> successUserList(java.util.List<com.web.model.User> data, String message) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * 处理服务层异常并返回List<User>类型的错误响应
     */
    public static ResponseEntity<ApiResponse<java.util.List<com.web.model.User>>> handleServiceExceptionUserList(
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
     * 创建Map<String, Object>类型的成功响应
     */
    public static ResponseEntity<ApiResponse<java.util.Map<java.lang.String, java.lang.Object>>> successMap(java.util.Map<java.lang.String, java.lang.Object> data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建Map<String, Object>类型的成功响应（带消息）
     */
    public static ResponseEntity<ApiResponse<java.util.Map<java.lang.String, java.lang.Object>>> successMap(java.util.Map<java.lang.String, java.lang.Object> data, String message) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * 创建Map<String, Object>类型的参数错误响应
     */
    public static ResponseEntity<ApiResponse<java.util.Map<java.lang.String, java.lang.Object>>> badRequestMap(String message) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 处理服务层异常并返回Map<String, Object>类型的错误响应
     */
    public static ResponseEntity<ApiResponse<java.util.Map<java.lang.String, java.lang.Object>>> handleServiceExceptionMap(
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
     * 创建List<GroupMember>类型的成功响应
     */
    public static ResponseEntity<ApiResponse<java.util.List<com.web.model.GroupMember>>> successGroupMemberList(java.util.List<com.web.model.GroupMember> data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建List<GroupMember>类型的成功响应（带消息）
     */
    public static ResponseEntity<ApiResponse<java.util.List<com.web.model.GroupMember>>> successGroupMemberList(java.util.List<com.web.model.GroupMember> data, String message) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * 处理服务层异常并返回List<GroupMember>类型的错误响应
     */
    public static ResponseEntity<ApiResponse<java.util.List<com.web.model.GroupMember>>> handleServiceExceptionGroupMemberList(
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

    /**
     * 返回字符串类型的错误响应
     */
    public static ResponseEntity<ApiResponse<String>> badRequestString(String message) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 处理服务层异常并返回字符串类型的错误响应
     */
    public static ResponseEntity<ApiResponse<String>> handleServiceExceptionString(
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
     * 处理服务层异常并返回列表类型的错误响应
     */
    public static <T> ResponseEntity<ApiResponse<List<T>>> handleServiceExceptionList(
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
     * 类型转换辅助方法：将Object类型转换为String类型响应
     */
    @SuppressWarnings("unchecked")
    public static ResponseEntity<ApiResponse<String>> convertToString(ResponseEntity<ApiResponse<Object>> response) {
        return (ResponseEntity<ApiResponse<String>>) (Object) response;
    }

    /**
     * 类型转换辅助方法：将Object类型转换为Map类型响应
     */
    @SuppressWarnings("unchecked")
    public static ResponseEntity<ApiResponse<Map<String, Object>>> convertToMap(ResponseEntity<ApiResponse<Object>> response) {
        return (ResponseEntity<ApiResponse<Map<String, Object>>>) (Object) response;
    }

    /**
     * 类型转换辅助方法：将Object类型转换为Boolean类型响应
     */
    @SuppressWarnings("unchecked")
    public static ResponseEntity<ApiResponse<Boolean>> convertToBoolean(ResponseEntity<ApiResponse<Object>> response) {
        return (ResponseEntity<ApiResponse<Boolean>>) (Object) response;
    }

    /**
     * 处理服务层异常并返回MigrationValidationReport类型的错误响应
     */
    public static ResponseEntity<ApiResponse<com.web.util.DatabaseMigrationUtil.MigrationValidationReport>> handleServiceExceptionMigration(
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
     * 返回MigrationValidationReport类型的错误响应
     */
    public static ResponseEntity<ApiResponse<com.web.util.DatabaseMigrationUtil.MigrationValidationReport>> badRequestMigration(String message) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 处理服务层异常并返回MigrationStatus类型的错误响应
     */
    public static ResponseEntity<ApiResponse<com.web.Controller.MigrationController.MigrationStatus>> handleServiceExceptionMigrationStatus(
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
     * 创建Map<String, Object>列表类型的成功响应
     */
    public static ResponseEntity<ApiResponse<java.util.List<java.util.Map<java.lang.String, java.lang.Object>>>> successMapList(java.util.List<java.util.Map<java.lang.String, java.lang.Object>> data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建Map<String, Object>列表类型的成功响应（带消息）
     */
    public static ResponseEntity<ApiResponse<java.util.List<java.util.Map<java.lang.String, java.lang.Object>>>> successMapList(java.util.List<java.util.Map<java.lang.String, java.lang.Object>> data, String message) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * 创建Map<String, Object>列表类型的参数错误响应
     */
    public static ResponseEntity<ApiResponse<java.util.List<java.util.Map<java.lang.String, java.lang.Object>>>> badRequestMapList(String message) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 处理服务层异常并返回Map<String, Object>列表类型的错误响应
     */
    public static ResponseEntity<ApiResponse<java.util.List<java.util.Map<java.lang.String, java.lang.Object>>>> handleServiceExceptionMapList(
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
}