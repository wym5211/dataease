# DataEase 用户与权限管理系统建设方案与计划

## 1. 项目背景与目标

当前 DataEase 开源版（Core）在用户与权限管理方面采用的是“替补实现”（Substitute Implementation），即通过 `SubstituteUserServer`、`SubstituteOrgServer` 等类返回硬编码的单一管理员账户，缺乏实际的多用户、多组织及细粒度权限控制功能。

本项目旨在为 DataEase Core 模块增加完整的用户管理、组织管理、系统权限管理、资源权限管理及数据行列权限管理功能，替换现有的替补实现，使其具备企业级的权限管控能力。

## 2. 现状分析

*   **现有代码**：
    *   `core-backend` 中包含 `substitute` 包，提供模拟的 User、Org、Auth 接口。
    *   数据库中缺乏用户（User）、角色（Role）、组织（Org）及权限（Permission）相关的表结构。
    *   依赖 `java-jwt` 进行简单的 Token 解析（如果有），但缺乏完整的认证授权流程。
*   **需求**：
    *   **用户管理**：用户的增删改查、状态管理、密码重置等。
    *   **组织管理**：多层级组织架构管理。
    *   **系统权限**：基于角色的访问控制（RBAC），控制菜单和功能按钮的访问。
    *   **资源权限**：针对仪表板、数据集、数据源等资源的可见性与操作权限（读/写/授权）。
    *   **数据权限**：
        *   **行权限**：基于用户属性或角色的数据行级过滤（Row Level Security）。
        *   **列权限**：基于角色的数据列级脱敏或隐藏。

## 3. 总体设计方案

### 3.1 数据库设计 (Database Schema)

我们将新增以下核心表结构（前缀 `sys_` 或 `core_`，保持风格一致，建议使用 `sys_` 以区分系统级表）：

1.  **`sys_user` (用户表)**
    *   字段：`id`, `username`, `password` (加密), `email`, `phone`, `status` (启用/禁用), `create_time`, `update_time`.
2.  **`sys_org` (组织表)**
    *   字段：`id`, `name`, `pid` (父组织ID), `description`, `sort`, `create_time`.
3.  **`sys_role` (角色表)**
    *   字段：`id`, `name`, `code` (标识), `type` (系统角色/组织角色/自定义), `description`.
4.  **`sys_user_role` (用户角色关联表)**
    *   字段：`user_id`, `role_id`.
5.  **`sys_menu` (菜单/功能表 - 扩展现有 core_menu 或新建)**
    *   现有 `core_menu` 可复用，需增加 `permission` 标识字段。
6.  **`sys_role_menu` (角色菜单关联表)**
    *   字段：`role_id`, `menu_id`.
7.  **`sys_resource_permission` (资源权限表)**
    *   字段：`id`, `resource_type` (dashboard/dataset/datasource), `resource_id`, `owner_id` (用户/角色/组织), `owner_type`, `permission` (read/write/share).
8.  **`sys_row_permission` (行权限表)**
    *   字段：`id`, `dataset_id`, `role_id` (或 user_id), `filter_expression` (过滤条件/SQL片段), `enable`.
9.  **`sys_column_permission` (列权限表)**
    *   字段：`id`, `dataset_id`, `role_id`, `column_name`, `permission_type` (hide/mask).

### 3.2 后端架构设计

1.  **认证模块 (Authentication)**
    *   实现基于 JWT 的登录认证接口 `/api/auth/login`。
    *   替换 `SubstituleAuthServer`，实现真正的 Token 生成与校验。
    *   使用 Spring Security 或自定义拦截器（Interceptor）进行请求鉴权。

2.  **用户与组织模块**
    *   实现 `UserServer` 替换 `SubstituteUserServer`。
    *   实现 `OrgServer` 替换 `SubstituleOrgServer`。
    *   提供完整的 RESTful API。

3.  **权限控制模块 (Authorization)**
    *   **功能权限**：基于 AOP 或拦截器，校验当前用户角色是否拥有请求接口的权限（对应 `sys_role_menu`）。
    *   **资源权限**：在业务 Service 层（如 `ChartService`, `DashboardService`）通过 `PermissionProxy` 或注解校验资源 ID 的访问权限。
    *   **数据权限**：
        *   **行权限**：在 SQL 生成阶段（`DatasetProvider` 或 Mybatis 拦截器）注入 `WHERE` 子句。
        *   **列权限**：在查询结果返回前，对敏感列进行过滤或脱敏处理。

## 4. 实施计划 (Implementation Plan)

### 第一阶段：基础架构与数据库 (Estimated: 2 Days)
1.  设计并编写 SQL 迁移脚本 (`db/migration/V2.11.0__auth_ddl.sql`)。
2.  使用 MyBatis Plus Generator 生成 Entity、Mapper、Service 基础代码。
3.  配置 Spring Security (可选) 或 JWT Filter 基础框架。

### 第二阶段：用户与组织管理 (Estimated: 3 Days)
1.  实现 `UserServer` 的 CRUD 逻辑及密码加密存储。
2.  实现 `OrgServer` 的树形结构管理逻辑。
3.  实现登录接口 `LoginController`，生成真实 JWT。
4.  替换 `core-backend` 中的替补 Bean (`@ConditionalOnMissingBean` 将失效)。

### 第三阶段：系统与资源权限 (Estimated: 4 Days)
1.  实现角色管理 (`RoleServer`) 与菜单分配。
2.  实现权限拦截器，基于 URL 或注解进行功能鉴权。
3.  设计资源授权接口（将仪表板分享给用户/角色）。
4.  在 `core_menu` 及业务接口中接入权限校验。

### 第四阶段：数据行列权限 (Estimated: 4 Days)
1.  **行权限**：研究 DataEase 的 SQL 生成逻辑（`Provider` 层），实现基于当前用户角色的 SQL 拼接注入。
2.  **列权限**：在数据查询返回结果集（`DataSetResult`）时，根据权限配置过滤字段。

### 第五阶段：联调与测试 (Estimated: 2 Days)
1.  编写单元测试覆盖核心权限逻辑。
2.  进行 API 接口测试。
3.  验证与前端页面的交互（需确认前端是否已有对应管理页面，若无由于本次主要涉及后端，前端部分可能需另行规划或使用简易页面验证）。

## 5. 交付物
1.  SQL DDL 脚本。
2.  后端 Java 源码（Controller, Service, Dao, Entity）。
3.  接口文档（Swagger/OpenAPI）。
