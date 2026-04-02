# DataEase 安全修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复安全分析报告中的 12 项安全问题，按 P0 → P3 优先级分阶段实施。

**Architecture:** 后端修改集中在 `sdk/common` 和 `core/core-backend` 的认证/授权模块；前端修改集中在 `core/core-frontend` 的组件层，通过 DOMPurify 统一过滤 HTML 内容。每项修复独立、影响范围最小化。

**Tech Stack:** Java 21 / Spring Boot 3.3 / Vue 3 / DOMPurify / BCrypt / JWT (auth0)

**Spec:** `docs/superpowers/specs/2026-04-02-security-analysis-design.md`

---

## 文件变更映射

### 后端
| 文件 | 操作 | 职责 |
|------|------|------|
| `sdk/common/.../utils/TokenUtils.java` | 修改 | JWT 密钥外部化 + 过期验证 + Link Token 签名验证 |
| `core/core-backend/.../commons/utils/JwtUtils.java` | 修改 | JWT 密钥外部化 |
| `core/core-backend/.../core/permissions/login/CoreLoginServer.java` | 修改 | 移除明文比对 + 登录限速 |
| `core/core-backend/.../share/util/LinkTokenUtil.java` | 修改 | 默认密码外部化 |
| `core/core-backend/src/main/resources/application.yml` | 修改 | 新增 `dataease.jwt.secret` 配置项 |
| `core/core-backend/src/main/resources/application-standalone.yml` | 修改 | 新增 JWT 密钥配置 |
| `sdk/common/.../utils/WhitelistUtils.java` | 修改 | Swagger 端点条件化 |
| `sdk/common/.../auth/filter/TokenFilter.java` | 修改 | Desktop 模式 localhost 检查 |
| `core/core-backend/.../system/interceptor/DePermitAop.java` | 修改 | SpEL 安全化 |

### 前端
| 文件 | 操作 | 职责 |
|------|------|------|
| `core/core-frontend/package.json` | 修改 | 添加 dompurify 依赖 |
| `core/core-frontend/src/utils/sanitize.ts` | 新建 | DOMPurify 封装工具 |
| `core/core-frontend/src/custom-component/v-text/Component.vue` | 修改 | v-html 添加过滤 |
| `core/core-frontend/src/custom-component/scroll-text/Component.vue` | 修改 | v-html 添加过滤 |
| `core/core-frontend/src/views/chart/components/views/index.vue` | 修改 | v-html 添加过滤 |

---

## Phase 1: P0 — 立即修复（认证核心）

### Task 1: JWT 密钥外部化

**Files:**
- Modify: `sdk/common/src/main/java/io/dataease/utils/TokenUtils.java`
- Modify: `core/core-backend/src/main/java/io/dataease/commons/utils/JwtUtils.java`
- Modify: `core/core-backend/src/main/resources/application.yml`
- Modify: `core/core-backend/src/main/resources/application-standalone.yml`

- [ ] **Step 1: 在 application.yml 添加 JWT 密钥配置**

```yaml
# 在 dataease: 节点下添加
dataease:
  jwt:
    secret: DataEase_Secret_Key_2024  # 默认值，生产环境务必修改
```

- [ ] **Step 2: 创建配置读取工具方法**

在 `TokenUtils.java` 中，将硬编码密钥改为可配置：

```java
// 替换 private static final String SECRET = "DataEase_Secret_Key_2024";
private static String SECRET = "DataEase_Secret_Key_2024";

public static void setSecret(String secret) {
    if (StringUtils.isNotBlank(secret)) {
        SECRET = secret;
    }
}
```

- [ ] **Step 3: 创建 Spring 配置初始化类**

新建 `sdk/common/src/main/java/io/dataease/auth/config/JwtConfig.java`：

```java
package io.dataease.auth.config;

import io.dataease.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class JwtConfig {
    @Value("${dataease.jwt.secret:DataEase_Secret_Key_2024}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        TokenUtils.setSecret(jwtSecret);
    }
}
```

- [ ] **Step 4: 同步修改 JwtUtils.java**

```java
// 替换 private static final String SECRET = "DataEase_Secret_Key_2024";
private static String SECRET = "DataEase_Secret_Key_2024";

public static void setSecret(String secret) {
    if (secret != null && !secret.isEmpty()) {
        SECRET = secret;
    }
}
```

在 `JwtConfig.java` 中同时初始化：

