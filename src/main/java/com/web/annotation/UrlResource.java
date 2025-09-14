package com.web.annotation;

// 引入Java注解相关的包
import java.lang.annotation.*;

/**
 * 自定义注解 @UrlResource，用于标记方法或类，以指定与URL资源相关的信息。
 */

@Documented // 指定此注解会被 Javadoc 工具处理，生成的文档会包含注解信息
@Retention(RetentionPolicy.RUNTIME) // 指定注解在运行时有效，可以通过反射获取注解信息
@Target({ElementType.METHOD, ElementType.TYPE}) // 指定注解可以用于方法和类上
public @interface UrlResource {
    /**
     * 用于存储URL资源的值。
     * 默认为空字符串，用户可以通过注解的value属性设置具体的值。
     */
    String value() default "";
}
