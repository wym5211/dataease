-- 使用明文密码修复管理员用户
-- CoreLoginServer会自动检测并转换为BCrypt格式

-- 删除旧用户数据
DELETE FROM sys_user_role WHERE user_id = 1;
DELETE FROM sys_user WHERE id = 1;

-- 插入admin用户，使用明文密码 "DataEase@123456"
INSERT INTO sys_user (id, username, password, nick_name, email, dept_id, status, create_time, update_time)
VALUES (1, 'admin', 'DataEase@123456', '管理员', 'admin@dataease.io', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- 确保角色存在
INSERT INTO sys_role (id, name, role_alias, type, description, status, create_time) VALUES
(1, '管理员', 'admin', 0, '系统管理员', 1, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE name = '管理员', status = 1;

-- 关联用户和角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
