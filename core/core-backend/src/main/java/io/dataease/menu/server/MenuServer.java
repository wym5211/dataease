package io.dataease.menu.server;

import io.dataease.api.menu.MenuApi;
import io.dataease.api.menu.vo.MenuVO;
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
