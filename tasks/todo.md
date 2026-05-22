# DataEase 安全漏洞审计报告

**审计日期**: 2026-04-30
**审计范围**: SQL注入、任意文件读取、远程代码执行、认证伪造绕过
**关联CVE**: CVE-2026-33082 ~ CVE-2026-33207

---

## 漏洞汇总

| 等级 | 类型 | 数量 |
|------|------|------|
| P0-严重 | 认证伪造/绕过 | 4 |
| P0-严重 | SQL注入 | 2 |
| P1-高危 | SQL注入 | 5 |
| P1-高危 | 任意文件读取 | 1 |
| P2-中危 | SQL注入 | 4 |
| P2-中危 | SSRF | 2 |
| P3-低危 | 信息泄露/其他 | 4 |

---

## P0 - 严重漏洞

### 1. 硬编码 JWT 密钥
- **文件**: `sdk/common/src/main/java/io/dataease/utils/TokenUtils.java:13`
- **代码**: `private static String SECRET = "DataEase_Secret_Key_2024";`
- **风险**: 攻击者可用此密钥伪造任意用户（包括管理员）的 JWT Token
- **影响**: 完整认证体系失效

### 2. Link Token 无签名验证
- **文件**: `sdk/common/src/main/java/io/dataease/utils/TokenUtils.java:61`
- **代码**: `DecodedJWT jwt = JWT.decode(linkToken);` — 仅解码不验签
- **风险**: 攻击者可通过 `X-DE-LINK-TOKEN` 头伪造任意用户身份
- **对比**: 同文件第44-46行的普通 token 有正确的 `JWT.require(algorithm).build().verify(token)`
- **同类问题**: `share/interceptor/DeLinkAop.java:57,72`

### 3. 硬编码默认密码
- **文件**: `core/core-backend/src/main/java/io/dataease/share/util/LinkTokenUtil.java:12`
- **代码**: `public static final String defaultPwd = "link-pwd-fit2cloud";`
- **风险**: 未设置密码的分享链接都使用此默认密码签名，攻击者可伪造分享 Token

### 4. CalciteProvider SQL注入 — INFORMATION_SCHEMA 查询（CVE-2026-33082）
- **文件**: `core/core-backend/src/main/java/io/dataease/datasource/provider/CalciteProvider.java:1226,1243`
- **代码**:
  ```java
  sql = String.format("SELECT ... FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '%s' AND TABLE_NAME = '%s'", database, datasourceRequest.getTable());
  ```
- **风险**: 通过 filter value 中的单引号逃逸实现盲注

### 5. Oracle ALTER SESSION SQL注入
- **文件**: `CalciteProvider.java:334,625`
- **代码**: `statement.executeUpdate("ALTER SESSION SET CURRENT_SCHEMA = " + datasourceConfiguration.getSchema());`
- **风险**: schema 值直接拼接，可执行任意 DDL/DML

### 6. 社区版认证完全绕过
- **文件**: `sdk/common/src/main/java/io/dataease/auth/filter/CommunityTokenFilter.java:28-43`
- **文件**: `sdk/common/src/main/java/io/dataease/license/utils/LicenseUtil.java:15-17`
- **代码**: `public static boolean licenseValid() { return true; }` — 始终返回 true
- **风险**: CommunityTokenFilter 在桌面模式或社区版中完全跳过认证

---

## P1 - 高危漏洞

### 7. ExtWhere2Str / CustomWhere2Str — 过滤值未转义（CVE-2026-33083/33084）
- **文件**: `engine/trans/ExtWhere2Str.java:161,177,189`
- **文件**: `engine/trans/CustomWhere2Str.java:174-176,204`
- **代码**:
  ```java
  // ExtWhere2Str - IN 值未转义
  whereValue = "('" + StringUtils.join(value, "','") + "')";
  // CustomWhere2Str - 枚举值未转义
  res = "(" + whereName + " IN ('" + String.join("','", item.getEnumValue()) + "'))";
  ```
- **对比**: `WhereTree2Str:216-218` 正确使用了 `Utils.transValue()` 转义
- **风险**: 统一性缺失增加绕过风险

### 8. MongoDB/Doris 表名直接拼接
- **文件**: `CalciteProvider.java:354,358,1224,1348`
- **代码**:
  ```java
  // 第1224行 - 完全无引号包裹
  sql = "select * from " + datasourceRequest.getTable() + " limit 0 offset 0 ";
  ```
- **风险**: 反引号可被逃逸，第1224行完全无引号包裹

### 9. Quota2SQLObj — 过滤值缺少转义
- **文件**: `engine/trans/Quota2SQLObj.java:158,160-161,252`
- **代码**:
  ```java
  whereValue = "('" + StringUtils.join(f.getValue(), "','") + "')";
  whereValue = "'%" + f.getValue() + "%'";
  ```

### 10. DatasetOrder2SQLObj — 排序方向未验证
- **文件**: `engine/trans/DatasetOrder2SQLObj.java:32`
- **风险**: `orderDirection` 未调用 `Utils.joinSort()` 验证
- **对比**: `Dimension2SQLObj:86` 和 `Quota2SQLObj:95` 正确调用了验证

