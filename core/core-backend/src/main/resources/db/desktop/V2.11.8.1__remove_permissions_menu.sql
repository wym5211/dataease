-- 清理桌面版权限管理菜单（desktop模式不需要权限管理）
DELETE FROM core_menu WHERE id IN (100, 101, 102, 103, 104, 105, 106, 107);
DELETE FROM sys_role_menu WHERE menu_id IN (100, 101, 102, 103, 104, 105, 106, 107);
