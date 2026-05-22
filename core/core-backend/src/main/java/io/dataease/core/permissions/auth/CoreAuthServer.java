package io.dataease.core.permissions.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.dataease.api.permissions.auth.api.AuthApi;
import io.dataease.api.permissions.auth.dto.*;
import io.dataease.api.permissions.auth.vo.PermissionItem;
import io.dataease.api.permissions.auth.vo.PermissionOrigin;
import io.dataease.api.permissions.auth.vo.PermissionVO;
import io.dataease.api.permissions.auth.vo.ResourceVO;
import io.dataease.exception.DEException;
import io.dataease.datasource.manage.DataSourceManage;
import io.dataease.dataset.manage.DatasetGroupManage;
import io.dataease.menu.dao.auto.entity.CoreMenu;
import io.dataease.menu.manage.MenuManage;
import io.dataease.model.BusiNodeRequest;
import io.dataease.model.BusiNodeVO;
import io.dataease.system.dao.auto.entity.SysResourcePermission;
import io.dataease.system.dao.auto.entity.SysRole;
import io.dataease.system.dao.auto.entity.SysRoleMenu;
import io.dataease.system.dao.auto.entity.SysUser;
import io.dataease.system.dao.auto.entity.SysUserRole;
import io.dataease.system.dao.auto.mapper.SysResourcePermissionMapper;
import io.dataease.system.dao.auto.mapper.SysRoleMapper;
import io.dataease.system.dao.auto.mapper.SysRoleMenuMapper;
import io.dataease.system.dao.auto.mapper.SysUserMapper;
import io.dataease.system.dao.auto.mapper.SysUserRoleMapper;
import io.dataease.permissions.notify.PermissionChangeNotifier;
import io.dataease.constant.CacheConstant;
import io.dataease.utils.AuthUtils;
import io.dataease.utils.CacheUtils;
import io.dataease.visualization.manage.CoreVisualizationManage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@Service("authServer")
@Primary
@RestController
@RequestMapping("/auth")
public class CoreAuthServer implements AuthApi {

    @Autowired
    private MenuManage menuManage;
    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private SysResourcePermissionMapper sysResourcePermissionMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private CoreVisualizationManage coreVisualizationManage;
    @Autowired
    private DatasetGroupManage datasetGroupManage;
    @Autowired
    private DataSourceManage dataSourceManage;
    @Autowired
    private PermissionChangeNotifier permissionChangeNotifier;

    @Override
    public List<ResourceVO> busiResource(String flag) {
        requireAdmin();
        if (StringUtils.isBlank(flag)) {
            return Collections.emptyList();
        }
        String f = normalizeFlag(flag);
        if (StringUtils.equals(f, "dashboard-dataV")) {
            return busiResourceDashboardAndDataV();
        }
        List<BusiNodeVO> nodes = switch (f) {
            case "dashboard", "dataV" -> {
                BusiNodeRequest req = new BusiNodeRequest();
                req.setBusiFlag(f);
                req.setLeaf(null);
                req.setResourceTable("core");
                yield coreVisualizationManage.tree(req);
            }
            case "dataset" -> {
                BusiNodeRequest req = new BusiNodeRequest();
                req.setBusiFlag("dataset");
                req.setLeaf(null);
                req.setResourceTable("core");
                yield datasetGroupManage.tree(req);
            }
            case "datasource" -> {
                BusiNodeRequest req = new BusiNodeRequest();
                req.setBusiFlag("datasource");
                req.setLeaf(null);
                req.setResourceTable("core");
                yield dataSourceManage.tree(req);
            }
            default -> Collections.emptyList();
        };
        return toResourceVOList(nodes);
    }

    @Override
    public PermissionVO busiPermission(BusiPermissionRequest request) {
        requireAdmin();
        if (request == null) {
            return emptyPermission();
        }
        QueryWrapper<SysResourcePermission> qw = new QueryWrapper<>();
        qw.eq("owner_id", request.getId());
        qw.eq("owner_type", request.getType());
        qw.eq("resource_type", request.getFlag());
        List<SysResourcePermission> list = sysResourcePermissionMapper.selectList(qw);
        PermissionVO vo = emptyPermission();
        vo.setPermissions(list.stream().map(this::toPermissionItem).filter(Objects::nonNull).toList());
        return vo;
    }

    @Override
    public PermissionVO busiTargetPermission(BusiPermissionRequest request) {
        requireAdmin();
        if (request == null) {
            return emptyPermission();
        }
        QueryWrapper<SysResourcePermission> qw = new QueryWrapper<>();
        qw.eq("resource_id", String.valueOf(request.getId()));
        qw.eq("resource_type", request.getFlag());
        if (request.getType() != null) {
            qw.eq("owner_type", request.getType());
        }
        List<SysResourcePermission> list = sysResourcePermissionMapper.selectList(qw);
        Map<Long, List<SysResourcePermission>> byOwner = list.stream().collect(Collectors.groupingBy(SysResourcePermission::getOwnerId));
        PermissionVO vo = emptyPermission();
        vo.setPermissionOrigins(byOwner.entrySet().stream().map(e -> toPermissionOrigin(request.getType(), e.getKey(), e.getValue())).toList());
        return vo;
    }

