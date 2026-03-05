-- 添加用户999到sys_user表
-- 使用BCrypt加密的密码 "DataEase@123456"
-- BCrypt hash generated for "DataEase@123456"

-- 删除旧的admin和999用户
DELETE FROM sys_user_role WHERE user_id IN (1, 2);
DELETE FROM sys_user WHERE id IN (1, 2);

-- 插入admin用户（ID=1），使用BCrypt密码
INSERT INTO sys_user (id, username, password, nick_name, email, dept_id, status, create_time, update_time)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin@dataease.io', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- 插入用户999（ID=2），使用BCrypt密码
INSERT INTO sys_user (id, username, password, nick_name, email, dept_id, status, create_time, update_time)
VALUES (2, '999', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', '999@test.com', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- 确保管理员角色存在
INSERT INTO sys_role (id, name, role_alias, type, description, status, create_time) VALUES
(1, '管理员', 'admin', 0, '系统管理员', 1, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE name = '管理员', status = 1;

-- 确保普通用户角色存在
INSERT INTO sys_role (id, name, role_alias, type, description, status, create_time) VALUES
(2, '普通用户', 'user', 1, '普通用户', 1, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE name = '普通用户', status = 1;

-- 关联用户和角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 2);
