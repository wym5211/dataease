# DataEase 安全分析报告

**日期**: 2026-04-02
**范围**: 认证与授权、注入攻击防护
**方法**: 风险驱动的分层分析

---

## 概述

对 DataEase 项目前后端进行了安全审计，发现 **12 项安全问题**，其中 **8 项严重级别为 CRITICAL**。核心风险集中在：JWT 密钥硬编码、Link Token 无签名验证、存储型 XSS、无 CSRF 防护。

---

## 一、认证与授权

### CRITICAL-1: JWT 密钥硬编码

**位置**:
- `core/core-backend/src/main/java/io/dataease/commons/utils/JwtUtils.java:13`
- `sdk/common/src/main/java/io/dataease/utils/TokenUtils.java:13`

**问题**: 密钥 `"DataEase_Secret_Key_2024"` 写死在源码中，无法通过配置或环境变量覆盖。

**代码**:
```java
private static final String SECRET = "DataEase_Secret_Key_2024";
```

**影响**: 任何获取源码的人可伪造任意用户的 JWT 令牌，完全绕过认证。

**修复方案**:
1. 从 `application.yml` 或环境变量读取密钥
2. 启动时检查密钥是否为默认值，若是则拒绝启动（生产环境）
3. 生成随机密钥并持久化到数据库

---

### CRITICAL-2: Link Token 无签名验证

**位置**:
- `sdk/common/src/main/java/io/dataease/utils/TokenUtils.java:47-62`
- `core/core-backend/src/main/java/io/dataease/share/util/LinkTokenUtil.java:12`

**问题**:
1. `validateLinkToken()` 仅调用 `JWT.decode()` 解码，**不验证 HMAC 签名**
2. 默认密码 `"link-pwd-fit2cloud"` 硬编码在源码中

**代码**:
```java
// TokenUtils.java:55 — 仅解码，无签名验证
DecodedJWT jwt = JWT.decode(linkToken);
Long userId = jwt.getClaim("uid").asLong();

// LinkTokenUtil.java:12 — 硬编码默认密码
public static final String defaultPwd = "link-pwd-fit2cloud";
```

**影响**: 可伪造分享链接令牌，访问任何共享资源。

**修复方案**:
1. `validateLinkToken()` 添加签名验证（与 `DeLinkAop` 逻辑一致）
2. 默认密码改为启动时随机生成

---

### CRITICAL-3: 桌面模式完全绕过认证

**位置**: `sdk/common/src/main/java/io/dataease/auth/filter/TokenFilter.java:60-66`

**问题**: `spring.profiles.active=desktop` 时，所有请求直接设置默认管理员用户，跳过全部认证。

**代码**:
```java
if (isDesktop) {
    UserUtils.setDesktopUser();
    filterChain.doFilter(servletRequest, servletResponse);
    return;
}
```

**影响**: 配置错误（误设为 desktop）即暴露整个系统为管理员权限。

**修复方案**:
1. Desktop 模式至少验证请求来源（localhost 检查）
2. 添加日志告警：桌面模式启动时打印警告

---

### CRITICAL-4: TokenUtils 不验证令牌过期

**位置**: `sdk/common/src/main/java/io/dataease/utils/TokenUtils.java:29-44`

**问题**: `JwtUtils` 有 24 小时过期机制（`withExpiresAt`），但 `TokenUtils.validate()` 不检查过期时间。

**代码**:
```java
// TokenUtils.java — 只验证签名，不检查过期
JWT.require(algorithm).build().verify(token);
return userBOByToken(token);
```

**影响**: 一旦令牌泄露，永不过期，攻击者可永久使用。

**修复方案**: 在 `validate()` 和 `validateLinkToken()` 中添加过期时间检查。

---

### CRITICAL-5: 明文密码比对 + 无暴力破解防护

**位置**: `core/core-backend/src/main/java/io/dataease/core/permissions/login/CoreLoginServer.java:60-69`

**问题**:
1. 非 BCrypt 密码回退到明文比对 `pwd.equals(user.getPassword())`
2. 无登录失败次数限制、无账户锁定、无验证码

**代码**:
```java
if (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$")) {
    matches = passwordEncoder.matches(pwd, user.getPassword());
} else {
    if (pwd.equals(user.getPassword())) { // 明文比对
        matches = true;
        user.setPassword(passwordEncoder.encode(pwd));
    }
}
```

**修复方案**:
1. 移除明文比对逻辑，启动时强制迁移所有明文密码为 BCrypt
2. 添加登录失败计数 + 账户锁定（5 次失败锁定 15 分钟）
3. 添加可选验证码

---

### MEDIUM-1: 白名单暴露 Swagger 文档

**位置**: `sdk/common/src/main/java/io/dataease/utils/WhitelistUtils.java:32-34`

**问题**: `/swagger-resources` 和 `/doc.html` 在白名单中，对外公开。

**修复方案**: 生产环境默认关闭，通过配置项控制。

---

### MEDIUM-2: CORS 默认允许所有来源

