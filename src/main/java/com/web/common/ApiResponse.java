package com.web.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一 API 响应格式
 * 支持业务异常、系统异常、成功响应等各种场景
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;           // 状态码，0=成功，>0=业务异常，<0=系统异常
    private String message;     // 错误消息或成功提示
    private T data;             // 具体返回数据
    private LocalDateTime timestamp; // 响应时间戳
    private String path;        // 请求路径（可选）

    // 构造函数重载
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(int code, String message) {
        this(code, message, null);
    }

    /** 快速返回成功 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "操作成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, "操作成功", null);
    }

    /** 快速返回错误 */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(-1, message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    /** 业务异常码定义 */
    public static final class ErrorCode {
        // 通用错误码
        public static final int SUCCESS = 0;
        public static final int SYSTEM_ERROR = -1;
        public static final int PARAM_ERROR = 1001;
        public static final int UNAUTHORIZED = 1002;
        public static final int FORBIDDEN = 1003;
        public static final int NOT_FOUND = 1004;
        public static final int METHOD_NOT_ALLOWED = 1005;

        // 用户相关错误码
        public static final int USER_NOT_FOUND = 2001;
        public static final int USER_ALREADY_EXISTS = 2002;
        public static final int USER_PASSWORD_ERROR = 2003;
        public static final int USER_STATUS_ERROR = 2004;

        // 文件相关错误码
        public static final int FILE_UPLOAD_ERROR = 3001;
        public static final int FILE_SIZE_EXCEED = 3002;
        public static final int FILE_TYPE_NOT_SUPPORTED = 3003;

        // 聊天相关错误码
        public static final int GROUP_NOT_FOUND = 4001;
        public static final int GROUP_PERMISSION_DENIED = 4002;
        public static final int MESSAGE_SEND_FAILED = 4003;
    }

    /** 常用成功响应方法 */
    public static <T> ApiResponse<T> ok(T data) {
        return success(data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return success(message, data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(0, "创建成功", data);
    }

    public static <T> ApiResponse<T> updated(T data) {
        return new ApiResponse<>(0, "更新成功", data);
    }

    public static <T> ApiResponse<T> deleted() {
        return new ApiResponse<>(0, "删除成功", null);
    }

    /** 常用错误响应方法 */
    public static <T> ApiResponse<T> badRequest(String message) {
        return error(ErrorCode.PARAM_ERROR, message);
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return error(ErrorCode.UNAUTHORIZED, message);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return error(ErrorCode.FORBIDDEN, message);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return error(ErrorCode.NOT_FOUND, message);
    }

    public static <T> ApiResponse<T> systemError(String message) {
        return error(ErrorCode.SYSTEM_ERROR, message);
    }

    /** 设置请求路径（在Controller中调用） */
    public ApiResponse<T> withPath(String path) {
        this.path = path;
        return this;
    }

    /** 检查是否成功 */
    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS;
    }

    /** 检查是否为业务异常 */
    public boolean isBusinessError() {
        return this.code > 0;
    }

    /** 检查是否为系统异常 */
    public boolean isSystemError() {
        return this.code < 0;
    }
}
