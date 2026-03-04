-- 检查admin用户信息
SELECT '当前admin用户信息：' AS '';
SELECT user_id, username, password FROM sys_user WHERE username = 'admin';

-- 检查菜单数据
SELECT '当前菜单数据：' AS '';
SELECT id, name, path FROM core_menu ORDER BY id;

-- 如果需要重置admin密码为123456（BCrypt加密后的值）
-- 注意：这是BCrypt加密后的123456
-- UPDATE sys_user SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH' WHERE username = 'admin';

-- 检查角色菜单关系
SELECT 'Admin角色的菜单权限：' AS '';
SELECT role_id, menu_id FROM sys_role_menu WHERE role_id = 1;
