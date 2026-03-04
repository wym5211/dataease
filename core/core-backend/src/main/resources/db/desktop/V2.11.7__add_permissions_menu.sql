-- 添加权限管理菜单数据（桌面版H2）
-- 为管理员提供完整的权限管理功能

-- 清理可能存在的旧数据（避免主键冲突）
DELETE FROM core_menu WHERE id IN (100, 101, 102, 103, 104, 105, 106, 107);

-- 插入权限管理主菜单及子菜单
-- id从100开始，避免与现有菜单冲突
INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES
-- 权限管理主菜单
(100, 0, 1, 'permissions', NULL, 10, 'icon_permissions', '/permissions', 0, 1, 0),
-- 权限中心（默认页）
(101, 100, 2, 'permissions-menu', 'permissions/menu', 1, NULL, 'menu', 0, 1, 1),
-- 用户管理
(102, 100, 2, 'permissions-user', 'permissions/user', 2, NULL, 'user', 0, 1, 1),
-- 角色管理
(103, 100, 2, 'permissions-role', 'permissions/role', 3, NULL, 'role', 0, 1, 1),
-- 菜单授权
(104, 100, 2, 'permissions-menu-auth', 'permissions/menu', 4, NULL, 'menu-auth', 0, 1, 1),
-- 资源授权
(105, 100, 2, 'permissions-resource', 'permissions/resource', 5, NULL, 'resource', 0, 1, 1),
-- 权限模板
(106, 100, 2, 'permissions-templates', 'permissions/template', 6, NULL, 'templates', 0, 1, 1),
-- 权限审计
(107, 100, 2, 'permissions-audit', 'permissions/audit', 7, NULL, 'audit', 0, 1, 1);

-- 为管理员角色(角色ID=1)分配所有权限菜单
MERGE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 100), (1, 101), (1, 102), (1, 103), (1, 104), (1, 105), (1, 106), (1, 107);