    @Override
    public List<ResourceVO> menuResource() {
        requireAdmin();
        List<CoreMenu> menus = menuManage.coreMenus();
        Map<Long, List<CoreMenu>> children = menus.stream().collect(Collectors.groupingBy(m -> m.getPid() == null ? 0L : m.getPid()));
        List<CoreMenu> roots = children.getOrDefault(0L, Collections.emptyList());
        return roots.stream().map(r -> toMenuResource(r, children)).toList();
    }

    @Override
    public PermissionVO menuPermission(MenuPermissionRequest request) {
        requireAdmin();
        if (request == null || request.getId() == null) {
            return emptyPermission();
        }
        Long rid = request.getId();
        SysRole role = sysRoleMapper.selectById(rid);
        QueryWrapper<SysRoleMenu> qw = new QueryWrapper<>();
        qw.eq("role_id", rid);
        List<SysRoleMenu> list = sysRoleMenuMapper.selectList(qw);
        PermissionVO vo = emptyPermission();
        if (role != null) {
            vo.setReadonly(role.getType() != null && role.getType() == 0);
            vo.setRoot(role.getId() != null && role.getId() == 1L);
        }
        vo.setPermissions(list.stream().map(rm -> toMenuPermissionItem(rm.getMenuId())).toList());
        return vo;
    }

    @Override
    public PermissionVO menuTargetPermission(MenuPermissionRequest request) {
        requireAdmin();
        if (request == null || request.getId() == null) {
            return emptyPermission();
        }
        Long menuId = request.getId();
        QueryWrapper<SysRoleMenu> qw = new QueryWrapper<>();
        qw.eq("menu_id", menuId);
        List<SysRoleMenu> list = sysRoleMenuMapper.selectList(qw);
        Map<Long, List<SysRoleMenu>> byRole = list.stream().collect(Collectors.groupingBy(SysRoleMenu::getRoleId));
        PermissionVO vo = emptyPermission();
        vo.setPermissionOrigins(byRole.entrySet().stream().map(e -> {
            SysRole role = sysRoleMapper.selectById(e.getKey());
            PermissionOrigin origin = new PermissionOrigin();
            origin.setId(e.getKey());
            origin.setName(role == null ? String.valueOf(e.getKey()) : role.getName());
            origin.setPermissions(List.of(toMenuPermissionItem(menuId)));
            return origin;
        }).toList());
        return vo;
    }

    @Override
    @Transactional
    public void saveBusiPer(BusiPerEditor editor) {
        requireAdmin();
        if (editor == null || editor.getId() == null || editor.getType() == null) {
            return;
        }
        QueryWrapper<SysResourcePermission> del = new QueryWrapper<>();
        del.eq("owner_id", editor.getId());
        del.eq("owner_type", editor.getType());
        del.eq("resource_type", editor.getFlag());
        sysResourcePermissionMapper.delete(del);
        if (CollectionUtils.isEmpty(editor.getPermissions())) {
            return;
        }
        for (PermissionItem item : editor.getPermissions()) {
            if (item == null || item.getId() == null) {
                continue;
            }
            SysResourcePermission p = new SysResourcePermission();
            p.setOwnerId(editor.getId());
            p.setOwnerType(editor.getType());
            p.setResourceType(editor.getFlag());
            p.setResourceId(String.valueOf(item.getId()));
            p.setPermission(item.getWeight());
            sysResourcePermissionMapper.insert(p);
        }
        // 资源权限变更后失效缓存并通知相关用户
        List<Long> userIds = evictBusiPerCaches(editor.getType(), editor.getId());
        permissionChangeNotifier.notifyUsers(userIds, "RESOURCE");
    }

