# 后台SQL执行错误分析与解决方案

## 📊 错误摘要

### 发现的错误

| 错误类型 | 错误代码 | 发生位置 | 严重程度 |
|---------|---------|---------|----------|
| Oracle SQL错误 | ORA-00918 | DatasetDataManage.previewSqlWithLog | 🔴 高 |
| 表名错误 | 无效的表名 | DatasetDataManage.previewSqlWithLog | 🔴 高 |

---

## 🔍 详细错误分析

### 错误1: ORA-00918 (column specifier invalid)

**错误日志：**
```
2026-02-27 15:04:16.773 ERROR --- [nio-8100-exec-3] GlobalExceptionHandler
SQL ERROR: ORA-00918: ¿¿¿¿¿¿
```

**调用栈：**
```
DatasetDataServer.previewSql()
  └─> DatasetDataManage.previewSqlWithLog()
       └─> DatasetDataManage.previewSql()
            └─> SQLProvider.createQuerySQL()
                 └─> provider.rebuildSQL()
                      └─> provider.fetchResultField()
                           └─> CalciteProvider.execute SQL
```

**问题分析：**

1. **ORA-00918含义**
   - Oracle官方：`column specifier invalid`
   - 通常原因：SQL中列引用语法错误

2. **可能的根本原因**
   - ✗ Calcite生成的SQL与Oracle语法不兼容
   - ✗ 列名包含特殊字符（如下划线、大写）未正确处理
   - ✗ Oracle驱动版本（19c）与数据库版本不兼容
   - ✗ 引用标识符问题（引号使用不当）

3. **字符集问题**
   - 错误消息显示为乱码（¿¿¿¿¿¿）
   - 可能存在字符集转换问题

---

### 错误2: 无效的表名

**错误日志：**
```
2026-02-27 15:04:10.454 ERROR --- [io-8100-exec-10] GlobalExceptionHandler
无效的表名！
```

**可能原因：**
- Oracle对表名大小写敏感
- 表名需要使用引号或正确转义
- Schema配置不正确

---

## 💡 解决方案

### 方案1：启用详细SQL调试（已配置）✅

已在 `application-dev.yml` 中添加：
```yaml
logging:
  level:
    io.dataease.datasource: DEBUG
    io.dataease.dataset: DEBUG
    org.apache.calcite: DEBUG
```

**重启后端后，日志将显示：**
- 实际执行的SQL语句
- Calcite生成的查询计划
- 列名和表名的转换过程

### 方案2：检查Oracle数据源配置

**请确认以下配置：**

1. **JDBC URL格式**
   ```java
   // Service Name方式（推荐）
   jdbc:oracle:thin:@hostname:1521/service_name

   // SID方式
   jdbc:oracle:thin:@hostname:1521:sid
   ```

2. **字符集配置**
   ```yaml
   # 在Oracle数据源配置中
   charset: UTF-8
   targetCharset: UTF-8
   ```

3. **Schema配置**
   - 确认Schema名称大小写正确
   - Oracle默认大写，需要使用双引号引用小写对象

### 方案3：修改Oracle驱动版本（如需要）

**当前版本：** `ojdbc10-19.19.0.0.jar` (Oracle 19c)

**如果使用旧版本Oracle数据库：**

| Oracle版本 | 推荐驱动 | 下载地址 |
|------------|----------|----------|
| 11g | ojdbc6.jar | https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html |
| 12c | ojdbc8.jar | https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html |
| 19c | ojdbc10.jar | 已包含 |

**替换步骤：**
1. 备份当前驱动：`drivers/ojdbc10-19.19.0.0.jar`
2. 下载对应版本的驱动
3. 替换文件：`drivers/ojdbc10-19.19.0.0.jar`
4. 重启后端服务

### 方案4：检查SQL语法兼容性

**Oracle特殊语法注意事项：**

1. **双引号问题**
   ```sql
   -- Oracle推荐
   SELECT "column_name" FROM "table_name"

   -- 避免（除非列名包含特殊字符）
   SELECT column_name FROM table_name
   ```

2. **分页限制**
   ```sql
   -- Oracle 12c+
   SELECT * FROM table_name FETCH FIRST 10 ROWS ONLY

   -- Oracle 11g及以下
   SELECT * FROM (SELECT a.*, ROWNUM rn FROM table_name a WHERE ROWNUM <= 10)
   ```

3. **日期函数**
   ```sql
   -- Oracle
   TO_DATE('2024-01-01', 'YYYY-MM-DD')

   -- 避免使用
   DATE '2024-01-01'
   ```

---

## 🔧 调试步骤

### 步骤1：重启后端服务
```bash
# 停止后端
taskkill /F /IM java.exe

# 重新启动
cd E:\cursor\dataease
start-backend-dev.bat
```

### 步骤2：重现问题
1. 在前端创建Oracle数据源
2. 创建数据集
3. 点击"预览"按钮
4. 查看后端日志

### 步骤3：查看详细日志
```bash
# 实时查看日志
tail -f E:\cursor\dataease\core\core-backend\logs\backend.log

# 查看DEBUG级别的SQL日志
grep -E "DEBUG.*sql|DEBUG.*calcite|SELECT|FROM" E:\cursor\dataease\core\core-backend\logs\backend.log
```

### 步骤4：分析实际执行的SQL
在日志中查找：
```
calcite data preview sql: SELECT ...
```

这将显示Calcite生成的实际SQL，可以：
1. 复制SQL到Oracle客户端直接测试
2. 对比原SQL和生成SQL的差异
3. 确认问题所在

---

## 📝 常见问题与修复

### 问题1：列名包含下划线

**现象：** 列名如 `user_name` 导致ORA-00918

**修复：**
```java
// 在查询配置中
SELECT "USER_NAME" FROM table_name  // 使用大写和引号
```

### 问题2：表名大小写

**现象：** 创建表时用小写，查询时找不到

**修复：**
```sql
-- 创建时
CREATE TABLE "my_table" (...)

-- 查询时
SELECT * FROM "my_table"
```

### 问题3：Schema配置

**现象：** 提示无效的表名

**修复：**
```yaml
# 数据源配置中指定正确的Schema
schema: YOUR_SCHEMA_NAME  # 通常是大写
```

---

## 🎯 后续行动

### 立即行动
1. ✅ 已启用DEBUG日志级别
2. ⏳ 重启后端服务
3. ⏳ 重现问题并收集详细SQL日志

### 根据日志分析选择方案
- 如果是SQL语法问题 → 检查Calcite生成的SQL
- 如果是驱动版本问题 → 更换Oracle驱动
- 如果是配置问题 → 修正数据源配置

---

## 📞 获取帮助

如果问题持续存在，请提供以下信息：

1. **Oracle数据库版本**
   ```sql
   SELECT * FROM v$version;
   ```

2. **实际执行的SQL**（从backend.log中获取）

3. **数据源配置信息**

4. **完整的错误堆栈**

---

## 📚 参考资源

- [Oracle JDBC文档](https://docs.oracle.com/en/database/jdbc/)
- [Calcite SQL语法](https://calcite.apache.org/docs/reference.html)
- [DataEase官方文档](https://dataease.io/docs/)
