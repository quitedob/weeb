package com.web.common; // Adjusted package name

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
  private int code;        // 状态码，0=成功，>0=业务异常，<0=系统异常
  private String message;  // 错误消息或成功提示
  private T data;          // 具体返回数据

  /** 快速返回成功 */
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(0, "success", data);
  }

  public static <T> ApiResponse<T> success() {
    return new ApiResponse<>(0, "success", null);
  }

  /** 快速返回错误 */
  public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message, null);
  }

  /** 快速返回错误 with data */
  public static <T> ApiResponse<T> error(int code, String message, T data) {
      return new ApiResponse<>(code, message, data);
  }
}
