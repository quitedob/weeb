package com.web.Config;

import com.web.annotation.UserIp;
import com.web.annotation.Userid;
import com.web.util.IpUtil;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 自定义参数解析器，用于解析标记了 @Userid 和 @UserIp 的方法参数
 */
public class UserInfoArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 支持解析带有 @Userid 或 @UserIp 注解的参数
        return parameter.hasParameterAnnotation(Userid.class) ||
                parameter.hasParameterAnnotation(UserIp.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  @Nullable ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  @Nullable WebDataBinderFactory binderFactory) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        // 解析 @Userid 注解的参数
        if (parameter.hasParameterAnnotation(Userid.class)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> userinfo = (Map<String, Object>) request.getAttribute("userinfo");
            if (userinfo != null && userinfo.get("userId") != null) {
                Object uid = userinfo.get("userId");
                // 如果传递的用户ID是 Long 类型，则直接返回
                if (uid instanceof Long) {
                    return uid;
                }
                // 如果是 String 类型，则尝试转换为 Long 类型
                if (uid instanceof String) {
                    try {
                        return Long.valueOf((String) uid);
                    } catch (NumberFormatException e) {
                        // 转换失败时，返回 null 或者根据业务需要抛出异常
                        return null;
                    }
                }
                // 其他类型直接返回
                return uid;
            }
        }

        // 解析 @UserIp 注解的参数
        if (parameter.hasParameterAnnotation(UserIp.class)) {
            String ipAddr = IpUtil.getIpAddr(request);
            if (ipAddr != null) {
                return ipAddr;
            }
        }
        return null;
    }
}
