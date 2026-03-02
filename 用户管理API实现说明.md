# 用户管理后端API实现说明

## 实现概述
已成功实现完整的用户管理后端API，路径为 `/user`，符合前端API设计要求。

## 已实现的接口

### 1. 用户列表查询
- **接口**: `POST /user/pager/{goPage}/{pageSize}`
- **功能**: 分页查询用户列表，支持搜索和筛选
- **支持的条件**:
  - 关键词搜索（用户名、昵称、邮箱）
  - 状态筛选（启用/禁用）
  - 角色筛选
  - 时间排序
- **返回**: `IPage<UserGridVO>` - 包含角色信息的用户列表

### 2. 创建用户
- **接口**: `POST /user/create`
- **功能**: 创建新用户并关联角色
- **验证规则**:
  - 用户名唯一性验证
  - 邮箱唯一性验证
  - 默认密码：`DataEase@123456`（BCrypt加密）
- **自动处理**:
  - 设置当前用户所属组织
  - 创建用户角色关联

### 3. 编辑用户
- **接口**: `POST /user/edit`
- **功能**: 更新用户信息和角色关联
- **验证规则**:
  - 邮箱唯一性验证（排除自己）
- **更新内容**:
  - 基本信息（昵称、邮箱、电话、状态）
  - 角色关联（先删除旧关联，再添加新关联）

### 4. 删除用户
- **接口**: `POST /user/delete/{id}`
- **功能**: 删除指定用户
- **安全验证**:
  - 不能删除admin用户（ID=1）
  - 不能删除当前登录用户
- **级联操作**: 自动删除用户角色关联

### 5. 批量删除用户
- **接口**: `POST /user/batchDel`
- **功能**: 批量删除用户
- **安全验证**:
  - 不能删除admin用户
  - 不能删除当前登录用户
- **级联操作**: 自动删除用户角色关联

### 6. 重置密码
- **接口**: `POST /user/resetPwd/{id}`
- **功能**: 重置用户密码为默认密码
- **默认密码**: `DataEase@123456`（BCrypt加密）

### 7. 切换用户状态
- **接口**: `POST /user/enable`
- **功能**: 启用/禁用用户
- **安全验证**:
  - 不能禁用admin用户
  - 不能禁用当前登录用户

### 8. 角色可选用户
- **接口**: `POST /user/role/option`
- **功能**: 获取指定角色未绑定的用户列表
- **支持**: 关键词搜索

### 9. 角色已选用户
- **接口**: `POST /user/role/selected/{goPage}/{pageSize}`
- **功能**: 分页获取指定角色已绑定的用户列表
- **支持**: 关键词搜索

## 技术实现细节

### 依赖注入
```java
@Autowired
private SysUserMapper sysUserMapper;

@Autowired
private SysUserRoleMapper sysUserRoleMapper;

@Autowired
private SysRoleMapper sysRoleMapper;
```

### 密码加密
使用 `BCryptPasswordEncoder` 进行密码加密：
```java
private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
```

### 事务管理
关键操作使用 `@Transactional` 保证数据一致性：
- 创建用户
- 编辑用户
- 删除用户
- 批量删除

### 辅助方法
1. **toUserGridVO()**: 将用户实体转换为列表VO，包含角色信息
2. **toUserItemVO()**: 将用户实体转换为选项VO
3. **getUserRoles()**: 获取用户的角色列表
4. **getSelectedUserIds()**: 获取角色已绑定的用户ID集合
5. **getUserIdsByRoleIds()**: 根据角色ID列表获取用户ID列表

## 安全特性

1. **权限控制**: 通过 `@DePermit` 注解进行权限验证
2. **唯一性验证**: 用户名和邮箱唯一性检查
3. **操作限制**: 
   - 不能删除/禁用admin用户
   - 不能删除/禁用当前登录用户
4. **密码加密**: 使用BCrypt加密算法
5. **事务一致性**: 使用Spring事务管理

## 数据库表结构

### sys_user (用户表)
- id: 主键
- username: 用户名（唯一）
- password: 密码（BCrypt加密）
- nick_name: 昵称
- email: 邮箱
- phone: 电话
- dept_id: 部门ID
- status: 状态（1:启用 0:禁用）
- create_time: 创建时间
- update_time: 更新时间

### sys_user_role (用户角色关联表)
- id: 主键
- user_id: 用户ID
- role_id: 角色ID
- 唯一索引: (user_id, role_id)

### sys_role (角色表)
- id: 主键
- name: 角色名称
- role_alias: 角色别名
- type: 类型（0:系统 1:自定义）
- description: 描述
- create_time: 创建时间

## 测试建议

1. **单元测试**: 测试各个方法的业务逻辑
2. **集成测试**: 测试API端到端功能
3. **安全测试**: 验证权限控制和数据验证
4. **性能测试**: 测试大数据量下的分页查询性能

## 后续优化建议

1. **缓存优化**: 对角色信息进行缓存
2. **审计日志**: 记录用户管理操作日志
3. **批量导入**: 实现Excel批量导入用户
4. **导出功能**: 实现用户列表导出
5. **密码策略**: 支持自定义密码策略
6. **MFA支持**: 完善多因素认证功能

## 文件位置

- **实现文件**: `core/core-backend/src/main/java/io/dataease/core/permissions/user/CoreUserServer.java`
- **API接口**: `sdk/api/api-permissions/src/main/java/io/dataease/api/permissions/user/api/UserApi.java`
- **DTO定义**: `sdk/api/api-permissions/src/main/java/io/dataease/api/permissions/user/dto/`
- **VO定义**: `sdk/api/api-permissions/src/main/java/io/dataease/api/permissions/user/vo/`
