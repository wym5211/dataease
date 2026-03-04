# 权限错误 70001 诊断报告

**日期**: 2026-03-03
**问题**: 用户 999 使用正确凭据登录后，访问 /user/create 接口返回错误码 70001
**错误信息**: "没有权限访问该接口"

---

## 权限系统分析

### API 权限要求
**文件**: `sdk/api/api-permissions/src/main/java/io/dataease/api/permissions/user/api/UserApi.java`
```java
@DePermit("m:read")
@PostMapping("/create")
Long create(@RequestBody UserCreator creator);
```

权限要求: `m:read` (菜单读取权限)

### 权限验证逻辑
**文件**: `core/core-backend/src/main/java/io/dataease/system/interceptor/DePermitAop.java`

**验证流程**:
1. `@DePermit("m:read")` 被拦截
2. 调用 `checkMenuPermit(user, AuthResourceEnum.USER)` (Line 114-127)
3. 查询 `sys_role_menu` 表，验证用户的角色是否有 USER 菜单的访问权限

```java
private boolean checkMenuPermit(TokenUserBO user, AuthResourceEnum rt) {
    if (rt == null) {
        return false;
    }
    long menuId = rt.getMenuId();  // USER 菜单的 ID
    Set<Long> roleIds = getRoleIds(user.getUserId());  // 用户的角色列表
    if (roleIds.isEmpty()) {
        return false;
    }
    QueryWrapper<SysRoleMenu> qw = new QueryWrapper<>();
    qw.eq("menu_id", menuId);
    qw.in("role_id", roleIds);
    return sysRoleMenuMapper.selectCount(qw) > 0;  // 必须找到至少一条记录
}
```

---

## 数据库表结构

### 1. sys_user (用户表)
- user_id: 用户ID
- username: 账号
- password: 密码

### 2. sys_role (角色表)
- role_id: 角色ID
- name: 角色名称

### 3. sys_user_role (用户-角色关联表)
- user_id: 用户ID
- role_id: 角色ID

### 4. sys_role_menu (角色-菜单关联表) ⭐ 关键表
- role_id: 角色ID
- menu_id: 菜单ID

### 5. sys_resource_permission (资源权限表)
- resource_type: 资源类型
- resource_id: 资源ID
- permission_id: 权限ID

---

## 诊断 SQL 查询

### 查询 1: 检查用户 999 的基本信息
```sql
SELECT user_id, username, name FROM sys_user WHERE username = '999';
```

### 查询 2: 检查用户 999 的角色
```sql
SELECT ur.role_id, r.name AS role_name
FROM sys_user_role ur
JOIN sys_role r ON ur.role_id = r.role_id
WHERE ur.user_id = (SELECT user_id FROM sys_user WHERE username = '999');
```

### 查询 3: 检查这些角色是否有 USER 菜单权限 ⭐ 关键
```sql
SELECT rm.role_id, r.name AS role_name, rm.menu_id
FROM sys_role_menu rm
JOIN sys_role r ON rm.role_id = r.role_id
WHERE rm.role_id IN (
    SELECT role_id FROM sys_user_role
    WHERE user_id = (SELECT user_id FROM sys_user WHERE username = '999')
)
AND rm.menu_id = (SELECT menu_id FROM sys_resource_permission WHERE resource_type = 'USER' LIMIT 1);
```

### 查询 4: 查找 USER 资源的 menu_id
```sql
SELECT * FROM sys_resource_permission WHERE resource_type = 'USER';
```

### 查询 5: 完整诊断（一次性检查所有）
```sql
-- 用户 999 的 ID
SELECT @user_id := user_id FROM sys_user WHERE username = '999';

-- 用户 999 的角色
SELECT @role_ids := GROUP_CONCAT(role_id) FROM sys_user_role WHERE user_id = @user_id;

-- USER 菜单的 ID
SELECT @menu_id := menu_id FROM sys_resource_permission WHERE resource_type = 'USER' LIMIT 1;

-- 检查角色是否有菜单权限
SELECT * FROM sys_role_menu WHERE role_id IN (@role_ids) AND menu_id = @menu_id;
```

---

## 可能的问题原因

### 原因 1: 角色未关联到 USER 菜单 ⭐ 最可能
**现象**: 用户 999 有角色，但该角色在 `sys_role_menu` 表中没有 USER 菜单的记录

**解决方法**:
```sql
-- 假设 USER 菜单的 menu_id 是 1 (需要通过查询4确认)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role_id, 1 FROM sys_user_role WHERE user_id = @user_id;
```

### 原因 2: sys_resource_permission 表中没有 USER 资源记录
**现象**: AuthResourceEnum.USER.getMenuId() 返回 0 或找不到对应的菜单

**解决方法**: 检查菜单初始化数据

### 原因 3: 角色配置不完整
**现象**: 用户 999 的角色本身配置不完整

**解决方法**: 检查并重新分配角色

---

## 修复建议

1. **首先执行诊断查询**，确定缺少哪个环节的配置
2. **根据查询结果**，添加缺失的 `sys_role_menu` 记录
3. **验证修复**: 重新登录用户 999 并尝试创建用户

---

## AuthResourceEnum 定义参考

**文件**: `sdk/common/src/main/java/io/dataease/permissions/enums/AuthResourceEnum.java`

```java
USER("user", 1L, "用户管理"),
// ... 其他资源
```

预期的 menu_id 应该是 1L 或其他预定义值。

---

**状态**: 📋 待执行数据库诊断查询
