package io.dataease.permissions.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRoles {
    
    /**
     * 角色标识，支持多个角色
     * 例如：{"admin", "user"}
     */
    String[] value() default {};
    
    /**
     * 逻辑操作符，默认AND
     */
    Logical logical() default Logical.AND;
    
    /**
     * 逻辑操作符枚举
     */
    enum Logical {
        AND, OR
    }
}