```java
@PostConstruct
public void init() {
    TokenUtils.setSecret(jwtSecret);
    JwtUtils.setSecret(jwtSecret);
}
```

- [ ] **Step 5: 在 application-standalone.yml 覆盖默认密钥**

```yaml
dataease:
  jwt:
    secret: ${DE_JWT_SECRET:DataEase_Secret_Key_2024}
```

- [ ] **Step 6: 验证编译通过**

Run: `cd core/core-backend && mvn compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add sdk/common/src/main/java/io/dataease/utils/TokenUtils.java \
        core/core-backend/src/main/java/io/dataease/commons/utils/JwtUtils.java \
        sdk/common/src/main/java/io/dataease/auth/config/JwtConfig.java \
        core/core-backend/src/main/resources/application.yml \
        core/core-backend/src/main/resources/application-standalone.yml
git commit -m "fix(security): JWT 密钥外部化，支持配置和环境变量"
```

---

### Task 2: TokenUtils 添加过期验证

**Files:**
- Modify: `sdk/common/src/main/java/io/dataease/utils/TokenUtils.java`

- [ ] **Step 1: 修改 validate() 方法，添加过期检查**

当前 `validate()` 使用 `JWT.require(algorithm).build().verify(token)` — auth0-jwt 的 `verify()` 实际上**已经包含过期检查**。需要确认 `JWT.decode()` 路径不绕过它。

将 `validateLinkToken()` 中的 `JWT.decode()` 改为带签名验证的完整验证：

```java
public static TokenUserBO validateLinkToken(String linkToken) {
    if (StringUtils.isBlank(linkToken)) {
        String uri = ServletUtils.request().getRequestURI();
        DEException.throwException("link token is empty for uri {" + uri + "}");
    }
    if (StringUtils.length(linkToken) < 100) {
        DEException.throwException("token is invalid");
    }
    try {
        // 添加签名验证和过期检查
        com.auth0.jwt.algorithms.Algorithm algorithm = com.auth0.jwt.algorithms.Algorithm.HMAC256(SECRET);
        JWT.require(algorithm).build().verify(linkToken);
    } catch (com.auth0.jwt.exceptions.TokenExpiredException e) {
        DEException.throwException("link token 已过期");
    } catch (Exception e) {
        DEException.throwException("link token verification failed");
    }
    DecodedJWT jwt = JWT.decode(linkToken);
    Long userId = jwt.getClaim("uid").asLong();
    Long oid = jwt.getClaim("oid").asLong();
    if (ObjectUtils.isEmpty(userId)) {
        DEException.throwException("link token格式错误！");
    }
    return new TokenUserBO(userId, oid);
}
```

> **注意**: Link Token 的签名密钥可能不是全局 SECRET，而是资源专属密码。这个修改需要确保与 `LinkTokenUtil.generate()` 和 `DeLinkAop` 的验证逻辑一致。如果 Link Token 使用资源专属密码签名，则此处的验证需要查询数据库获取密码。**简化方案**: 在 TokenFilter 层不做 Link Token 的完整签名验证（保留给 DeLinkAop），但添加过期检查：

```java
public static TokenUserBO validateLinkToken(String linkToken) {
    // ... 空值和长度检查 ...
    DecodedJWT jwt = JWT.decode(linkToken);
    // 添加过期检查
    if (jwt.getExpiresAt() != null && jwt.getExpiresAt().before(new java.util.Date())) {
        DEException.throwException("link token 已过期");
    }
    Long userId = jwt.getClaim("uid").asLong();
    // ... 其余逻辑不变 ...
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd core/core-backend && mvn compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add sdk/common/src/main/java/io/dataease/utils/TokenUtils.java
git commit -m "fix(security): Link Token 添加过期检查"
```

---