    @Override
    @Transactional
    public void saveBusiTargetPer(BusiTargetPerCreator creator) {
        requireAdmin();
        if (creator == null || creator.getType() == null || CollectionUtils.isEmpty(creator.getIds())) {
            return;
        }
        for (Long resourceId : creator.getIds()) {
            QueryWrapper<SysResourcePermission> del = new QueryWrapper<>();
            del.eq("resource_id", String.valueOf(resourceId));
            del.eq("resource_type", creator.getFlag());
            del.eq("owner_type", creator.getType());
            sysResourcePermissionMapper.delete(del);
            if (CollectionUtils.isEmpty(creator.getPermissions())) {
                continue;
            }
            for (PermissionItem item : creator.getPermissions()) {
                if (item == null || item.getId() == null) {
                    continue;
                }
                SysResourcePermission p = new SysResourcePermission();
                p.setOwnerId(item.getId());
                p.setOwnerType(creator.getType());
                p.setResourceType(creator.getFlag());
                p.setResourceId(String.valueOf(resourceId));
                p.setPermission(item.getWeight());
                sysResourcePermissionMapper.insert(p);
            }
        }
        // 根据 owner_type 清除缓存并通知相关用户刷新资源权限
        if (creator.getType() != null && CollectionUtils.isNotEmpty(creator.getPermissions())) {
            for (PermissionItem item : creator.getPermissions()) {
                if (item == null || item.getId() == null) continue;
                List<Long> ids = evictBusiPerCaches(creator.getType(), item.getId());
                permissionChangeNotifier.notifyUsers(ids, "RESOURCE");
            }
        }
    }

    @Override
    @Transactional
    public void saveMenuPer(MenuPerEditor editor) {
        requireAdmin();
        if (editor == null || editor.getId() == null) {
            return;
        }
        Long rid = editor.getId();
        sysRoleMenuMapper.delete(new QueryWrapper<SysRoleMenu>().eq("role_id", rid));
        if (CollectionUtils.isEmpty(editor.getPermissions())) {
            return;
        }
        for (PermissionItem item : editor.getPermissions()) {
            if (item == null || item.getId() == null) {
                continue;
            }
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(rid);
            rm.setMenuId(item.getId());
            sysRoleMenuMapper.insert(rm);
        }
        // 角色菜单权限变更后，清除该角色下所有用户的权限缓存并通知
        List<Long> userIds = evictRoleCaches(rid);
        permissionChangeNotifier.notifyUsers(userIds, "MENU");
    }

    @Override
    @Transactional
    public void saveMenuTargetPer(MenuTargetPerCreator creator) {
        requireAdmin();
        if (creator == null || CollectionUtils.isEmpty(creator.getIds())) {
            return;
        }
        List<PermissionItem> targets = creator.getPermissions();
        for (Long menuId : creator.getIds()) {
            sysRoleMenuMapper.delete(new QueryWrapper<SysRoleMenu>().eq("menu_id", menuId));
            if (CollectionUtils.isEmpty(targets)) {
                continue;
            }
            for (PermissionItem item : targets) {
                if (item == null || item.getId() == null) {
                    continue;
                }
                SysRoleMenu rm = new SysRoleMenu();
                rm.setMenuId(menuId);
                rm.setRoleId(item.getId());
                sysRoleMenuMapper.insert(rm);
            }
        }
    }

    private PermissionVO emptyPermission() {
        PermissionVO vo = new PermissionVO();
        vo.setRoot(false);
        vo.setReadonly(false);
        vo.setPermissions(Collections.emptyList());
        vo.setPermissionOrigins(Collections.emptyList());
        return vo;
    }

    private PermissionItem toMenuPermissionItem(Long menuId) {
        PermissionItem item = new PermissionItem();
        item.setId(menuId);
        item.setWeight(1);
        return item;
    }

    private PermissionItem toPermissionItem(SysResourcePermission p) {
        Long id;
        try {
            id = Long.parseLong(p.getResourceId());
        } catch (Exception e) {
            return null;
        }
        PermissionItem item = new PermissionItem();
        item.setId(id);
        item.setWeight(p.getPermission() == null ? 1 : p.getPermission());
        return item;
    }

    private PermissionOrigin toPermissionOrigin(Integer ownerType, Long ownerId, List<SysResourcePermission> items) {
        PermissionOrigin origin = new PermissionOrigin();
        origin.setId(ownerId);
        origin.setName(resolveOwnerName(ownerType, ownerId));
        origin.setPermissions(items.stream().map(this::toPermissionItem).filter(Objects::nonNull).toList());
        return origin;
    }

    private String resolveOwnerName(Integer ownerType, Long ownerId) {
        if (ownerType == null) {
            return String.valueOf(ownerId);
        }
        if (ownerType == 1) {
            SysRole role = sysRoleMapper.selectById(ownerId);
            return role == null ? String.valueOf(ownerId) : role.getName();
        }
        if (ownerType == 0) {
            SysUser user = sysUserMapper.selectById(ownerId);
            return user == null ? String.valueOf(ownerId) : (user.getNickName() == null ? user.getUsername() : user.getNickName());
        }
        return String.valueOf(ownerId);
    }

    private ResourceVO toMenuResource(CoreMenu menu, Map<Long, List<CoreMenu>> children) {
        ResourceVO vo = new ResourceVO();
        vo.setId(menu.getId());
        vo.setName(menu.getName());
        List<CoreMenu> child = children.get(menu.getId());
        if (CollectionUtils.isEmpty(child)) {
            vo.setLeaf(true);
            vo.setChildren(Collections.emptyList());
            return vo;
        }
        vo.setChildren(child.stream().map(m -> toMenuResource(m, children)).toList());
        return vo;
    }

