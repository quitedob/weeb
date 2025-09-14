package com.web.annotation;

// 引入Java注解相关的包
import java.lang.annotation.*;

/**
 * 自定义注解 @UserIp，用于标记方法参数，将客户端的 IP 地址绑定到该参数。
 */

@Target(ElementType.PARAMETER) // 指定此注解可以用在方法参数上
@Retention(RetentionPolicy.RUNTIME) // 指定注解在运行时生效（可以通过反射获取）
@Documented // 指定此注解会被 Javadoc 工具处理，生成的文档会包含注解信息
public @interface UserIp {
    // 注解本身没有定义属性，起到标记作用
}