### Task 3: 移除明文密码比对 + 登录限速

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/core/permissions/login/CoreLoginServer.java`

- [ ] **Step 1: 移除明文密码比对，改为强制 BCrypt**

> **升级注意**: 如果数据库中存在非 BCrypt 格式的密码用户，升级后这些用户将无法登录，需管理员重置密码。Desktop 模式的 `SubstituleLoginServer` 使用独立验证逻辑，不受影响。

将登录验证逻辑改为：

```java
boolean matches = false;
if (user.getPassword() != null) {
    if (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$")) {
        matches = passwordEncoder.matches(pwd, user.getPassword());
    } else {
        // 不再明文比对，直接返回错误，要求管理员重置密码
        DEException.throwException("User not found or password incorrect");
    }
}
```

- [ ] **Step 2: 添加简单的内存登录限速**

在 `CoreLoginServer` 中添加限速逻辑：

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// 类级别字段
private static final ConcurrentHashMap<String, AtomicInteger> loginFailCount = new ConcurrentHashMap<>();
private static final ConcurrentHashMap<String, Long> lockTime = new ConcurrentHashMap<>();
private static final int MAX_FAIL_COUNT = 5;
private static final long LOCK_DURATION_MS = 15 * 60 * 1000; // 15分钟

private void checkLoginRate(String name) {
    Long lockedAt = lockTime.get(name);
    if (lockedAt != null) {
        if (System.currentTimeMillis() - lockedAt < LOCK_DURATION_MS) {
            DEException.throwException("Account temporarily locked, please try again later");
        } else {
            lockTime.remove(name);
            loginFailCount.remove(name);
        }
    }
}

private void recordLoginFail(String name) {
    AtomicInteger count = loginFailCount.computeIfAbsent(name, k -> new AtomicInteger(0));
    if (count.incrementAndGet() >= MAX_FAIL_COUNT) {
        lockTime.put(name, System.currentTimeMillis());
    }
}

private void clearLoginFail(String name) {
    loginFailCount.remove(name);
    lockTime.remove(name);
}
```

- [ ] **Step 3: 在 localLogin 方法中集成限速**

```java
@Override
public TokenVO localLogin(PwdLoginDTO dto) {
    String name = dto.getName();
    String pwd = dto.getPwd();

    checkLoginRate(name);  // 新增

    // ... 原有的 RSA 解密和用户查询 ...

    if (!matches) {
        recordLoginFail(name);  // 新增
        DEException.throwException("User not found or password incorrect");
    }

    clearLoginFail(name);  // 新增

    // ... 原有的 Token 生成逻辑 ...
}
```

- [ ] **Step 4: 验证编译通过**

Run: `cd core/core-backend && mvn compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/core/permissions/login/CoreLoginServer.java
git commit -m "fix(security): 移除明文密码比对，添加登录限速（5次失败锁定15分钟）"
```

---

### Task 4: Desktop 模式添加 localhost 检查

**Files:**
- Modify: `sdk/common/src/main/java/io/dataease/auth/filter/TokenFilter.java`

- [ ] **Step 1: 添加 localhost 验证**

在 TokenFilter 的 desktop 分支中添加来源检查：

```java
if (isDesktop) {
    // 安全检查：仅允许 localhost 访问
    String remoteAddr = request.getRemoteAddr();
    if (!isLocalAddress(remoteAddr)) {
        LogUtil.error("Desktop mode rejected non-local request from: " + remoteAddr);
        HttpServletResponse res = (HttpServletResponse) servletResponse;
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
    }
    UserUtils.setDesktopUser();
    filterChain.doFilter(servletRequest, servletResponse);
    return;
}
```

在 `TokenFilter` 类中添加私有方法：

```java
private boolean isLocalAddress(String addr) {
    if (addr == null) return false;
    return "127.0.0.1".equals(addr) || "0:0:0:0:0:0:0:1".equals(addr) || "localhost".equalsIgnoreCase(addr);
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd core/core-backend && mvn compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add sdk/common/src/main/java/io/dataease/auth/filter/TokenFilter.java
git commit -m "fix(security): Desktop 模式添加 localhost 来源检查"
```

---

## Phase 2: P1 — 一周内修复（注入防护）

### Task 5: 前端安装 DOMPurify 并创建工具函数

**Files:**
- Modify: `core/core-frontend/package.json`
- Create: `core/core-frontend/src/utils/sanitize.ts`

- [ ] **Step 1: 安装 DOMPurify**

Run: `cd core/core-frontend && npm install dompurify && npm install -D @types/dompurify`

- [ ] **Step 2: 创建 sanitize 工具函数**

新建 `core/core-frontend/src/utils/sanitize.ts`：

