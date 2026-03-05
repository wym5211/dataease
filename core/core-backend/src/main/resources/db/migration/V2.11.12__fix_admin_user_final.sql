-- 最终修复管理员用户问题
-- 确保admin用户存在且密码正确

-- 1. 删除可能存在的旧用户数据
DELETE FROM sys_user_role WHERE user_id = 1;
DELETE FROM sys_user WHERE id = 1;

-- 2. 插入admin用户，使用明文密码 "DataEase@123456"
-- CoreLoginServer会自动检测并转换为BCrypt格式
INSERT INTO sys_user (id, username, password, nick_name, email, dept_id, status, create_time, update_time)
VALUES (1, 'admin', 'DataEase@123456', '管理员', 'admin@dataease.io', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- 3. 确保角色存在
INSERT INTO sys_role (id, name, role_alias, type, description, status, create_time) VALUES
(1, '管理员', 'admin', 0, '系统管理员', 1, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE name = '管理员', status = 1;

-- 4. 关联用户和角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 5. 确保组织存在
INSERT INTO sys_org (id, pid, name, description, sort, create_time)
VALUES (1, 0, '默认组织', 'Default Organization', 1, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE name = '默认组织';
