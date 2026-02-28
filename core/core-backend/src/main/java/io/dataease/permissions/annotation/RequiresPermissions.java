package io.dataease.permissions.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermissions {
    
    /**
     * 权限标识，支持多个权限
     * 例如：{"user:create", "user:update"}
     */
    String[] value() default {};
    
    /**
     * 逻辑操作符，默认AND
     */
    Logical logical() default Logical.AND;
    
    /**
     * 权限类型，默认资源权限
     */
    PermissionType type() default PermissionType.RESOURCE;
    
    /**
     * 逻辑操作符枚举
     */
    enum Logical {
        AND, OR
    }
    
    /**
     * 权限类型枚举
     */
    enum PermissionType {
        RESOURCE, MENU, DATA
    }
}