```typescript
import DOMPurify from 'dompurify'

// 允许的标签和属性（白名单）
const ALLOWED_TAGS = [
  'b', 'i', 'em', 'strong', 'a', 'p', 'br', 'span', 'div',
  'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'table', 'thead', 'tbody', 'tr', 'th', 'td', 'img', 'font'
]

const ALLOWED_ATTR = [
  'href', 'target', 'style', 'class', 'color', 'bgcolor',
  'align', 'valign', 'width', 'height', 'src', 'alt', 'face', 'size'
]

export function sanitizeHtml(dirty: string): string {
  if (!dirty) return dirty
  return DOMPurify.sanitize(dirty, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    ALLOW_DATA_ATTR: false
  })
}
```

- [ ] **Step 3: Commit**

```bash
git add core/core-frontend/package.json core/core-frontend/package-lock.json core/core-frontend/src/utils/sanitize.ts
git commit -m "feat(security): 引入 DOMPurify，创建 HTML 过滤工具函数"
```

---

### Task 6: v-text 组件添加 XSS 过滤

**Files:**
- Modify: `core/core-frontend/src/custom-component/v-text/Component.vue`

- [ ] **Step 1: 添加 sanitize 导入和计算属性**

在 `<script setup>` 开头添加：

```typescript
import { sanitizeHtml } from '@/utils/sanitize'
```

添加计算属性（在 `const { editMode, curComponent }` 之后）：

```typescript
const safePropValue = computed(() => sanitizeHtml(element.value.propValue))
```

- [ ] **Step 2: 替换 v-html 绑定**

将第 126 行：
```vue
v-html="element['propValue']"
```
改为：
```vue
v-html="safePropValue"
```

将第 132 行（预览模式）：
```vue
v-html="element['propValue']"
```
改为：
```vue
v-html="safePropValue"
```

需要在 import 中添加 `computed`（检查是否已有）。

- [ ] **Step 3: Commit**

```bash
git add core/core-frontend/src/custom-component/v-text/Component.vue
git commit -m "fix(security): v-text 组件添加 XSS 过滤"
```

---

### Task 7: scroll-text 组件添加 XSS 过滤

**Files:**
- Modify: `core/core-frontend/src/custom-component/scroll-text/Component.vue`

- [ ] **Step 1: 添加 sanitize 导入和计算属性**

```typescript
import { sanitizeHtml } from '@/utils/sanitize'
```

```typescript
const safePropValue = computed(() => sanitizeHtml(element.value.propValue))
```

- [ ] **Step 2: 替换所有 v-html 绑定**

编辑模式（约第 141 行附近）：
```vue
v-html="safePropValue"
```

预览模式（约第 141 行附近）：
```vue
v-html="safePropValue"
```

- [ ] **Step 3: Commit**

```bash
git add core/core-frontend/src/custom-component/scroll-text/Component.vue
git commit -m "fix(security): scroll-text 组件添加 XSS 过滤"
```

---

### Task 8: chart tooltip 添加 XSS 过滤

**Files:**
- Modify: `core/core-frontend/src/views/chart/components/views/index.vue`

- [ ] **Step 1: 添加 sanitize 导入**

在 script setup 部分添加：

```typescript
import { sanitizeHtml } from '@/utils/sanitize'
```

- [ ] **Step 2: 添加计算属性**

```typescript
const safeRemark = computed(() => sanitizeHtml(state.title_remark.remark))
```

- [ ] **Step 3: 替换第 1169 行的 v-html**

```vue
v-html="safeRemark"
```

- [ ] **Step 4: Commit**

```bash
git add core/core-frontend/src/views/chart/components/views/index.vue
git commit -m "fix(security): 图表 tooltip 添加 XSS 过滤"
```

---

### Task 9: Swagger 端点条件化

**Files:**
- Modify: `sdk/common/src/main/java/io/dataease/utils/WhitelistUtils.java`
- Modify: `core/core-backend/src/main/resources/application.yml`

- [ ] **Step 1: 添加配置读取能力**

在 `WhitelistUtils` 中添加环境判断：

> **注意**: 项目当前 profiles 为 `standalone`/`desktop`/`distributed`，没有 `dev` profile。桌面模式下保持 Swagger 可访问便于调试。

```java
public static boolean isDevMode() {
    Environment env = CommonBeanFactory.getBean(Environment.class);
    if (env == null) return false;
    String[] profiles = env.getActiveProfiles();
    for (String p : profiles) {
        if ("dev".equals(p) || "desktop".equals(p)) return true;
    }
    return false;
}
```

- [ ] **Step 2: 在 match() 方法中条件化 Swagger 端点**

将 `WHITE_PATH` 中的 `/swagger-resources` 和 `/doc.html` 移到条件判断：

