package com.web.annotation;

// 引入Java注解相关的包
import java.lang.annotation.*;

/**
 * 自定义注解 @UrlFree，用于标记某些方法或类不受特定规则（如访问限制、权限校验等）的约束。
 */

@Documented // 指定此注解会被 Javadoc 工具处理，生成的文档会包含注解信息
@Retention(RetentionPolicy.RUNTIME) // 指定注解在运行时有效，可以通过反射获取注解信息
@Target({ElementType.METHOD, ElementType.TYPE}) // 指定此注解可以应用于方法和类上
public @interface UrlFree {
    /**
     * 用于描述当前标记的自由资源的附加信息。
     * 默认为空字符串。
     */
    String value() default "";
}
