package io.dataease.menu.server;

import io.dataease.api.menu.MenuApi;
import io.dataease.api.menu.vo.MenuVO;
import io.dataease.license.utils.LicenseUtil;
import io.dataease.menu.dao.auto.entity.CoreMenu;
import io.dataease.menu.manage.MenuManage;
import io.dataease.system.dao.auto.entity.SysRoleMenu;
import io.dataease.system.dao.auto.entity.SysUserRole;
import io.dataease.system.dao.auto.mapper.SysRoleMenuMapper;
import io.dataease.system.dao.auto.mapper.SysUserRoleMapper;
import io.dataease.utils.AuthUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/menu")
public class MenuServer implements MenuApi {

    @Resource
    private MenuManage menuManage;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<MenuVO> query() {
        List<CoreMenu> coreMenus = menuManage.coreMenus();
        return menuManage.query(new ArrayList<>(filterMenus(coreMenus)));
    }

    @Override
    public List<MenuVO> tree() {
        // 权限管理场景：返回所有菜单，不进行权限过滤
        // 非企业版过滤掉企业版菜单（数据填报等）
        List<CoreMenu> coreMenus = menuManage.coreMenus();
        if ("community".equals(LicenseUtil.getLicenseType())) {
            coreMenus = filterXpackMenus(coreMenus);
        }
        return menuManage.query(coreMenus);
    }

    /**
     * 过滤企业版菜单（非企业版使用）
     */
    private List<CoreMenu> filterXpackMenus(List<CoreMenu> coreMenus) {
        // 企业版菜单ID列表（与 MenuManage.isXpackMenu 保持一致）
        Set<Long> xpackMenuIds = Set.of(7L, 11L, 12L, 14L, 17L, 18L, 25L, 26L, 27L, 28L,
                35L, 40L, 50L, 60L, 61L, 65L, 80L, 90L);
        Set<Long> xpackPidIds = Set.of(7L, 21L, 70L);

        return coreMenus.stream()
                .filter(m -> !xpackMenuIds.contains(m.getId()))
                .filter(m -> !xpackPidIds.contains(m.getPid()))
                .collect(Collectors.toList());
    }

    private List<CoreMenu> filterMenus(List<CoreMenu> coreMenus) {
        if (AuthUtils.isSysAdmin()) {
            return coreMenus;
        }
        var user = AuthUtils.getUser();
        if (user == null) {
            return coreMenus.stream().filter(m -> m.getAuth() == null || !m.getAuth()).toList();
        }
        Set<Long> roleIds = sysUserRoleMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUserRole>()
                .eq("user_id", user.getUserId())).stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return coreMenus.stream().filter(m -> m.getAuth() == null || !m.getAuth()).toList();
        }
        Set<Long> allowMenuIds = sysRoleMenuMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysRoleMenu>()
                .in("role_id", roleIds)).stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
        if (allowMenuIds.isEmpty()) {
            return coreMenus.stream().filter(m -> m.getAuth() == null || !m.getAuth()).toList();
        }
        Map<Long, CoreMenu> byId = coreMenus.stream().collect(Collectors.toMap(CoreMenu::getId, m -> m, (a, b) -> a));
        Set<Long> all = coreMenus.stream().filter(m -> m.getAuth() == null || !m.getAuth()).map(CoreMenu::getId).collect(Collectors.toSet());
        all.addAll(allowMenuIds);
        Set<Long> expand = new java.util.HashSet<>(all);
        for (Long mid : all) {
            CoreMenu cur = byId.get(mid);
            while (cur != null && cur.getPid() != null && cur.getPid() != 0L) {
                expand.add(cur.getPid());
                cur = byId.get(cur.getPid());
            }
        }
        return coreMenus.stream().filter(m -> expand.contains(m.getId())).toList();
    }
}
