-- 添加资源备份菜单（桌面版H2）
-- 作为独立的顶级菜单（不是sys-setting的子菜单，因为sys-setting被排除）

-- 清理可能存在的旧数据（避免主键冲突）
DELETE FROM core_menu WHERE id = 65;

-- 插入资源备份菜单作为独立顶级菜单
-- id=65, pid=0(顶级菜单), type=2, name='backup', 组件路径, 排序, 图标, 路径
INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES
(65, 0, 2, 'backup', 'system/parameter/backup', 20, 'icon_backup', '/backup', 0, 1, 1);

-- 为管理员角色分配菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 65);

-- 验证插入结果
SELECT id, pid, name, path, component FROM core_menu WHERE id = 65;
