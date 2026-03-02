# 角色管理数据库优化

**日期:** 2026-03-01
**状态:** ✅ 已完成

---

## 问题分析

### 发现的问题
1. **sys_role 表缺少 status 字段**
   - 前端需要显示角色的启用/禁用状态
   - 后端返回的 status 为 `NaN`
   - 导致前端无法正确显示状态

2. **RoleVO 缺少必要字段**
   - code（角色编码）
   - description（描述）
   - status（状态）
   - createTime（创建时间）

3. **toRoleVO 方法映射不完整**
   - 只映射了 id、name、readonly、root
   - 缺少其他重要字段

---

## 解决方案

### 1. 数据库迁移

**文件:** `core/core-backend/src/main/resources/db/migration/V2.11.1__role_status_field.sql`

```sql
-- 添加 status 字段到 sys_role 表
ALTER TABLE `sys_role`
ADD COLUMN `status` INT(11) DEFAULT 1 COMMENT '状态 0:禁用 1:启用' AFTER `description`;

-- 更新现有数据，默认设置为启用状态
UPDATE `sys_role` SET `status` = 1 WHERE `status` IS NULL;
```

### 2. 后端实体类更新

#### SysRole.java
```java
/**
 * 状态 0:禁用 1:启用
 */
private Integer status;
```

#### RoleVO.java
```java
@Schema(description = "角色编码")
private String code;

@Schema(description = "描述")
private String description;

@Schema(description = "状态 0:禁用 1:启用")
private Integer status;

@Schema(description = "创建时间")
private Long createTime;
```

#### RoleCreator.java
```java
@Schema(description = "状态 0:禁用 1:启用")
private Integer status;
```

#### RoleEditor.java
```java
@Schema(description = "状态 0:禁用 1:启用")
private Integer status;
```

### 3. 后端服务更新

#### CoreRoleServer.java - create()
```java
@Override
@Transactional
public Long create(RoleCreator creator) {
    SysRole role = new SysRole();
    role.setName(creator.getName());
    role.setRoleAlias(creator.getName());
    role.setType(creator.getTypeCode());
    role.setDescription(creator.getDesc());
    role.setStatus(creator.getStatus() != null ? creator.getStatus() : 1); // 默认启用
    role.setCreateTime(System.currentTimeMillis());
    sysRoleMapper.insert(role);
    return role.getId();
}
```

#### CoreRoleServer.java - edit()
```java
@Override
public void edit(RoleEditor editor) {
    SysRole role = sysRoleMapper.selectById(editor.getId());
    if (role == null) {
        DEException.throwException("角色不存在");
    }
    if (role.getType() != null && role.getType() == 0 && !AuthUtils.isSysAdmin()) {
        DEException.throwException("系统角色不可编辑");
    }
    role.setName(editor.getName());
    role.setRoleAlias(editor.getName());
    role.setDescription(editor.getDesc());
    if (editor.getStatus() != null) {
        role.setStatus(editor.getStatus());
    }
    sysRoleMapper.updateById(role);
}
```

#### CoreRoleServer.java - toRoleVO()
```java
private RoleVO toRoleVO(SysRole role) {
    RoleVO vo = new RoleVO();
    vo.setId(role.getId());
    vo.setName(role.getName());
    vo.setCode(role.getRoleAlias()); // 角色编码使用 role_alias
    vo.setDescription(role.getDescription());
    vo.setCreateTime(role.getCreateTime());
    vo.setStatus(role.getStatus() != null ? role.getStatus() : 1); // 使用数据库中的 status
    vo.setReadonly(role.getType() != null && role.getType() == 0);
    vo.setRoot(role.getId() != null && role.getId() == 1L);
    return vo;
}
```

#### CoreRoleServer.java - detail()
```java
@Override
public RoleDetailVO detail(Long rid) {
    SysRole role = sysRoleMapper.selectById(rid);
    if (role == null) {
        return null;
    }
    RoleDetailVO vo = new RoleDetailVO();
    vo.setId(role.getId());
    vo.setName(role.getName());
    vo.setTypeCode(role.getType());
    vo.setDesc(role.getDescription());
    vo.setStatus(role.getStatus() != null ? role.getStatus() : 1);
    vo.setRid(role.getId());
    return vo;
}
```

### 4. 前端更新

#### api.ts
移除了临时的 NaN 处理代码，因为后端现在返回正确的数据。

```typescript
// 获取角色列表
export const getRoleList = async (params: RoleListRequest): Promise<RoleListResponse> => {
  const response: any = await request.post({
    url: `${BASE_URL}/byCurOrg`,
    data: { keyword: params.keyword || '' }
  })
  const data = response.data || response
  const records = Array.isArray(data) ? data : data.records || []

  return {
    records,
    total: records.length
  }
}
```

---

## 测试步骤

### 1. 编译后端

```bash
cd core/core-backend
mvn clean package -DskipTests
```

### 2. 重启后端服务

```bash
java -jar target/CoreApplication.jar --spring.profiles.active=standalone
```

### 3. 验证数据库迁移

连接数据库，检查 sys_role 表结构：

```sql
DESCRIBE sys_role;
```

应该看到 status 字段：
```
Field       Type         Null    Default
-------     ----------- -------  -------
id          bigint(20)   NO
name        varchar(50)  NO
role_alias  varchar(50)  YES
type        int(11)      YES      1
description varchar(255) YES
status      int(11)      YES      1  ← 新增字段
create_time bigint(13)   YES
```

### 4. 前端功能测试

1. 访问角色管理页面：`http://localhost:8081/#/permissions/role`
2. 检查角色列表的状态列是否正确显示
3. 点击"编辑"按钮，检查状态单选框是否正确选中
4. 修改状态并保存，验证是否成功更新

---

## 影响范围

### 数据库
- 新增字段：`sys_role.status`
- 默认值：1（启用）
- 现有数据：自动更新为 1

### 后端
- SDK 模块：RoleVO、RoleCreator、RoleEditor
- Core 模块：SysRole、CoreRoleServer

### 前端
- 简化了 API 适配器的数据处理逻辑
- RoleDialog 组件正常工作

---

## 回滚方案

如果需要回滚：

```sql
-- 移除 status 字段
ALTER TABLE `sys_role` DROP COLUMN `status`;
```

然后恢复之前的代码版本。

---

**最后更新:** 2026-03-01
