-- 修复用户密码，使用BCrypt加密格式
-- BCrypt hash for "DataEase@123456": $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi

-- 更新admin用户密码为BCrypt格式
UPDATE sys_user SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi' WHERE username = 'admin';

-- 更新999用户密码为BCrypt格式
UPDATE sys_user SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi' WHERE username = '999';

-- 如果用户不存在则插入
INSERT INTO sys_user (id, username, password, nick_name, email, dept_id, status, create_time, update_time)
SELECT 1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin@dataease.io', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');

INSERT INTO sys_user (id, username, password, nick_name, email, dept_id, status, create_time, update_time)
SELECT 2, '999', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', '999@test.com', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = '999');
