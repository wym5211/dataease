package io.dataease.system.manage;

import io.dataease.api.permissions.auth.dto.BusiPerCheckDTO;
import io.dataease.system.dao.auto.entity.SysResourcePermission;
import io.dataease.system.dao.auto.entity.SysUserRole;
import io.dataease.system.dao.auto.mapper.SysResourcePermissionMapper;
import io.dataease.system.dao.auto.mapper.SysUserRoleMapper;
import io.dataease.license.config.XpackInteract;
import io.dataease.utils.AuthUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CorePermissionManage {

    private final SysResourcePermissionMapper sysResourcePermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    public CorePermissionManage(SysResourcePermissionMapper sysResourcePermissionMapper,
                                SysUserRoleMapper sysUserRoleMapper) {
        this.sysResourcePermissionMapper = sysResourcePermissionMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    @XpackInteract(value = "corePermissionManage", replace = true)
    public boolean checkAuth(BusiPerCheckDTO dto) {
        if (dto == null || dto.getId() == null) {
            return false;
        }
        if (AuthUtils.isSysAdmin()) {
            return true;
        }
        var user = AuthUtils.getUser();
        if (user == null) {
            return false;
        }
        int required = dto.getAuthEnum() == null ? 1 : mapAuth(dto.getAuthEnum().getWeight());
        String rid = String.valueOf(dto.getId());

        List<SysResourcePermission> direct = sysResourcePermissionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysResourcePermission>()
                        .eq("resource_id", rid)
                        .eq("owner_type", 0)
                        .eq("owner_id", user.getUserId()));
        if (CollectionUtils.isNotEmpty(direct) && direct.stream().anyMatch(p -> hasPermission(p.getPermission(), required))) {
            return true;
        }

        Set<Long> roleIds = sysUserRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUserRole>().eq("user_id", user.getUserId()))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return false;
        }
        List<SysResourcePermission> rolePerms = sysResourcePermissionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysResourcePermission>()
                        .eq("resource_id", rid)
                        .eq("owner_type", 1)
                        .in("owner_id", roleIds));
        return CollectionUtils.isNotEmpty(rolePerms) && rolePerms.stream().anyMatch(p -> hasPermission(p.getPermission(), required));
    }

    private boolean hasPermission(Integer actual, int required) {
        if (actual == null) {
            return false;
        }
        return (actual & required) == required;
    }

    private int mapAuth(Integer weight) {
        if (weight == null) {
            return 1;
        }
        if (weight == 7 || weight == 2) {
            return 2;
        }
        if (weight == 4) {
            return 4;
        }
        return 1;
    }
}
