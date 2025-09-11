package com.web.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解 @Userid，用于标记方法参数，将用户的 ID 值绑定到该参数。
 */
@Target(ElementType.PARAMETER)         // 注解作用目标：方法参数
@Retention(RetentionPolicy.RUNTIME)    // 注解在运行时有效
@Documented                          // 生成的文档会包含该注解信息
public @interface Userid {
    // 该注解不需要定义其他属性，仅作为标记使用
}
