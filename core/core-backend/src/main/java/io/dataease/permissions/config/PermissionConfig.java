package io.dataease.permissions.config;

import io.dataease.permissions.interceptor.AnnotationPermissionInterceptor;
import io.dataease.permissions.interceptor.UrlPermissionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PermissionConfig implements WebMvcConfigurer {

    @Autowired
    private UrlPermissionInterceptor urlPermissionInterceptor;

    @Autowired
    private AnnotationPermissionInterceptor annotationPermissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // URL权限拦截器，优先级较高（order=10）
        registry.addInterceptor(urlPermissionInterceptor)
                .addPathPatterns("/de2api/**")
                .order(10);

        // 注解权限拦截器，优先级较低（order=20）
        registry.addInterceptor(annotationPermissionInterceptor)
                .addPathPatterns("/de2api/**")
                .order(20);
    }
}