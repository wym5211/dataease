# 用户角色权限测试实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 使用 Playwright MCP 自动化测试 DataEase 的 RBAC 权限体系，验证不同角色的菜单访问权限控制

**Architecture:** 启动后端(8100)和前端(8080)，使用 Playwright MCP 浏览器工具执行端到端测试，创建不同权限的角色和用户，验证权限控制是否生效

**Tech Stack:** Bash, Playwright MCP, DataEase (Vue3 + Spring Boot)

---

## 前置条件

- MySQL 数据库已运行，且存在 `dataease10` 数据库
- 后端 JAR 包已构建: `core/core-backend/target/CoreApplication.jar`
- 前端依赖已安装: `core/core-frontend/node_modules` 存在

---

## Task 1: 检查环境并启动后端服务

**Files:**
- Check: `core/core-backend/target/CoreApplication.jar`
- Read: `core/core-backend/src/main/resources/application-standalone.yml`

**Step 1: 验证后端 JAR 包存在**

```bash
ls -la core/core-backend/target/CoreApplication.jar
```

Expected: 文件存在

**Step 2: 停止旧的后端进程（如果有）**

```bash
for pid in $(netstat -ano | grep ":8100" | grep "LISTENING" | awk '{print $5}'); do
    taskkill //F //PID $pid 2>/dev/null || true
done
```

**Step 3: 在后台启动后端服务**

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

**Step 4: 等待后端启动完成**

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

Expected: "Backend started successfully!"

---

## Task 2: 启动前端开发服务器

**Files:**
- Check: `core/core-frontend/node_modules/.package-lock.json`

**Step 1: 验证前端依赖已安装**

```bash
ls core/core-frontend/node_modules/.package-lock.json
```

Expected: 文件存在

**Step 2: 停止旧的前端进程（如果有）**

```bash
for pid in $(netstat -ano | grep ":8080" | grep "LISTENING" | awk '{print $5}'); do
    taskkill //F //PID $pid 2>/dev/null || true
done
```

**Step 3: 在后台启动前端服务**

```bash
cd core/core-frontend
npm run dev:win > ../../logs/frontend-dev.log 2>&1 &
echo $! > ../../logs/frontend.pid
```

**Step 4: 等待前端启动完成**

```bash
timeout=60
while [ $timeout -gt 0 ]; do
    if grep -q "Local:.*http://localhost:8080" ../logs/frontend-dev.log 2>/dev/null; then
        echo "✅ Frontend started successfully!"
        break
    fi
    sleep 2
    timeout=$((timeout - 2))
done
```

Expected: "Frontend started successfully!"

---

## Task 3: 管理员登录并创建角色

**Step 1: 打开浏览器并访问登录页**

使用 Playwright MCP:
```json
{
  "tool": "mcp__Playwright__browser_navigate",
  "params": {
    "url": "http://localhost:8080"
  }
}
```

**Step 2: 清除本地存储（确保需要登录）**

使用 `mcp__Playwright__browser_evaluate`:
```javascript
() => {
  localStorage.clear();
  sessionStorage.clear();
  return 'Storage cleared';
}
```

**Step 3: 刷新页面进入登录页**

使用 `mcp__Playwright__browser_navigate`:
```json
{
  "url": "http://localhost:8080/#/login"
}
```

**Step 4: 填写登录信息并登录**

使用 `mcp__Playwright__browser_fill_form`:
- 用户名: admin
- 密码: 123456

然后点击登录按钮。

**Step 5: 验证登录成功**

使用 `mcp__Playwright__browser_wait_for` 等待"工作台"或页面加载完成。

**Step 6: 导航到角色管理页面**

使用 `mcp__Playwright__browser_navigate`:
```json
{
  "url": "http://localhost:8080/#/permissions/role"
}
```

**Step 7: 创建"普通用户"角色**

- 点击"创建角色"按钮
- 填写角色名称: 普通用户
- 填写角色标识: normal_user
- 保存

**Step 8: 为"普通用户"角色分配菜单权限**

- 点击角色行的"菜单权限"按钮
- 勾选: 工作台、仪表板
- 不勾选: 数据大屏、数据准备
- 保存

**Step 9: 创建"受限用户"角色**

- 点击"创建角色"按钮
- 填写角色名称: 受限用户
- 填写角色标识: limited_user
- 保存

**Step 10: 为"受限用户"角色分配菜单权限**

- 点击角色行的"菜单权限"按钮
- 只勾选: 工作台
- 不勾选: 仪表板、数据大屏、数据准备
- 保存

---

## Task 4: 创建测试用户

**Step 1: 导航到用户管理页面**

使用 `mcp__Playwright__browser_navigate`:
```json
{
  "url": "http://localhost:8080/#/permissions/user"
}
```

**Step 2: 创建 user_admin 用户（管理员角色）**

- 点击"创建用户"按钮
- 用户名: user_admin
- 姓名: 测试管理员
- 邮箱: useradmin@test.com
- 密码: Test@123456
- 角色: 管理员
- 保存

**Step 3: 创建 user_normal 用户（普通用户角色）**

- 点击"创建用户"按钮
- 用户名: user_normal
- 姓名: 测试普通用户
- 邮箱: usernormal@test.com
- 密码: Test@123456
- 角色: 普通用户
- 保存

**Step 4: 创建 user_limited 用户（受限用户角色）**

- 点击"创建用户"按钮
- 用户名: user_limited
- 姓名: 测试受限用户
- 邮箱: userlimited@test.com
- 密码: Test@123456
- 角色: 受限用户
- 保存

