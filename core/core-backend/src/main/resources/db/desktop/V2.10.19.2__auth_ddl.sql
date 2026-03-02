-- ----------------------------
-- Desktop模式认证相关表
-- 这些表对于CoreAuthInitializer启动初始化是必需的
-- ----------------------------

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `username`    varchar(50)  NOT NULL COMMENT '用户名',
    `password`    varchar(100) NOT NULL COMMENT '密码',
    `nick_name`   varchar(50) DEFAULT NULL COMMENT '昵称',
    `email`       varchar(100) DEFAULT NULL COMMENT '邮箱',
    `phone`       varchar(20) DEFAULT NULL COMMENT '手机号',
    `dept_id`     bigint(20) DEFAULT NULL COMMENT '部门ID',
    `status`      tinyint(1) DEFAULT '1' COMMENT '状态 1:启用 0:禁用',
    `create_time` bigint(13) DEFAULT NULL COMMENT '创建时间',
    `update_time` bigint(13) DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) COMMENT='系统用户表';

DROP TABLE IF EXISTS `sys_org`;
CREATE TABLE `sys_org`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `pid`         bigint(20) DEFAULT '0' COMMENT '父ID',
    `name`        varchar(50) NOT NULL COMMENT '组织名称',
    `description` varchar(255) DEFAULT NULL COMMENT '描述',
    `sort`        int(11) DEFAULT '0' COMMENT '排序',
    `create_time` bigint(13) DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) COMMENT='组织表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `name`        varchar(50) NOT NULL COMMENT '角色名称',
    `role_alias`  varchar(50) DEFAULT NULL COMMENT '角色别名',
    `type`        int(11) DEFAULT '1' COMMENT '类型 0:系统 1:自定义',
    `description` varchar(255) DEFAULT NULL COMMENT '描述',
    `status`      int(11) DEFAULT '1' COMMENT '状态 0:禁用 1:启用',
    `create_time` bigint(13) DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) COMMENT='角色表';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`
(
    `id`      bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `role_id` bigint(20) NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) COMMENT='用户角色关联表';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`
(
    `id`      bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role_id` bigint(20) NOT NULL COMMENT '角色ID',
    `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) COMMENT='角色菜单关联表';
