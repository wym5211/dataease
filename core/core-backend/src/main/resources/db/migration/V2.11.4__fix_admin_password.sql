-- 修复管理员密码
-- 问题：BCrypt哈希值可能不匹配
-- 解决：删除并重新创建admin用户，使用简单的明文密码进行测试

-- 先删除可能存在的旧用户
DELETE FROM sys_user_role WHERE user_id = 1;
DELETE FROM sys_user WHERE username = 'admin';

-- 重新创建admin用户，使用明文密码测试（密码: admin123）
INSERT INTO sys_user (id, username, password, nick_name, email, dept_id, status, create_time, update_time)
VALUES (1, 'admin', 'admin123', '管理员', 'admin@dataease.io', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- 确保角色存在
INSERT INTO sys_role (id, name, role_alias, type, description, status, create_time)
VALUES (1, '管理员', 'admin', 0, '系统管理员', 1, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE name = '管理员';

-- 关联用户和角色
INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1);