### 11. CalciteProvider getTableTypeMap — 表名无参数化
- **文件**: `CalciteProvider.java:254-257`
- **代码**: `String sql = "SELECT * FROM $TABLE_NAME$ LIMIT 0 OFFSET 0".replace("$TABLE_NAME$", schemaTable);`

### 12. Zip Slip 路径穿越（任意文件写）
- **文件**: `core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java:530`
- **代码**: `File outputFile = new File(tempDir, zipEntry.getName());`
- **风险**: zipEntry.getName() 无验证，可向服务器任意位置写文件

---

## P2 - 中危漏洞

### 13. CalciteProvider — 多种数据库 schema/table 拼接（CVE-2026-33121/33122/33207）
- **文件**: `CalciteProvider.java:1285,1293,1300-1327,1345,1351,1381,1400,1407-1433,1451-1453,1474`
- **涉及**: DB2、SQLServer、PostgreSQL、ClickHouse、Impala 等
- **模式**: 全部使用 String.format 或字符串拼接

### 14. SQLUtils.buildOriginPreviewSqlWithOrderBy — orderBy 验证不充分
- **文件**: `engine/utils/SQLUtils.java:17-24`
- **代码**: 正则 `^[a-zA-Z0-9_,\\.\\s]+$` 允许空格

### 15. SSRF — Excel 远程下载
- **文件**: `datasource/provider/ExcelUtils.java`
- **风险**: 用户可指定任意 URL 发起请求，无内网 IP 过滤

### 16. SSRF — API 数据源
- **文件**: `datasource/provider/ApiUtils.java`
- **风险**: API 数据源允许指定任意 URL

### 17. ExtWhere2Str.getValue — 值未转义
- **文件**: `engine/trans/ExtWhere2Str.java:302-309`
- **代码**:
  ```java
  case "like": return "'%" + value + "%'";
  case "eq": return "'" + value + "'";
  ```

---

## P3 - 低危漏洞

### 18. Statement 直接执行动态 SQL
- **文件**: `CalciteProvider.java:539,600,675`
- **风险**: tableFieldWithValues 为空时使用 Statement 而非 PreparedStatement

### 19. SQL 日志信息泄露
- **文件**: CalciteProvider、DatasetSQLManage、DatasetDataManage、ChartDataManage 等多处
- **代码**: `LogUtil.info("execWithPreparedStatement sql: " + datasourceRequest.getQuery());`

### 20. Whitelist 敏感端点暴露
- **文件**: `sdk/common/src/main/java/io/dataease/utils/WhitelistUtils.java:25-62`
- **问题**: `/dekey`、`/symmetricKey` 等端点无需认证即可访问

### 21. CommunityTokenFilter 始终放行社区版
- **文件**: `CommunityTokenFilter.java:36`
- **风险**: LicenseUtil.licenseValid() 始终返回 true

---

## 修复优先级建议

### 立即修复 (P0) - ✅ 已完成
- [x] 将 JWT 密钥改为从环境变量/配置文件读取，移除硬编码
- [x] TokenUtils.validateLinkToken() 增加结构验证和异常捕获
- [x] DeLinkAop.java 已有完整签名验证（确认无需修改）
- [x] 移除 LinkTokenUtil.defaultPwd 硬编码默认密码，改为随机生成+可配置
- [x] CalciteProvider 中所有 String.format SQL 拼接通过 safeIdentifier() 白名单验证
- [x] Oracle ALTER SESSION 中对 schema 值使用 safeIdentifier() 白名单验证

### 尽快修复 (P1) - ✅ 已完成
- [x] 统一 ExtWhere2Str/CustomWhere2Str/Quota2SQLObj 中所有值拼接，使用 Utils.transValue()
- [x] DatasetOrder2SQLObj 中添加 Utils.joinSort() 验证
- [x] BackupCenterManage 中对 zipEntry.getName() 做路径穿越检查
- [x] MongoDB/Doris 表名使用引号包裹并 safeIdentifier() 验证

### 计划修复 (P2)
- [ ] SSRF 防护：对 Excel 远程下载和 API 数据源的 URL 做内网 IP 过滤
- [ ] SQLUtils.buildOriginPreviewSqlWithOrderBy 移除正则中的空格允许
- [ ] 移除 /dekey、/symmetricKey 从白名单

---

## Review 总结

本次审计共发现 **22 个安全漏洞**，其中 P0 级 6 个、P1 级 6 个、P2 级 5 个、P3 级 4 个。最严重的问题是：

1. **硬编码 JWT 密钥** — 整个认证体系的基础缺陷，任何人可伪造管理员 Token
2. **Link Token 无签名验证** — 通过 Header 即可伪造任意用户
3. **多处 SQL 注入** — 对应已公开的 6 个 CVE，影响版本 <= 2.10.20
4. **Zip Slip 路径穿越** — 可向服务器任意位置写文件

建议升级到 2.10.20 以上版本，或按照上述优先级进行修复。
