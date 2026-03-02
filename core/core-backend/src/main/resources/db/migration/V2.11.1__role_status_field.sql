-- 为 sys_role 表添加 status 字段
-- 用于控制角色的启用/禁用状态

ALTER TABLE `sys_role`
ADD COLUMN `status` INT(11) DEFAULT 1 COMMENT '状态 0:禁用 1:启用' AFTER `description`;

-- 更新现有数据，默认设置为启用状态
UPDATE `sys_role` SET `status` = 1 WHERE `status` IS NULL;
