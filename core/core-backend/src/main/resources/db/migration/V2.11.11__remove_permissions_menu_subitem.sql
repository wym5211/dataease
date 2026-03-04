-- 删除重复的权限中心子菜单
-- 权限中心页面已经包含菜单授权功能，不需要单独的子菜单

-- 删除角色菜单关联
DELETE FROM sys_role_menu WHERE menu_id = 101;

-- 删除菜单记录
DELETE FROM core_menu WHERE id = 101;

