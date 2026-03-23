-- 修复资源备份菜单权限：auth=1 表示需要权限，普通用户将无法看到
UPDATE core_menu SET auth = 1 WHERE id = 65;
