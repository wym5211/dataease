-- 确保sys_user表中有admin和999用户
-- 使用INSERT IGNORE避免重复插入错误

-- 直接插入admin用户（如果不存在）
INSERT IGNORE INTO sys_user (id, username, password, nick_name, email, dept_id, status, create_time, update_time)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin@dataease.io', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- 直接插入999用户（如果不存在）
INSERT IGNORE INTO sys_user (id, username, password, nick_name, email, dept_id, status, create_time, update_time)
VALUES (2, '999', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', '999@test.com', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- 确保角色存在
INSERT IGNORE INTO sys_role (id, name, role_alias, type, description, status, create_time) VALUES
(1, '管理员', 'admin', 0, '系统管理员', 1, UNIX_TIMESTAMP() * 1000);
INSERT IGNORE INTO sys_role (id, name, role_alias, type, description, status, create_time) VALUES
(2, '普通用户', 'user', 1, '普通用户', 1, UNIX_TIMESTAMP() * 1000);

-- 确保用户角色关联存在
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (2, 2);