---

## Task 5: 测试 user_admin 权限

**Step 1: 退出管理员账号**

- 点击右上角管理员头像
- 点击"退出登录"

**Step 2: 使用 user_admin 登录**

- 输入用户名: user_admin
- 输入密码: Test@123456
- 点击登录

**Step 3: 验证工作台可访问**

- 检查是否存在"工作台"菜单
- 点击"工作台"菜单，验证页面能正常加载
- Expected: ✅ 可以访问

**Step 4: 验证仪表板可访问**

- 检查是否存在"仪表板"菜单
- 点击"仪表板"菜单，验证页面能正常加载
- Expected: ✅ 可以访问

**Step 5: 验证数据大屏可访问**

- 检查是否存在"数据大屏"菜单
- 点击"数据大屏"菜单，验证页面能正常加载
- Expected: ✅ 可以访问

**Step 6: 验证数据准备可访问**

- 检查是否存在"数据准备"菜单
- 点击"数据准备"菜单，验证页面能正常加载
- Expected: ✅ 可以访问

**Step 7: 截图保存测试结果**

使用 `mcp__Playwright__browser_take_screenshot`:
```json
{
  "filename": "test-user-admin.png"
}
```

---

## Task 6: 测试 user_normal 权限

**Step 1: 退出 user_admin 账号**

- 点击右上角用户头像
- 点击"退出登录"

**Step 2: 使用 user_normal 登录**

- 输入用户名: user_normal
- 输入密码: Test@123456
- 点击登录

**Step 3: 验证工作台可访问**

- 检查是否存在"工作台"菜单
- 点击"工作台"菜单
- Expected: ✅ 可以访问

**Step 4: 验证仪表板可访问**

- 检查是否存在"仪表板"菜单
- 点击"仪表板"菜单
- Expected: ✅ 可以访问

**Step 5: 验证数据大屏不可访问**

- 检查是否存在"数据大屏"菜单
- Expected: ❌ 菜单不存在或不可点击

**Step 6: 验证数据准备不可访问**

- 检查是否存在"数据准备"菜单
- Expected: ❌ 菜单不存在或不可点击

**Step 7: 截图保存测试结果**

使用 `mcp__Playwright__browser_take_screenshot`:
```json
{
  "filename": "test-user-normal.png"
}
```

---

## Task 7: 测试 user_limited 权限

**Step 1: 退出 user_normal 账号**

- 点击右上角用户头像
- 点击"退出登录"

**Step 2: 使用 user_limited 登录**

- 输入用户名: user_limited
- 输入密码: Test@123456
- 点击登录

**Step 3: 验证工作台可访问**

- 检查是否存在"工作台"菜单
- 点击"工作台"菜单
- Expected: ✅ 可以访问

**Step 4: 验证仪表板不可访问**

- 检查是否存在"仪表板"菜单
- Expected: ❌ 菜单不存在

**Step 5: 验证数据大屏不可访问**

- 检查是否存在"数据大屏"菜单
- Expected: ❌ 菜单不存在

**Step 6: 验证数据准备不可访问**

- 检查是否存在"数据准备"菜单
- Expected: ❌ 菜单不存在

**Step 7: 截图保存测试结果**

使用 `mcp__Playwright__browser_take_screenshot`:
```json
{
  "filename": "test-user-limited.png"
}
```

---

## Task 8: 清理测试数据

**Step 1: 退出 user_limited 账号**

- 点击右上角用户头像
- 点击"退出登录"

**Step 2: 管理员重新登录**

- 输入用户名: admin
- 输入密码: 123456
- 点击登录

**Step 3: 删除测试用户**

导航到用户管理页面，删除以下用户：
- user_admin
- user_normal
- user_limited

**Step 4: 删除测试角色**

导航到角色管理页面，删除以下角色：
- 普通用户
- 受限用户

**Step 5: 验证清理完成**

- 用户列表中不应存在测试用户
- 角色列表中不应存在测试角色

---

## Task 9: 关闭浏览器并停止服务

**Step 1: 关闭浏览器**

使用 `mcp__Playwright__browser_close`

**Step 2: 停止前端服务**

```bash
for pid in $(netstat -ano | grep ":8080" | grep "LISTENING" | awk '{print $5}'); do
    taskkill //F //PID $pid 2>/dev/null || true
done
echo "Frontend stopped"
```

**Step 3: 停止后端服务**

```bash
for pid in $(netstat -ano | grep ":8100" | grep "LISTENING" | awk '{print $5}'); do
    taskkill //F //PID $pid 2>/dev/null || true
done
echo "Backend stopped"
```

---

## 测试报告

测试完成后，输出以下信息：

1. **测试结果**: 通过 / 失败
2. **权限验证结果**:
   - user_admin: 所有菜单可访问 ✅
   - user_normal: 工作台、仪表板可访问，数据大屏、数据准备不可访问 ✅
   - user_limited: 仅工作台可访问，其他不可访问 ✅
3. **截图文件**:
   - test-user-admin.png
   - test-user-normal.png
   - test-user-limited.png
4. **日志文件**:
   - logs/backend-standalone.log
   - logs/frontend-dev.log

---

## 故障排查

### 角色权限不生效
- 检查是否正确保存了菜单权限
- 检查用户是否正确分配了角色
- 尝试刷新页面或重新登录

### 菜单仍然存在
- 前端可能有缓存，尝试强制刷新（Ctrl+F5）
- 检查是否正确退出了当前账号

### 用户无法登录
- 检查密码是否正确
- 检查用户状态是否为"启用"
