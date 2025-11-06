package com.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 群组权限检查注解
 * 用于Controller方法，自动检查用户是否有指定的群组权限
 * 
 * 使用示例：
 * @RequireGroupPermission(permission = GroupPermissionService.PERMISSION_EDIT_INFO)
 * public ResponseEntity<?> updateGroupInfo(@PathVariable Long groupId, ...)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireGroupPermission {
    
    /**
     * 需要的权限值（使用GroupPermissionService中定义的常量）
     */
    int permission();
    
    /**
     * 群组ID参数名称（默认为groupId）
     */
    String groupIdParam() default "groupId";
    
    /**
     * 权限不足时的错误消息
     */
    String message() default "权限不足";
}