**位置**: `sdk/common/src/main/java/io/dataease/auth/interceptor/CorsConfig.java:42-44`

**问题**: `corsStrict=false` 时 `allowedOrigins("*")`。

**修复方案**: 生产环境强制 `corsStrict=true`。

---

### MEDIUM-3: RSA 私钥存储在数据库

**位置**: `sdk/common/src/main/java/io/dataease/utils/RsaUtils.java:167-170`

**问题**: 数据库被入侵则私钥泄露。

**修复方案**: 考虑使用密钥管理服务或文件系统存储。

---

## 二、注入攻击防护

### CRITICAL-6: 存储型 XSS（4 处）

**位置**:
| 文件 | 行号 | 代码 |
|------|------|------|
| `custom-component/v-text/Component.vue` | 130, 136 | `v-html="element['propValue']"` |
| `custom-component/scroll-text/Component.vue` | 187, 191 | `v-html="element['propValue']"` |
| `views/chart/components/views/index.vue` | 1169 | `v-html="state.title_remark.remark"` |
| `custom-component/rich-text/DeRichTextView.vue` | 420 | `innerHTML` 直接赋值 |

**问题**: 用户输入的 HTML 内容直接通过 `v-html` 渲染，无任何过滤。全项目仅登录页使用了 `xss` 库（`login/index.vue:210`）。

**影响**: 存储型 XSS 可窃取 Token、劫持会话、执行任意 JS。

**修复方案**:
1. 引入 DOMPurify 库
2. 在所有 `v-html` 绑定处添加 `DOMPurify.sanitize()` 过滤
3. 配置 tinymce 编辑器的 XSS 过滤规则

---

### CRITICAL-7: 无 CSRF 防护

**位置**: 全局（前后端均未实现）

**问题**:
- 无 CSRF Token 生成和验证
- 无 SameSite Cookie 属性
- 无 `X-Frame-Options` / `Content-Security-Policy` 头

**影响**: 恶意网站可代替已登录用户发起操作。

**修复方案**:
1. 后端生成 CSRF Token，前端在每个修改请求中携带
2. 配置 Spring Security CSRF 防护
3. 添加安全响应头（CSP, X-Frame-Options, X-Content-Type-Options）

---

### CRITICAL-8: SpEL 表达式注入

**位置**: `core/core-backend/src/main/java/io/dataease/system/interceptor/DePermitAop.java:196`

**问题**: `expressionParser.parseExpression(expr)` 执行权限注解中的 SpEL 表达式，无沙箱、无白名单。

**代码**:
```java
Expression e = expressionParser.parseExpression(expr);
Object val = e.getValue(ctx);
```

**影响**: 方法参数直接传入 SpEL 上下文，可能绕过授权或访问敏感数据。

**修复方案**:
1. 限制 SpEL 表达式仅允许 `#p0`、`.id`、`:read` 等安全模式
2. 或使用 `SimpleEvaluationContext` 替代 `StandardEvaluationContext`

---

### HIGH-1: SQL 查询未完全参数化

**位置**: `core/core-backend/src/main/java/io/dataease/datasource/provider/CalciteProvider.java:336`

**问题**: `statement.executeQuery(datasourceRequest.getQuery())` 直接执行拼接的 SQL。

**代码**:
```java
// 表名直接拼接
resultSet = statement.executeQuery("select * from " + String.format(" `%s`", table) + " limit 0 offset 0 ");
```

**缓解因素**: SQL 经过 Calcite 解析器验证，且大部分查询使用 PreparedStatement。

**修复方案**: 审计所有 `executeQuery` 调用，确保用户可控输入使用参数化查询。

---

## 三、修复路线图

| 阶段 | 修复项 | 工作量 | 影响 |
|------|--------|--------|------|
| **P0 立即** | JWT 密钥外部化 + Link Token 签名验证 | 中 | 根除令牌伪造 |
| **P0 立即** | 移除明文密码比对 + 增加登录限速 | 小 | 阻断暴力破解 |
| **P1 一周内** | v-html 添加 DOMPurify 过滤 | 小 | 消除存储型 XSS |
| **P1 一周内** | 添加 CSRF Token 机制 | 中 | 防止跨站请求 |
| **P1 一周内** | 关闭 Swagger 公开访问 | 极小 | 减少攻击面 |
| **P2 一个月** | TokenUtils 添加过期验证 | 小 | 限制令牌生命周期 |
| **P2 一个月** | SpEL 表达式白名单 | 中 | 防止表达式注入 |
| **P3 长期** | SQL 查询全面参数化审计 | 大 | 彻底消除 SQL 注入 |

---

## 四、安全优势

项目也有一些做得好的地方：
1. BCrypt 密码哈希存储（主流程）
2. RSA 加密密码传输
3. Apache Calcite SQL 解析器防止大部分 SQL 注入
4. AOP 权限拦截器框架设计合理
5. URL 路径遍历防护（`./`, `%2e`, `;`）
6. 过滤器链有序且正确清理 ThreadLocal
