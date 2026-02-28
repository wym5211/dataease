package io.dataease.permissions.interceptor;

import io.dataease.permissions.utils.PermissionUtils;
import io.dataease.utils.WhitelistUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UrlPermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private PermissionUtils permissionUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        // 白名单路径直接放行
        if (WhitelistUtils.match(requestURI)) {
            return true;
        }
        
        // 检查URL权限
        permissionUtils.checkUrlPermission(requestURI);
        
        return true;
    }
}