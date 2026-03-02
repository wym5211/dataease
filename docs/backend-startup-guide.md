# 后端编译和启动指南

**日期:** 2026-03-01
**状态:** SDK已编译安装，core-backend需要IDEA启动

---

## 当前状态

### ✅ 已完成
1. **SDK模块编译并安装**
   - `api-permissions` 模块已成功编译
   - 已安装到本地Maven仓库 `C:\Users\wym\.m2\repository\io\dataease\api-permissions\2.10.19\`
   - 包含我们的修改：RoleVO、RoleCreator、RoleEditor

2. **数据库迁移脚本**
   - `V2.11.1__role_status_field.sql` 已创建
   - 将在后端启动时由Flyway自动执行

3. **后端代码修改**
   - `SysRole.java` - 已添加status字段
   - `CoreRoleServer.java` - 已更新方法

### ⚠️ 需要处理
1. **core-backend完整编译错误**
   - 错误在 `MybatisInterceptor.java`（与我们的修改无关）
   - 这是项目已存在的问题

---

## 启动方式（推荐）

### 方式1: 使用IntelliJ IDEA（推荐）

1. **在IDEA中打开项目**
   ```
   File → Open → E:\cursor\dataease
   ```

2. **配置SDK**
   - File → Project Structure → Project
   - SDK: 选择 `E:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot`
   - Language Level: 21

3. **找到主类**
   - 导航到 `core/core-backend/src/main/java/io/dataease/CoreApplication.java`

4. **运行**
   - 右键点击 `CoreApplication.java`
   - 选择 Run 'CoreApplication'
   - 或点击类名旁的绿色运行按钮

5. **配置运行参数**
   - Run → Edit Configurations
   - Active profiles: `standalone`
   - VM options: `-Dspring.profiles.active=standalone`

### 方式2: 使用Maven（如果IDEA方式不可用）

由于完整编译有错误，可以尝试跳过测试：

```bash
cd E:\cursor\dataease

# 设置Java 21
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

# 只编译不打包
"C:\Program Files\JetBrains\IntelliJ IDEA 2023.2.1\plugins\maven\lib\maven3\bin\mvn" -pl core/core-backend compile
```

然后在IDEA中运行主类。

---

## 数据库迁移验证

后端启动时，Flyway会自动执行迁移脚本。验证方法：

```sql
-- 连接到数据库
mysql -u root -p dataease10

-- 检查表结构
DESCRIBE sys_role;

-- 应该看到status字段：
-- status     int(11)      YES     1
```

或者查看Flyway版本历史：

```sql
SELECT * FROM flyway_schema_history WHERE version = '2.11.1';
```

---

## 测试后端API

启动后，测试角色管理API：

```bash
# 获取角色列表
curl -X POST http://localhost:8100/de2api/role/byCurOrg \
  -H "Content-Type: application/json" \
  -d '{"keyword":""}'
```

期望返回：
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
      "createTime": 1234567890000,
      "readonly": true,
      "root": true
    }
  ],
  "msg": "success"
}
```

---

## 常见问题

### Q: 编译失败提示找不到符号
A: 确保使用Java 21，并先执行SDK模块的install：
```bash
mvn -f sdk/api/api-permissions/pom.xml clean install -DskipTests
```

### Q: 后端启动失败，提示连接数据库错误
A: 检查 `application-standalone.yml` 中的数据库配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dataease10
    username: root
    password: your_password
```

### Q: Flyway迁移失败
A: 检查数据库是否存在：
```sql
CREATE DATABASE IF NOT EXISTS dataease10 CHARACTER SET utf8mb4;
```

---

## 文件修改清单

**已修改（需要重启后端生效）：**
1. `sdk/api/api-permissions/src/main/java/io/dataease/api/permissions/role/vo/RoleVO.java`
2. `sdk/api/api-permissions/src/main/java/io/dataease/api/permissions/role/dto/RoleCreator.java`
3. `sdk/api/api-permissions/src/main/java/io/dataease/api/permissions/role/dto/RoleEditor.java`
4. `core/core-backend/src/main/java/io/dataease/system/dao/auto/entity/SysRole.java`
5. `core/core-backend/src/main/java/io/dataease/core/permissions/role/CoreRoleServer.java`
6. `core/core-backend/src/main/resources/db/migration/V2.11.1__role_status_field.sql`（新建）

**前端修改（已生效）：**
- `core/core-frontend/src/views/permissions/role/api.ts`

---

**下一步:** 在IDEA中运行CoreApplication，然后刷新前端页面测试功能。
