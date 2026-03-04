-- 清理重复的权限管理菜单（修复版）
-- 问题：存在两个权限管理菜单（一个name为中文'权限管理'，一个name为'permissions'）
-- 解决：删除name为中文的权限管理菜单，保留ID 100-107的英文name菜单

-- 首先查找重复的中文权限管理菜单ID（非100-107ID范围的）
SET @duplicate_menu_id := (SELECT id FROM core_menu WHERE name = '权限管理' AND path = '/permissions' AND id NOT IN (100, 101, 102, 103, 104, 105, 106, 107) LIMIT 1);

-- 删除重复菜单的角色关联
DELETE FROM sys_role_menu WHERE menu_id = @duplicate_menu_id;

-- 删除重复菜单的子菜单的角色关联（使用派生表避免子查询引用同一表的问题）
DELETE FROM sys_role_menu WHERE menu_id IN (SELECT id FROM (SELECT id FROM core_menu WHERE pid = @duplicate_menu_id) AS temp);

-- 删除重复菜单的子菜单记录
DELETE FROM core_menu WHERE pid = @duplicate_menu_id;

-- 删除重复的主菜单记录
DELETE FROM core_menu WHERE id = @duplicate_menu_id;

-- 同时检查是否有其他可能的重复权限菜单（比如ID 70左右的）
-- 删除ID在70-99之间且与权限管理相关的菜单（如果存在）
DELETE FROM sys_role_menu WHERE menu_id IN (70, 71, 72, 73, 74, 75, 76, 77, 78, 79);
DELETE FROM core_menu WHERE id IN (70, 71, 72, 73, 74, 75, 76, 77, 78, 79);

-- 确保权限管理菜单的name统一为permissions（不是中文）
UPDATE core_menu SET name = 'permissions' WHERE id = 100 AND name != 'permissions';
UPDATE core_menu SET name = 'permissions-menu' WHERE id = 101;
UPDATE core_menu SET name = 'permissions-user' WHERE id = 102;
UPDATE core_menu SET name = 'permissions-role' WHERE id = 103;
UPDATE core_menu SET name = 'permissions-menu-auth' WHERE id = 104;
UPDATE core_menu SET name = 'permissions-resource' WHERE id = 105;
UPDATE core_menu SET name = 'permissions-templates' WHERE id = 106;
UPDATE core_menu SET name = 'permissions-audit' WHERE id = 107;

