package com.web.annotation;

// 引入所需的类和枚举
import com.web.constant.LimitKeyType; // 自定义枚举，表示限制的关键类型（ID或IP）

import java.lang.annotation.ElementType; // 用于定义注解的适用目标
import java.lang.annotation.Retention; // 用于定义注解的保留策略
import java.lang.annotation.RetentionPolicy; // 注解保留策略枚举
import java.lang.annotation.Target; // 用于定义注解的目标范围

/**
 * 自定义注解 @UrlLimit，用于对特定方法的访问频率进行限制。
 */

@Target({ElementType.METHOD}) // 指定此注解只能作用于方法
@Retention(RetentionPolicy.RUNTIME) // 指定此注解在运行时有效，可以通过反射获取
public @interface UrlLimit {

    /**
     * 指定限制的类型（ID或IP）。
     * 默认为 `LimitKeyType.ID`，即基于用户ID进行限制。
     */
    LimitKeyType keyType() default LimitKeyType.ID;

    /**
     * 指定每分钟允许的最大请求次数。
     * 默认值为60次。
     */
    int maxRequests() default 60;
}
