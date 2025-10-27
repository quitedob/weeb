package com.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限验证注解
 * 用于标记需要特定权限才能访问的Controller方法
 * 
 * 使用示例：
 * @RequiresPermission("ARTICLE_DELETE_ANY")
 * public ResponseEntity<?> deleteArticle(@PathVariable Long id) { ... }
 * 
 * @RequiresPermission(value = {"ARTICLE_UPDATE_ANY", "ARTICLE_UPDATE_OWN"}, requireAll = false)
 * public ResponseEntity<?> updateArticle(@PathVariable Long id) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    
    /**
     * 需要的权限列表
     */
    String[] value();
    
    /**
     * 是否需要所有权限
     * true: 需要拥有所有指定的权限
     * false: 只需要拥有任意一个权限即可（默认）
     */
    boolean requireAll() default false;
}
