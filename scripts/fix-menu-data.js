// 修复菜单数据脚本
// 使用方法：在后端项目中创建一个临时的REST接口执行此SQL

const SQL = `
-- 修复缺失的菜单数据
DELETE FROM core_menu WHERE id IN (1, 2, 3, 4, 5, 6);

INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES
(1, 0, 2, 'workbranch', 'workbranch', 1, NULL, '/workbranch', 0, 1, 1),
(2, 0, 2, 'panel', 'visualized/view/panel', 2, NULL, '/panel', 0, 1, 1),
(3, 0, 2, 'screen', 'visualized/view/screen', 3, NULL, '/screen', 0, 1, 1),
(4, 0, 1, 'data', NULL, 4, NULL, '/data', 0, 1, 0),
(5, 4, 2, 'dataset', 'visualized/data/dataset', 1, NULL, '/dataset', 0, 1, 1),
(6, 4, 2, 'datasource', 'visualized/data/datasource', 2, NULL, '/datasource', 0, 1, 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6);
`;

console.log('请手动执行以下SQL来修复菜单数据：');
console.log(SQL);
console.log('\n执行位置：E:\\cursor\\dataease\\core\\core-backend\\src\\main\\resources\\db\\migration\\V2.11.6__fix_menu_data.sql');