    private List<ResourceVO> busiResourceDashboardAndDataV() {
        BusiNodeRequest dashboardReq = new BusiNodeRequest();
        dashboardReq.setBusiFlag("dashboard");
        dashboardReq.setLeaf(null);
        dashboardReq.setResourceTable("core");

        BusiNodeRequest dataVReq = new BusiNodeRequest();
        dataVReq.setBusiFlag("dataV");
        dataVReq.setLeaf(null);
        dataVReq.setResourceTable("core");

        List<ResourceVO> dashboardTree = toResourceVOList(stripRoot(coreVisualizationManage.tree(dashboardReq)));
        List<ResourceVO> dataVTree = toResourceVOList(stripRoot(coreVisualizationManage.tree(dataVReq)));

        ResourceVO dashboardRoot = new ResourceVO();
        dashboardRoot.setId(-1000L);
        dashboardRoot.setName("dashboard");
        dashboardRoot.setChildren(dashboardTree);

        ResourceVO dataVRoot = new ResourceVO();
        dataVRoot.setId(-1001L);
        dataVRoot.setName("dataV");
        dataVRoot.setChildren(dataVTree);

        return List.of(dashboardRoot, dataVRoot);
    }

    private String normalizeFlag(String flag) {
        if (StringUtils.equalsAnyIgnoreCase(flag, "dashboard", "dashboard-copy")) {
            return "dashboard";
        }
        if (StringUtils.equalsAnyIgnoreCase(flag, "dataV", "dataV-copy")) {
            return "dataV";
        }
        if (StringUtils.equalsIgnoreCase(flag, "dashboard-dataV")) {
            return "dashboard-dataV";
        }
        if (StringUtils.equalsIgnoreCase(flag, "dataset")) {
            return "dataset";
        }
        if (StringUtils.equalsIgnoreCase(flag, "datasource")) {
            return "datasource";
        }
        return flag;
    }

    private List<BusiNodeVO> stripRoot(List<BusiNodeVO> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return Collections.emptyList();
        }
        if (nodes.size() == 1) {
            BusiNodeVO root = nodes.getFirst();
            if (CollectionUtils.isNotEmpty(root.getChildren())) {
                return root.getChildren();
            }
        }
        return nodes;
    }

    private List<ResourceVO> toResourceVOList(List<BusiNodeVO> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return Collections.emptyList();
        }
        return nodes.stream().map(this::toResourceVO).toList();
    }

    private ResourceVO toResourceVO(BusiNodeVO node) {
        ResourceVO vo = new ResourceVO();
        vo.setId(node.getId());
        vo.setName(node.getName());
        vo.setLeaf(Boolean.TRUE.equals(node.getLeaf()));
        vo.setExtraFlag(node.getExtraFlag());
        if (CollectionUtils.isEmpty(node.getChildren())) {
            vo.setChildren(Collections.emptyList());
            return vo;
        }
        vo.setChildren(node.getChildren().stream().map(this::toResourceVO).toList());
        return vo;
    }

    private void requireAdmin() {
        if (!AuthUtils.isSysAdmin()) {
            DEException.throwException(io.dataease.result.ResultCode.INTERFACE_FORBID_VISIT.code(), io.dataease.result.ResultCode.INTERFACE_FORBID_VISIT.message());
        }
    }

    /**
     * 根据 owner_type 清除资源权限缓存
     *
     * @param ownerType 0=用户 1=角色 2=部门
     * @param ownerId   owner_id
     */
    private List<Long> evictBusiPerCaches(Integer ownerType, Long ownerId) {
        if (ownerType == null || ownerId == null) {
            return Collections.emptyList();
        }
        if (ownerType == 0) {
            CacheUtils.evictUserPermissionCaches(ownerId);
            return List.of(ownerId);
        } else if (ownerType == 1) {
            return evictRoleCaches(ownerId);
        }
        return Collections.emptyList();
    }

    private List<Long> evictRoleCaches(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        CacheUtils.keyRemove(CacheConstant.RoleCacheConstant.ROLE_MENU_PERS_CACHE, String.valueOf(roleId));
        CacheUtils.keyRemove(CacheConstant.RoleCacheConstant.ROLE_BUSI_PERS_CACHE, String.valueOf(roleId));
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
            new QueryWrapper<SysUserRole>().eq("role_id", roleId)
        );
        List<Long> userIds = new ArrayList<>();
        if (userRoles != null) {
            for (SysUserRole ur : userRoles) {
                if (ur == null || ur.getUserId() == null) {
                    continue;
                }
                CacheUtils.evictUserPermissionCaches(ur.getUserId());
                userIds.add(ur.getUserId());
            }
        }
        return userIds;
    }
}
