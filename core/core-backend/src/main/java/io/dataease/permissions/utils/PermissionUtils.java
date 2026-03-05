package io.dataease.permissions.utils;

import io.dataease.auth.bo.TokenUserBO;
import io.dataease.constant.AuthConstant;
import io.dataease.exception.DEException;
import io.dataease.menu.dao.auto.entity.CoreMenu;
import io.dataease.menu.dao.auto.mapper.CoreMenuMapper;
import io.dataease.permissions.annotation.RequiresPermissions;
import io.dataease.permissions.annotation.RequiresRoles;
import io.dataease.result.ResultCode;
import io.dataease.system.dao.auto.entity.SysResourcePermission;
import io.dataease.system.dao.auto.entity.SysRole;
import io.dataease.system.dao.auto.entity.SysRoleMenu;
import io.dataease.system.dao.auto.entity.SysUserRole;
import io.dataease.system.dao.auto.mapper.SysResourcePermissionMapper;
import io.dataease.system.dao.auto.mapper.SysRoleMapper;
import io.dataease.system.dao.auto.mapper.SysRoleMenuMapper;
import io.dataease.system.dao.auto.mapper.SysUserRoleMapper;
import io.dataease.utils.AuthUtils;
import io.dataease.utils.CommonBeanFactory;
import io.dataease.utils.ModelUtils;
import io.dataease.utils.WhitelistUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PermissionUtils {

    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysResourcePermissionMapper sysResourcePermissionMapper;
    private final CoreMenuMapper coreMenuMapper;

    public PermissionUtils(SysUserRoleMapper sysUserRoleMapper,
                          SysRoleMapper sysRoleMapper,
                          SysRoleMenuMapper sysRoleMenuMapper,
                          SysResourcePermissionMapper sysResourcePermissionMapper,
                          CoreMenuMapper coreMenuMapper) {
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysRoleMenuMapper = sysRoleMenuMapper;
        this.sysResourcePermissionMapper = sysResourcePermissionMapper;
        this.coreMenuMapper = coreMenuMapper;
    }

    /**
     * 检查用户是否拥有指定权限
     */
    public boolean hasPermissions(String[] permissions, RequiresPermissions.Logical logical) {
        if (ObjectUtils.isEmpty(permissions) || permissions.length == 0) {
            return true;
        }

        // 桌面模式下直接通过
        if (ModelUtils.isDesktop()) {
            return true;
        }

        TokenUserBO user = AuthUtils.getUser();
        if (ObjectUtils.isEmpty(user)) {
            return false;
        }

        // 系统管理员拥有所有权限
        if (AuthUtils.isSysAdmin(user.getUserId())) {
            return true;
        }

        Set<String> userPermissions = getUserPermissions(user.getUserId());
        
        if (logical == RequiresPermissions.Logical.AND) {
            return Arrays.stream(permissions).allMatch(userPermissions::contains);
        } else {
            return Arrays.stream(permissions).anyMatch(userPermissions::contains);
        }
    }

    /**
     * 检查用户是否拥有指定角色
     */
    public boolean hasRoles(String[] roles, RequiresRoles.Logical logical) {
        if (ObjectUtils.isEmpty(roles) || roles.length == 0) {
            return true;
        }

        // 桌面模式下直接通过
        if (ModelUtils.isDesktop()) {
            return true;
        }

        TokenUserBO user = AuthUtils.getUser();
        if (ObjectUtils.isEmpty(user)) {
            return false;
        }

        // 系统管理员拥有所有角色
        if (AuthUtils.isSysAdmin(user.getUserId())) {
            return true;
        }

        Set<String> userRoles = getUserRoles(user.getUserId());
        
        if (logical == RequiresRoles.Logical.AND) {
            return Arrays.stream(roles).allMatch(userRoles::contains);
        } else {
            return Arrays.stream(roles).anyMatch(userRoles::contains);
        }
    }

    /**
     * 检查用户是否有指定URL的访问权限
     */
    public boolean hasUrlPermission(String requestURI) {
        // 桌面模式下直接通过
        if (ModelUtils.isDesktop()) {
            return true;
        }

        TokenUserBO user = AuthUtils.getUser();
        if (ObjectUtils.isEmpty(user)) {
            return false;
        }

        // 系统管理员拥有所有权限
        if (AuthUtils.isSysAdmin(user.getUserId())) {
            return true;
        }

        // 移除context path和API前缀
        String cleanUri = cleanRequestURI(requestURI);

        // 登出API，只要是登录用户就可以访问
        if (cleanUri.equals("/logout") || cleanUri.equals("/login/logout")) {
            return true;
        }

        // 个人信息相关API，只要是登录用户就可以访问
        if (cleanUri.equals("/user/info") || cleanUri.equals("/user/personInfo") ||
            cleanUri.equals("/user/modifyPwd") || cleanUri.equals("/user/personEdit")) {
            return true;
        }

        // 菜单查询API，只要是登录用户就可以访问
        if (cleanUri.equals("/menu/query")) {
            return true;
        }

        // 可视化相关公共API，只要是登录用户就可以访问
        if (cleanUri.startsWith("/dataVisualization/")) {
            return true;
        }

        // 数据集相关公共API
        if (cleanUri.startsWith("/datasetTree/") || cleanUri.startsWith("/dataset/")) {
            return true;
        }

        // 数据源相关公共API
        if (cleanUri.startsWith("/datasource/")) {
            return true;
        }

        // 导出中心相关公共API
        if (cleanUri.startsWith("/exportCenter/")) {
            return true;
        }

        // 检查菜单权限
        return hasMenuPermission(cleanUri, user.getUserId());
    }

    /**
     * 检查用户是否已登录
     */
    public boolean checkAuthentication() {
        // 桌面模式下直接通过
        if (ModelUtils.isDesktop()) {
            return true;
        }

        TokenUserBO user = AuthUtils.getUser();
        return ObjectUtils.isNotEmpty(user);
    }

    /**
     * 获取用户权限列表
     */
    private Set<String> getUserPermissions(Long userId) {
        Set<String> permissions = new HashSet<>();
        
        // 获取用户角色
        Set<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return permissions;
        }

        // 获取角色对应的资源权限
        List<SysResourcePermission> resourcePermissions = sysResourcePermissionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysResourcePermission>()
                .in("owner_id", roleIds)
                .eq("owner_type", 1) // 1: role
        );

        // 这里需要根据具体的权限标识规则来构建权限字符串
        // 暂时返回资源ID作为权限标识
        resourcePermissions.forEach(permission -> {
            if (StringUtils.isNotBlank(permission.getResourceId())) {
                permissions.add(permission.getResourceType() + ":" + permission.getResourceId());
            }
        });

        return permissions;
    }

    /**
     * 获取用户角色列表
     */
    private Set<String> getUserRoles(Long userId) {
        Set<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return new HashSet<>();
        }

        List<SysRole> roles = sysRoleMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysRole>()
                .in("id", roleIds)
        );

        return roles.stream()
                .map(SysRole::getRoleAlias)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 获取用户角色ID列表
     */
    private Set<Long> getUserRoleIds(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUserRole>()
                .eq("user_id", userId)
        );

        return userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toSet());
    }

    /**
     * 检查菜单权限
     */
    private boolean hasMenuPermission(String requestURI, Long userId) {
        // 获取用户角色
        Set<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return false;
        }

        // 获取角色对应的菜单权限
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysRoleMenu>()
                .in("role_id", roleIds)
        );

        if (roleMenus.isEmpty()) {
            return false;
        }

        Set<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toSet());

        // 根据请求URI查找对应的菜单
        List<CoreMenu> menus = coreMenuMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CoreMenu>()
                .in("id", menuIds)
                .eq("auth", true) // 参与授权的菜单
        );

        // 检查请求的URI是否在用户有权限的菜单中
        return menus.stream().anyMatch(menu -> {
            String menuPath = menu.getPath();
            return StringUtils.isNotBlank(menuPath) && requestURI.startsWith(menuPath);
        });
    }

    /**
     * 清理请求URI
     */
    private String cleanRequestURI(String requestURI) {
        String cleanUri = requestURI;
        
        // 移除context path
        String contextPath = WhitelistUtils.getContextPath();
        if (StringUtils.isNotBlank(contextPath) && cleanUri.startsWith(contextPath)) {
            cleanUri = cleanUri.substring(contextPath.length());
        }
        
        // 移除API前缀
        if (cleanUri.startsWith(AuthConstant.DE_API_PREFIX)) {
            cleanUri = cleanUri.substring(AuthConstant.DE_API_PREFIX.length());
        }
        
        return cleanUri;
    }

    /**
     * 抛出权限异常
     */
    public void throwPermissionException(String message) {
        throw new DEException(ResultCode.PERMISSION_NO_ACCESS.code(), message);
    }

    /**
     * 检查权限，不满足时抛出异常
     */
    public void checkPermissions(String[] permissions, RequiresPermissions.Logical logical) {
        if (!hasPermissions(permissions, logical)) {
            throwPermissionException("没有权限访问该资源");
        }
    }

    /**
     * 检查角色，不满足时抛出异常
     */
    public void checkRoles(String[] roles, RequiresRoles.Logical logical) {
        if (!hasRoles(roles, logical)) {
            throwPermissionException("没有角色访问该资源");
        }
    }

    /**
     * 检查URL权限，不满足时抛出异常
     */
    public void checkUrlPermission(String requestURI) {
        if (!hasUrlPermission(requestURI)) {
            throwPermissionException("没有权限访问该接口");
        }
    }

    /**
     * 检查登录状态，不满足时抛出异常
     */
    public void requireAuthentication() {
        if (!checkAuthentication()) {
            throwPermissionException("请先登录系统");
        }
    }
}