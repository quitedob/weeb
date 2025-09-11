package com.web.exception;

import cn.hutool.json.JSONUtil;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 改进版 LinyuException，自定义异常类，支持更高的线程安全性和扩展性。
 * 提供功能：
 * - 自定义异常代码和消息。
 * - 附加详细上下文参数。
 * - 将参数转换为 JSON 字符串。
 * - 增加扩展数据支持。
 *
 * @author: dwh
 **/
public class WeebException extends RuntimeException {

    // 异常代码，表示错误类型
    private final int code;

    // 异常信息
    private final String message;

    // 附加参数，用于传递上下文信息
    private final Map<String, Object> param;

    // 扩展数据，用于存储更多上下文信息
    private final Object extraData;

    // 定义默认错误代码常量
    private static final int DEFAULT_ERROR_CODE = 1;

    /**
     * 构造函数，仅传递异常信息，默认错误代码。
     *
     * @param message 异常描述信息
     */
    public WeebException(String message) {
        this(DEFAULT_ERROR_CODE, message, null, null);
    }

    /**
     * 构造函数，传递异常代码和异常信息。
     *
     * @param code 异常代码
     * @param message 异常描述信息
     */
    public WeebException(int code, String message) {
        this(code, message, null, null);
    }

    /**
     * 构造函数，传递异常代码、异常信息和附加参数。
     *
     * @param code 异常代码
     * @param message 异常描述信息
     * @param param 附加参数（键值对形式）
     */
    public WeebException(int code, String message, Map<String, Object> param) {
        this(code, message, param, null);
    }

    /**
     * 构造函数，传递完整信息。
     *
     * @param code 异常代码
     * @param message 异常描述信息
     * @param param 附加参数（键值对形式）
     * @param extraData 扩展数据
     */
    public WeebException(int code, String message, Map<String, Object> param, Object extraData) {
        this.code = code;
        this.message = message;
        this.param = param == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(param);
        this.extraData = extraData;
    }

    /**
     * 添加异常参数的键值对（线程安全）。
     *
     * @param key 键名
     * @param value 键值
     * @return 当前异常对象（支持链式调用）
     */
    public WeebException param(String key, Object value) {
        this.param.put(key, value);
        return this;
    }

    /**
     * 清空附加参数（线程安全）。
     *
     * @return 当前异常对象（支持链式调用）
     */
    public WeebException empty() {
        this.param.clear();
        return this;
    }

    /**
     * 获取异常代码。
     *
     * @return 异常代码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取异常描述信息。
     *
     * @return 异常描述信息
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 获取附加参数的不可变副本。
     *
     * @return 不可变的附加参数
     */
    public Map<String, Object> getParam() {
        return Collections.unmodifiableMap(param);
    }

    /**
     * 获取扩展数据。
     *
     * @return 扩展数据
     */
    public Object getExtraData() {
        return extraData;
    }

    /**
     * 将附加参数转换为 JSON 字符串。
     *
     * @return JSON 字符串形式的参数信息，若参数为空则返回 null
     */
    public String paramToString() {
        if (param.isEmpty()) {
            return null;
        }
        return JSONUtil.toJsonStr(param);
    }
}
