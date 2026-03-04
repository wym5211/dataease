# 启用 CoreLoginServer 支持普通用户登录实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修改 CoreLoginServer 配置，使其在 standalone 模式下优先加载，支持数据库用户登录验证

**Architecture:** 通过调整 Spring Bean 的加载条件和优先级，确保 CoreLoginServer 替代 SubstituleLoginServer，同时使用 BCryptPasswordEncoder 进行密码验证

**Tech Stack:** Java, Spring Boot, MyBatis, BCrypt, Playwright MCP

---

## Task 1: 检查当前 CoreLoginServer 状态

**Files:**
- Read: `core/core-backend/src/main/java/io/dataease/core/permissions/login/CoreLoginServer.java`

**Step 1: 读取当前文件内容**

确认文件存在并了解当前注解配置。

**Step 2: 验证文件结构**

确认包含以下关键元素：
- `@Service("loginServer")` 注解
- `@Primary` 注解
- `localLogin` 方法实现

**Expected:** 文件存在，包含标准的 CoreLoginServer 实现

---

## Task 2: 修改 CoreLoginServer 加载条件

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/core/permissions/login/CoreLoginServer.java`

**Step 1: 添加 @ConditionalOnExpression 注解**

```java
package io.dataease.core.permissions.login;

import io.dataease.api.permissions.login.api.LoginApi;
import io.dataease.api.permissions.login.dto.*;
import io.dataease.api.permissions.login.vo.*;
import io.dataease.api.permissions.user.dto.ModifyPwdRequest;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.auth.vo.TokenVO;
import io.dataease.exception.DEException;
import io.dataease.system.dao.auto.entity.SysUser;
import io.dataease.system.dao.auto.mapper.SysUserMapper;
import io.dataease.utils.RsaUtils;
import io.dataease.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;

@Service("loginServer")
@Primary
@ConditionalOnExpression("'${spring.profiles.active:standalone}'.contains('standalone') || '${spring.profiles.active:standalone}'.contains('distributed')")
@RestController
public class CoreLoginServer implements LoginApi {
    // ... rest of the existing code
}
```

**Step 2: 保存文件**

确保修改后的文件格式正确。

---

## Task 3: 重新构建后端项目

**Files:**
- Execute: Maven build command

**Step 1: 停止当前后端服务**

```bash
for pid in $(netstat -ano | grep ":8100" | grep "LISTENING" | awk '{print $5}'); do
    taskkill //F //PID $pid 2>/dev/null || true
done
echo "Backend stopped"
```

Expected: "Backend stopped"

**Step 2: 清理并重新构建**

```bash
cd core/core-backend
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"

# 清理之前的构建
rm -rf target/

# 重新构建
"C:/Program Files/JetBrains/IntelliJ IDEA 2023.2.1/plugins/maven/lib/maven3/bin/mvn" clean package -DskipTests
```

Expected: BUILD SUCCESS

**Step 3: 验证 JAR 文件生成**

```bash
ls -la target/CoreApplication.jar
```

Expected: 文件存在，大小 > 100MB

---

## Task 4: 启动修改后的后端服务

**Files:**
- Execute: Start backend with standalone profile

**Step 1: 启动后端服务**

```bash
cd core/core-backend
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"

java -Dspring.profiles.active=standalone \
     -Ddataease.path.driver=../../drivers \
     -Ddataease.path.custom-drivers=../../custom-drivers/ \
     -Ddataease.path.ehcache=./cache-standalone \
     -jar target/CoreApplication.jar > ../../logs/backend-standalone.log 2>&1 &

echo $! > ../../logs/backend.pid
```

**Step 2: 等待后端启动完成**

```bash
timeout=90
while [ $timeout -gt 0 ]; do
    if grep -q "Started CoreApplication" logs/backend-standalone.log 2>/dev/null; then
        echo "✅ Backend started successfully!"
        break
    fi
    sleep 2
    timeout=$((timeout - 2))
