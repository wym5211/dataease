package io.dataease.permissions.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresLogin {
    
    /**
     * 是否必须登录，默认true
     */
    boolean required() default true;
    
    /**
     * 未登录时的提示信息
     */
    String message() default "请先登录系统";
}