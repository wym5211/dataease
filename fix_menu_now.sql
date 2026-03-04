-- 修复admin用户的菜单数据，确保能访问所有功能

-- 1. 删除可能冲突的旧数据
DELETE FROM core_menu WHERE id IN (1, 2, 3, 4, 5, 6);

-- 2. 插入完整的基础菜单
INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES
(1, 0, 2, 'workbranch', 'workbranch', 1, NULL, '/workbranch', 0, 1, 1),
(2, 0, 2, 'panel', 'visualized/view/panel', 2, NULL, '/panel', 0, 1, 1),
(3, 0, 2, 'screen', 'visualized/view/screen', 3, NULL, '/screen', 0, 1, 1),
(4, 0, 1, 'data', NULL, 4, NULL, '/data', 0, 1, 0),
(5, 4, 2, 'dataset', 'visualized/data/dataset', 1, NULL, '/dataset', 0, 1, 1),
(6, 4, 2, 'datasource', 'visualized/data/datasource', 2, NULL, '/datasource', 0, 1, 1);

-- 3. 为admin用户角色（ID=1）分配所有菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6);

-- 4. 验证结果
SELECT '修复完成，当前菜单列表：' AS '';
SELECT id, name, path FROM core_menu ORDER BY id;