```java
// 在 match() 方法的 return 语句中添加条件
|| (isDevMode() && StringUtils.equalsAny(requestURI, "/swagger-resources", "/doc.html", "/swagger-ui.html", "/v3/api-docs"))
```

同时从 `WHITE_PATH` 列表中移除这四项。

- [ ] **Step 3: Commit**

```bash
git add sdk/common/src/main/java/io/dataease/utils/WhitelistUtils.java
git commit -m "fix(security): Swagger 文档端点仅在开发模式可访问"
```

---

## Phase 3: P2 — 一个月内修复

### Task 10: SpEL 表达式安全化

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/system/interceptor/DePermitAop.java`

- [ ] **Step 1: 将 StandardEvaluationContext 替换为 SimpleEvaluationContext**

修改 `evalPermitExpr()` 方法：

```java
import org.springframework.expression.spel.support.SimpleEvaluationContext;

private String evalPermitExpr(String expr, Object[] args) {
    if (StringUtils.isBlank(expr)) {
        return null;
    }
    if (!StringUtils.contains(expr, "#")) {
        return expr;
    }
    // 使用 SimpleEvaluationContext 替代 StandardEvaluationContext，禁用危险操作
    SimpleEvaluationContext ctx = SimpleEvaluationContext.forReadOnlyDataBinding().build();
    for (int i = 0; i < args.length; i++) {
        ctx.setVariable("p" + i, args[i]);
        ctx.setVariable("a" + i, args[i]);
    }
    Expression e = expressionParser.parseExpression(expr);
    Object val = e.getValue(ctx);
    return val == null ? null : val.toString();
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd core/core-backend && mvn compile -DskipTests`
Expected: BUILD SUCCESS

> **注意**: `SimpleEvaluationContext` 不支持 `+` 字符串拼接运算符，但 DePermit 注解中使用了 `#p0.id+':read'` 这样的表达式。需要测试是否兼容。如果不兼容，改为正则白名单验证方案。

- [ ] **Step 3: 如果 SimpleEvaluationContext 不兼容，使用正则白名单方案**

> 实际 DePermit 注解使用的表达式模式：`#p0.id+':read'`、`#p0.pid + ':manage'`（有空格）、`#p0 + ':share'`（无属性访问）、`#p0.dvId+':export_view'`。正则需覆盖所有变体。

```java
private static final java.util.regex.Pattern SAFE_EXPR = java.util.regex.Pattern.compile(
    "^#[pa]\\d+(\\.\\w+)*\\s*\\+\\s*'[^']*'(\\s*,\\s*#[pa]\\d+(\\.\\w+)*\\s*\\+\\s*'[^']*')*$"
);

private String evalPermitExpr(String expr, Object[] args) {
    if (StringUtils.isBlank(expr)) return null;
    if (!StringUtils.contains(expr, "#")) return expr;
    // 白名单验证：仅允许 #p0.xxx / #a0.xxx + 字符串常量
    if (!SAFE_EXPR.matcher(expr).matches()) {
        DEException.throwException("Invalid permission expression");
    }
    // 原有 StandardEvaluationContext 逻辑保持不变
    StandardEvaluationContext ctx = new StandardEvaluationContext();
    for (int i = 0; i < args.length; i++) {
        ctx.setVariable("p" + i, args[i]);
        ctx.setVariable("a" + i, args[i]);
    }
    Expression e = expressionParser.parseExpression(expr);
    Object val = e.getValue(ctx);
    return val == null ? null : val.toString();
}
```

- [ ] **Step 4: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/system/interceptor/DePermitAop.java
git commit -m "fix(security): SpEL 权限表达式添加白名单验证"
```

---

## Phase 4: P3 — 长期（不纳入本次实施）

- SQL 查询全面参数化审计（工作量大，需独立规划）
- Link Token 默认密码随机化（需数据库迁移）
- CORS 生产环境强制 strict
- RSA 私钥存储优化
- CSRF Token 机制（需要前后端联动，架构改动较大）

---

## 测试策略

每个 Task 完成后：
1. 后端：`cd core/core-backend && mvn compile -DskipTests` 确保编译通过
2. 前端：`cd core/core-frontend && npm run build:base` 确保构建通过
3. 手动验证：启动应用，测试登录、Token 刷新、分享链接等核心流程

## 回滚策略

每个 Task 独立 commit，如出问题可 `git revert` 单个 commit 回滚。
