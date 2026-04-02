-- 删除权限模板菜单（id=106）
DELETE FROM core_menu WHERE id = 106;

-- 删除角色关联的权限模板菜单权限
DELETE FROM sys_role_menu WHERE menu_id = 106;