done
```

Expected: "✅ Backend started successfully!"

---

## Task 5: 验证 admin 用户仍可登录

**Files:**
- Test via Playwright MCP

**Step 1: 打开登录页面**

使用 `mcp__Playwright__browser_navigate`:
```json
{
  "url": "http://localhost:8080/#/login"
}
```

**Step 2: 填写 admin 登录信息**

使用 `mcp__Playwright__browser_fill_form`:
- 账号: `admin`
- 密码: `123456`

**Step 3: 点击登录按钮**

使用 `mcp__Playwright__browser_click` 点击登录按钮

**Step 4: 验证登录成功**

使用 `mcp__Playwright__browser_wait_for` 等待工作台页面加载

Expected: 成功跳转到工作台页面

**Step 5: 截图保存**

使用 `mcp__Playwright__browser_take_screenshot`:
```json
{
  "filename": "admin-login-after-fix.png"
}
```

---

## Task 6: 创建测试用户并验证登录

**Files:**
- Test via Playwright MCP

**Step 1: 导航到用户管理页面**

使用 `mcp__Playwright__browser_navigate`:
```json
{
  "url": "http://localhost:8080/#/permissions/user"
}
```

**Step 2: 创建测试用户**

点击"创建用户"按钮，填写：
- 用户名: `test_login_user`
- 姓名: `登录测试用户`
- 邮箱: `testlogin@test.com`
- 手机号: `13800138099`
- 密码: `Test@123456`
- 角色: 选择任意角色（如"普通用户"）

**Step 3: 保存用户**

点击"确定"按钮保存用户

**Step 4: 验证用户创建成功**

等待提示"创建成功"，并在列表中看到新用户

---

## Task 7: 测试新用户登录

**Files:**
- Test via Playwright MCP

**Step 1: 退出 admin 账号**

点击用户头像，选择"退出系统"

**Step 2: 使用新用户登录**

填写：
- 账号: `test_login_user`
- 密码: `Test@123456`

**Step 3: 点击登录按钮**

**Step 4: 验证登录成功**

等待页面跳转，验证是否进入系统

Expected: 成功登录，显示工作台页面

**Step 5: 截图保存**

使用 `mcp__Playwright__browser_take_screenshot`:
```json
{
  "filename": "new-user-login-success.png"
}
```

---

## Task 8: 测试错误密码登录失败

**Files:**
- Test via Playwright MCP

**Step 1: 退出当前用户**

点击用户头像，选择"退出系统"

**Step 2: 尝试错误密码登录**

填写：
- 账号: `test_login_user`
- 密码: `WrongPassword123`

**Step 3: 点击登录按钮**

**Step 4: 验证登录失败**

Expected: 显示错误提示"用户名或密码错误"

---

## Task 9: 清理测试数据

**Files:**
- Execute via Playwright MCP

**Step 1: 使用 admin 登录**

重新登录 admin 账号

**Step 2: 删除测试用户**

导航到用户管理页面，删除 `test_login_user`

**Step 3: 验证清理完成**

确认测试用户已从列表中移除

---

## Task 10: 停止服务并提交代码

**Files:**
- Execute: Git commands

**Step 1: 停止前端服务**

```bash
for pid in $(netstat -ano | grep ":8080" | grep "LISTENING" | awk '{print $5}'); do
    taskkill //F //PID $pid 2>/dev/null || true
done
echo "Frontend stopped"
```

**Step 2: 停止后端服务**

```bash
for pid in $(netstat -ano | grep ":8100" | grep "LISTENING" | awk '{print $5}'); do
    taskkill //F //PID $pid 2>/dev/null || true
done
echo "Backend stopped"
```

**Step 3: 提交代码修改**

```bash
git add core/core-backend/src/main/java/io/dataease/core/permissions/login/CoreLoginServer.java
git commit -m "fix(login): enable CoreLoginServer for standalone mode to support database user authentication

- Add @ConditionalOnExpression to ensure CoreLoginServer loads in standalone mode
- Allows non-admin users to login with database authentication
- Maintains backward compatibility with admin login"
```

---

## 故障排查

### 问题 1: CoreLoginServer 仍未生效

**诊断:**
```bash
curl -X POST http://localhost:8100/api/login/localLogin \
  -H "Content-Type: application/json" \
  -d '{"name":"testuser","pwd":"testpass"}'
```

如果返回"仅admin账号可用"，说明 SubstituleLoginServer 仍在使用。

**解决:**
检查构建是否成功，确保修改后的类已编译到 JAR 中。

### 问题 2: 密码验证失败

**诊断:** 检查数据库中用户密码字段格式

**解决:** CoreLoginServer 会自动处理明文密码（升级为 BCrypt），首次登录可能较慢。

### 问题 3: 用户状态为禁用

**诊断:** 检查 `sys_user` 表的 `status` 字段

**解决:** 将 `status` 更新为 1（启用）

---

## 测试报告模板

测试完成后，输出以下信息：

1. **测试结果**: 通过 / 失败
2. **修改文件**: `CoreLoginServer.java`
3. **截图文件**:
   - `admin-login-after-fix.png`
   - `new-user-login-success.png`
4. **验证项**:
   - [ ] admin 用户登录正常
   - [ ] 新用户创建成功
   - [ ] 新用户登录成功
   - [ ] 错误密码登录失败
5. **日志文件**: `logs/backend-standalone.log`

---
