package com.web.util;

import cn.hutool.json.JSONObject;

/**
 * 结果工具类，用于统一构建 API 返回结果
 */
public class ResultUtil {

    public enum ResponseEnum {
        SUCCEED(0),
        FAIL(1),
        TOKEN_INVALID(-1), // token失效
        FORBIDDEN(-2),     // 没有权限
        LOGIN_ELSEWHERE(-3); // 其他地方登录

        private final int type;

        ResponseEnum(int type) {
            this.type = type;
        }

        public int getType() {
            return this.type;
        }
    }

    public static final String CODE = "code";
    public static final String MSG = "msg";
    public static final String DATA = "data";

    /**
     * 根据条件返回
     *
     * @param flag 条件标志
     * @return 成功或失败的 JSON 对象
     */
    public static JSONObject ResultByFlag(boolean flag) {
        if (flag) {
            return Succeed();
        } else {
            return Fail();
        }
    }

    /**
     * 根据条件返回
     *
     * @param flag 条件标志
     * @param msg  失败时的消息
     * @param data 失败时的附加数据
     * @return 成功或失败的 JSON 对象
     */
    public static JSONObject ResultByFlag(boolean flag, String msg, Object data) {
        if (flag) {
            return Succeed();
        } else {
            return Fail(msg);
        }
    }

    /**
     * 成功没有返回数据
     *
     * @return 成功的 JSON 对象
     */
    public static JSONObject Succeed() {
        JSONObject result = new JSONObject();
        result.put(CODE, ResponseEnum.SUCCEED.getType());
        result.put(MSG, "操作成功");
        return result;
    }

    /**
     * 成功有返回数据
     *
     * @param data 返回的数据
     * @return 成功的 JSON 对象
     */
    public static JSONObject Succeed(Object data) {
        JSONObject result = new JSONObject();
        result.put(CODE, ResponseEnum.SUCCEED.getType());
        result.put(DATA, data);
        return result;
    }

    /**
     * 成功有返回消息和返回数据和提示
     *
     * @param msg  返回的消息
     * @param data 返回的数据
     * @return 成功的 JSON 对象
     */
    public static JSONObject Succeed(String msg, Object data) {
        JSONObject result = new JSONObject();
        result.put(CODE, ResponseEnum.SUCCEED.getType());
        result.put(MSG, msg);
        result.put(DATA, data);
        return result;
    }

    /**
     * 失败没有返回数据
     *
     * @return 失败的 JSON 对象
     */
    public static JSONObject Fail() {
        JSONObject result = new JSONObject();
        result.put(CODE, ResponseEnum.FAIL.getType());
        result.put(MSG, "操作失败");
        return result;
    }

    /**
     * 失败有返回消息
     *
     * @param msg 返回的失败消息
     * @return 失败的 JSON 对象
     */
    public static JSONObject Fail(String msg) {
        JSONObject result = new JSONObject();
        result.put(CODE, ResponseEnum.FAIL.getType());
        result.put(MSG, msg);
        return result;
    }

    /**
     * 自定义的返回
     *
     * @param code 返回的状态码
     * @param msg  返回的消息
     * @param data 返回的数据
     * @return 自定义的 JSON 对象
     */
    public static JSONObject Result(int code, String msg, Object data) {
        JSONObject result = new JSONObject();
        result.put(CODE, code);
        result.put(MSG, msg);
        result.put(DATA, data);
        return result;
    }

    /**
     * 自定义的返回
     *
     * @param code 返回的状态码
     * @param msg  返回的消息
     * @return 自定义的 JSON 对象
     */
    public static JSONObject Result(int code, String msg) {
        JSONObject result = new JSONObject();
        result.put(CODE, code);
        result.put(MSG, msg);
        return result;
    }

    /**
     * 失败有返回消息和返回数据
     *
     * @param msg  返回的失败消息
     * @param data 返回的数据
     * @return 失败的 JSON 对象
     */
    public static JSONObject Fail(String msg, Object data) {
        JSONObject result = new JSONObject();
        result.put(CODE, ResponseEnum.FAIL.getType());
        result.put(MSG, msg);
        result.put(DATA, data);
        return result;
    }

    /**
     * token失效
     *
     * @return token失效的 JSON 对象
     */
    public static JSONObject TokenInvalid() {
        JSONObject result = new JSONObject();
        result.put(CODE, ResponseEnum.TOKEN_INVALID.getType());
        result.put(MSG, "认证失效,请重新登录~");
        return result;
    }

    /**
     * 没有权限
     *
     * @return 没有权限的 JSON 对象
     */
    public static JSONObject Forbidden() {
        JSONObject result = new JSONObject();
        result.put(CODE, ResponseEnum.FORBIDDEN.getType());
        result.put(MSG, "该用户没有权限~");
        return result;
    }

    /**
     * 其他地方登录
     *
     * @return 其他地方登录的 JSON 对象
     */
    public static JSONObject LoginElsewhere() {
        JSONObject result = new JSONObject();
        result.put(CODE, ResponseEnum.LOGIN_ELSEWHERE.getType());
        result.put(MSG, "已在其它地方登录,请重新登录~");
        return result;
    }
}
