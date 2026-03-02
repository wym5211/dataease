# 后端启动快速指南（Lombok编译问题）

**日期:** 2026-03-01
**问题:** Maven编译时Lombok注解未生效

---

## 问题分析

项目中发现大量编译错误，都是因为实体类使用了Lombok的`@Data`注解，但getter/setter方法未生成：

```
找不到符号: 方法 getUsername()
找不到符号: 方法 getNickName()
找不到符号: 方法 getRoleAlias()
...等
```

这不是我们修改导致的问题，而是项目Lombok配置的问题。

---

## 推荐解决方案：使用IntelliJ IDEA启动

IDEA自带Lombok插件支持，可以直接运行。

### 步骤1: 在IDEA中打开项目

```
File → Open → 选择 E:\cursor\dataease
```

### 步骤2: 启用Lombok插件

1. `File → Settings → Plugins`
2. 搜索 "Lombok"
3. 安装 "Lombok" 插件（如果未安装）
4. 重启IDEA
5. `File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors`
6. 勾选 "Enable annotation processing"

### 步骤3: 配置JDK

1. `File → Project Structure → Project`
2. SDK: 选择 `E:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot`
3. Language Level: 21

### 步骤4: 运行CoreApplication

1. 导航到: `core/core-backend/src/main/java/io/dataease/CoreApplication.java`
2. 右键点击文件
3. 选择 `Run 'CoreApplication'`

### 步骤5: 配置运行参数

如果需要指定profile：

1. `Run → Edit Configurations`
2. 找到 `CoreApplication`
3. 在 `Active profiles` 中填写: `standalone`
4. 在 `VM options` 中填写: `-Dspring.profiles.active=standalone`

---

## 启动成功标志

当看到以下日志时，说明启动成功：

```
Started CoreApplication in XX seconds
Tomcat started on port(s): 8100 (http)
```

---

## 验证API

### 测试角色列表API

```bash
curl -X POST http://localhost:8100/de2api/role/byCurOrg \
  -H "Content-Type: application/json" \
  -d '{"keyword":""}'
```

期望返回包含status字段的JSON：
```json
{
  "code": 0,
  "data": [
    {
      "id": "1",
      "name": "管理员",
      "code": "管理员",
      "description": null,
      "status": 1,
      "createTime": 1234567890000
    }
  ]
}
```

---

## 数据库迁移验证

连接MySQL数据库：

```bash
mysql -u root -p
use dataease10;
DESCRIBE sys_role;
```

应该看到新增的status字段：
```
+-------------+--------------+------+-----+---------+-------+
| Field       | Type         | Null | Key | Default | Extra |
+-------------+--------------+------+-----+---------+-------+
| id          | bigint(20)   | NO   | PRI | NULL    |       |
| name        | varchar(50)  | NO   |     | NULL    |       |
| role_alias  | varchar(50)  | YES  |     | NULL    |       |
| type        | int(11)      | YES  |     | 1       |       |
| description | varchar(255) | YES  |     | NULL    |       |
| status      | int(11)      | YES  |     | 1       |       | ← 新增
| create_time | bigint(13)   | YES  |     | NULL    |       |
+-------------+--------------+------+-----+---------+-------+
```

检查Flyway迁移历史：
```sql
SELECT * FROM flyway_schema_history WHERE version = '2.11.1';
```

---

## 已完成的修改

### SDK模块（已编译安装）
- ✅ `api-permissions-2.10.19.jar` 已安装到本地Maven仓库
- ✅ 包含RoleVO、RoleCreator、RoleEditor的status字段

### 数据库
- ✅ `V2.11.1__role_status_field.sql` 迁移脚本已创建

### 后端代码
- ✅ `SysRole.java` - 已添加status字段
- ✅ `CoreRoleServer.java` - 已更新create、edit、toRoleVO、detail方法
- ✅ `MybatisInterceptorConfig.java` - 已手动添加getter/setter

---

## 前端测试

后端启动成功后：

1. 访问: `http://localhost:8081/#/permissions/role`
2. 点击"编辑"按钮
3. 状态应该正确显示为"启用"或"禁用"
4. 修改状态并保存
5. 检查列表是否更新

---

## 备选方案

如果IDEA也无法启动，可以考虑：

1. 使用Docker部署（如果有docker-compose配置）
2. 联系项目维护者修复Lombok配置
3. 手动为所有实体类添加getter/setter（工作量大）

---

**下一步:** 请在IntelliJ IDEA中启动后端，然后我们可以测试完整的角色管理功能。
