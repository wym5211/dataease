CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `nick_name` varchar(50) DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态 1:启用 0:禁用',
  `create_time` bigint(13) DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint(13) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE `sys_org` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `pid` bigint(20) DEFAULT '0' COMMENT '父ID',
  `name` varchar(50) NOT NULL COMMENT '组织名称',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `create_time` bigint(13) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织表';

CREATE TABLE `sys_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_alias` varchar(50) DEFAULT NULL COMMENT '角色别名',
  `type` int(11) DEFAULT '1' COMMENT '类型 0:系统 1:自定义',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` bigint(13) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE `sys_user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE `sys_role_menu` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

CREATE TABLE `sys_resource_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `resource_id` varchar(64) NOT NULL COMMENT '资源ID',
  `resource_type` varchar(20) NOT NULL COMMENT '资源类型(dashboard/dataset/datasource)',
  `owner_id` bigint(20) NOT NULL COMMENT '拥有者ID(user_id/role_id/dept_id)',
  `owner_type` int(11) NOT NULL COMMENT '拥有者类型(0:user 1:role 2:dept)',
  `permission` int(11) NOT NULL COMMENT '权限(1:read 2:write 4:share)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源权限表';

CREATE TABLE `sys_row_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `dataset_id` varchar(64) NOT NULL COMMENT '数据集ID',
  `auth_target_id` bigint(20) NOT NULL COMMENT '授权目标ID(role_id/user_id)',
  `auth_target_type` int(11) NOT NULL COMMENT '授权目标类型(0:user 1:role)',
  `filter_expression` text COMMENT '过滤条件表达式',
  `enable` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行权限表';

CREATE TABLE `sys_column_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `dataset_id` varchar(64) NOT NULL COMMENT '数据集ID',
  `auth_target_id` bigint(20) NOT NULL COMMENT '授权目标ID',
  `auth_target_type` int(11) NOT NULL COMMENT '授权目标类型',
  `column_name` varchar(100) NOT NULL COMMENT '列名',
  `permission_type` int(11) NOT NULL COMMENT '权限类型(0:隐藏 1:脱敏)',
  `mask_rule` varchar(255) DEFAULT NULL COMMENT '脱敏规则',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='列权限表';
