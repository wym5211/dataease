-- 初始化管理员用户
-- 密码: DataEase@123456 (BCrypt加密)

INSERT INTO sys_user (id, username, password, nick_name, email, phone, dept_id, status, create_time, update_time)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin@dataease.io', '13800138000', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE
  password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
  nick_name = '管理员',
  status = 1;

-- 初始化默认组织
INSERT INTO sys_org (id, pid, name, description, sort, create_time)
VALUES (1, 0, 'Default Organization', '默认组织', 1, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE name = 'Default Organization';

-- 初始化管理员角色
INSERT INTO sys_role (id, name, role_alias, type, description, status, create_time)
VALUES (1, '管理员', 'admin', 0, '系统管理员角色', 1, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE name = '管理员';

-- 关联用户和角色
INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1)
ON DUPLICATE KEY UPDATE user_id = user_id;

-- 插入一些测试角色
INSERT INTO sys_role (id, name, role_alias, type, description, status, create_time) VALUES
(2, '普通用户', 'user', 1, '普通用户角色', 1, UNIX_TIMESTAMP() * 1000),
(3, '数据分析师', 'analyst', 1, '数据分析师角色', 1, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE name = VALUES(name);
