-- 修复缺失的菜单数据
-- 补充完整的基础菜单（工作台、仪表板、数据大屏、数据准备、数据集、数据源）

-- 清理可能存在的旧数据（避免主键冲突）
DELETE FROM core_menu WHERE id IN (1, 2, 3, 4, 5, 6);

-- 插入完整的基础菜单
INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES
(1, 0, 2, 'workbranch', 'workbranch', 1, NULL, '/workbranch', 0, 1, 1),
(2, 0, 2, 'panel', 'visualized/view/panel', 2, NULL, '/panel', 0, 1, 1),
(3, 0, 2, 'screen', 'visualized/view/screen', 3, NULL, '/screen', 0, 1, 1),
(4, 0, 1, 'data', NULL, 4, NULL, '/data', 0, 1, 0),
(5, 4, 2, 'dataset', 'visualized/data/dataset', 1, NULL, '/dataset', 0, 1, 1),
(6, 4, 2, 'datasource', 'visualized/data/datasource', 2, NULL, '/datasource', 0, 1, 1);

-- 为管理员角色分配所有菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6);

-- 验证插入结果
SELECT id, pid, name, path, component FROM core_menu ORDER BY id;
