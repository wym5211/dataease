package io.dataease.permissions.interceptor;

import io.dataease.permissions.annotation.RequiresLogin;
import io.dataease.permissions.annotation.RequiresPermissions;
import io.dataease.permissions.annotation.RequiresRoles;
import io.dataease.permissions.utils.PermissionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Component
public class AnnotationPermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private PermissionUtils permissionUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只处理方法级别的请求
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        Class<?> targetClass = handlerMethod.getBeanType();

        // 检查类级别的注解
        checkClassLevelAnnotations(targetClass);
        
        // 检查方法级别的注解
        checkMethodLevelAnnotations(method);

        return true;
    }

    /**
     * 检查类级别的权限注解
     */
    private void checkClassLevelAnnotations(Class<?> targetClass) {
        // 检查类级别的登录要求
        RequiresLogin classRequiresLogin = targetClass.getAnnotation(RequiresLogin.class);
        if (ObjectUtils.isNotEmpty(classRequiresLogin) && classRequiresLogin.required()) {
            permissionUtils.checkAuthentication();
        }

        // 检查类级别的角色要求
        RequiresRoles classRequiresRoles = targetClass.getAnnotation(RequiresRoles.class);
        if (ObjectUtils.isNotEmpty(classRequiresRoles)) {
            permissionUtils.checkRoles(classRequiresRoles.value(), classRequiresRoles.logical());
        }

        // 检查类级别的权限要求
        RequiresPermissions classRequiresPermissions = targetClass.getAnnotation(RequiresPermissions.class);
        if (ObjectUtils.isNotEmpty(classRequiresPermissions)) {
            permissionUtils.checkPermissions(classRequiresPermissions.value(), classRequiresPermissions.logical());
        }
    }

    /**
     * 检查方法级别的权限注解
     */
    private void checkMethodLevelAnnotations(Method method) {
        // 检查方法级别的登录要求
        RequiresLogin methodRequiresLogin = method.getAnnotation(RequiresLogin.class);
        if (ObjectUtils.isNotEmpty(methodRequiresLogin)) {
            if (methodRequiresLogin.required()) {
                permissionUtils.checkAuthentication();
            }
        } else {
            // 如果类级别有登录要求，方法级别没有明确声明，则继承类级别的要求
            RequiresLogin classRequiresLogin = method.getDeclaringClass().getAnnotation(RequiresLogin.class);
            if (ObjectUtils.isNotEmpty(classRequiresLogin) && classRequiresLogin.required()) {
                permissionUtils.checkAuthentication();
            }
        }

        // 检查方法级别的角色要求
        RequiresRoles methodRequiresRoles = method.getAnnotation(RequiresRoles.class);
        if (ObjectUtils.isNotEmpty(methodRequiresRoles)) {
            permissionUtils.checkRoles(methodRequiresRoles.value(), methodRequiresRoles.logical());
        }

        // 检查方法级别的权限要求
        RequiresPermissions methodRequiresPermissions = method.getAnnotation(RequiresPermissions.class);
        if (ObjectUtils.isNotEmpty(methodRequiresPermissions)) {
            permissionUtils.checkPermissions(methodRequiresPermissions.value(), methodRequiresPermissions.logical());
        }
    }
}