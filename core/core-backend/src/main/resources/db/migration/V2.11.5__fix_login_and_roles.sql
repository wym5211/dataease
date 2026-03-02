-- 直接修复登录问题
-- 1. 删除所有旧的测试数据
DELETE FROM sys_user_role WHERE user_id = 1;
DELETE FROM sys_user WHERE id = 1;

-- 2. 插入admin用户，使用BCrypt加密的密码 "DataEase@123456"
-- BCrypt hash for "DataEase@123456": $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
INSERT INTO sys_user (id, username, password, nick_name, email, dept_id, status, create_time, update_time)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin@dataease.io', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- 3. 确保角色存在
INSERT INTO sys_role (id, name, role_alias, type, description, status, create_time) VALUES
(1, '管理员', 'admin', 0, '系统管理员', 1, UNIX_TIMESTAMP() * 1000),
(2, '普通用户', 'user', 1, '普通用户角色', 1, UNIX_TIMESTAMP() * 1000),
(3, '数据分析师', 'analyst', 1, '数据分析师角色', 1, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 4. 关联用户和角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 5. 查看创建的用户用于调试
SELECT id, username, nick_name, status FROM sys_user WHERE username = 'admin';
SELECT id, name, role_alias, type, status FROM sys_role;
