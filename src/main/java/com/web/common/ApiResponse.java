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

        // 文章相关错误码
        public static final int ARTICLE_NOT_FOUND = 5001;
        public static final int ARTICLE_ACCESS_DENIED = 5002;
        public static final int ARTICLE_CREATE_FAILED = 5003;
        public static final int ARTICLE_UPDATE_FAILED = 5004;
        public static final int ARTICLE_DELETE_FAILED = 5005;
        public static final int LIKE_OPERATION_FAILED = 5006;
        public static final int SUBSCRIBE_OPERATION_FAILED = 5007;
        public static final int COIN_OPERATION_FAILED = 5008;
        public static final int READ_COUNT_UPDATE_FAILED = 5009;
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

    /** 文章相关错误响应方法 */
    public static <T> ApiResponse<T> articleNotFound(String message) {
        return error(ErrorCode.ARTICLE_NOT_FOUND, message);
    }

    public static <T> ApiResponse<T> articleAccessDenied(String message) {
        return error(ErrorCode.ARTICLE_ACCESS_DENIED, message);
    }

    public static <T> ApiResponse<T> articleCreateFailed(String message) {
        return error(ErrorCode.ARTICLE_CREATE_FAILED, message);
    }

    public static <T> ApiResponse<T> articleUpdateFailed(String message) {
        return error(ErrorCode.ARTICLE_UPDATE_FAILED, message);
    }

    public static <T> ApiResponse<T> articleDeleteFailed(String message) {
        return error(ErrorCode.ARTICLE_DELETE_FAILED, message);
    }

    public static <T> ApiResponse<T> likeOperationFailed(String message) {
        return error(ErrorCode.LIKE_OPERATION_FAILED, message);
    }

    public static <T> ApiResponse<T> subscribeOperationFailed(String message) {
        return error(ErrorCode.SUBSCRIBE_OPERATION_FAILED, message);
    }

    public static <T> ApiResponse<T> coinOperationFailed(String message) {
        return error(ErrorCode.COIN_OPERATION_FAILED, message);
    }

    public static <T> ApiResponse<T> readCountUpdateFailed(String message) {
        return error(ErrorCode.READ_COUNT_UPDATE_FAILED, message);
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

    /** 响应构建器模式支持 */
    public static class Builder<T> {
        private int code;
        private String message;
        private T data;
        private String path;

        public Builder<T> code(int code) {
            this.code = code;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public Builder<T> path(String path) {
            this.path = path;
            return this;
        }

        public ApiResponse<T> build() {
            ApiResponse<T> response = new ApiResponse<>(code, message, data);
            response.setPath(path);
            return response;
        }
    }

    /** 创建响应构建器 */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /** 常用消息模板 */
    public static final class Messages {
        // 成功消息
        public static final String SUCCESS = "操作成功";
        public static final String CREATED = "创建成功";
        public static final String UPDATED = "更新成功";
        public static final String DELETED = "删除成功";

        // 通用错误消息
        public static final String PARAM_ERROR = "参数错误";
        public static final String UNAUTHORIZED = "未授权访问";
        public static final String FORBIDDEN = "禁止访问";
        public static final String NOT_FOUND = "资源未找到";
        public static final String SYSTEM_ERROR = "系统错误";

        // 文章相关消息
        public static final String ARTICLE_NOT_FOUND = "文章未找到";
        public static final String ARTICLE_ACCESS_DENIED = "无权访问该文章";
        public static final String ARTICLE_CREATE_SUCCESS = "文章创建成功";
        public static final String ARTICLE_CREATE_FAILED = "文章创建失败";
        public static final String ARTICLE_UPDATE_SUCCESS = "文章更新成功";
        public static final String ARTICLE_UPDATE_FAILED = "文章更新失败";
        public static final String ARTICLE_DELETE_SUCCESS = "文章删除成功";
        public static final String ARTICLE_DELETE_FAILED = "文章删除失败";
        public static final String LIKE_SUCCESS = "点赞成功";
        public static final String LIKE_FAILED = "点赞失败";
        public static final String SUBSCRIBE_SUCCESS = "订阅成功";
        public static final String SUBSCRIBE_FAILED = "订阅失败";
        public static final String COIN_ADD_SUCCESS = "金币添加成功";
        public static final String COIN_ADD_FAILED = "金币添加失败";
        public static final String READ_COUNT_UPDATE_SUCCESS = "阅读量更新成功";
        public static final String READ_COUNT_UPDATE_FAILED = "阅读量更新失败";
    }
}
