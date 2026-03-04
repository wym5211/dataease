# 用户权限管理功能测试实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 使用 Playwright MCP 自动化测试 DataEase 用户权限管理完整流程

**Architecture:** 顺序启动后端(8100)和前端(8081)，使用 Playwright MCP 浏览器工具执行端到端测试，覆盖登录→用户管理→创建用户→分配角色→验证的完整流程

**Tech Stack:** Bash, Playwright MCP, DataEase (Vue3 + Spring Boot)

---

## 前置条件

- MySQL 数据库已运行，且存在 `dataease10` 数据库
- 后端 JAR 包已构建: `core/core-backend/target/CoreApplication.jar`
- 前端依赖已安装: `core/core-frontend/node_modules` 存在

---

## Task 1: 检查环境并启动后端服务

**Files:**
- Read: `core/core-backend/src/main/resources/application-standalone.yml` (检查配置)
- Check: `core/core-backend/target/CoreApplication.jar` (检查 JAR 是否存在)

**Step 1: 验证后端 JAR 包存在**

```bash
ls -la core/core-backend/target/CoreApplication.jar
```

Expected: 文件存在

**Step 2: 检查 MySQL 连接配置**

Read: `core/core-backend/src/main/resources/application-standalone.yml`

Expected: 包含正确的 MySQL 连接信息

**Step 3: 停止旧的后端进程（如果有）**

```bash
for pid in $(netstat -ano | grep ":8100" | grep "LISTENING" | awk '{print $5}'); do
    taskkill //F //PID $pid 2>/dev/null || true
done
```

**Step 4: 在后台启动后端服务**

```bash
cd core/core-backend
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME\bin:$PATH"

java -Dspring.profiles.active=standalone \
     -Ddataease.path.driver=../../drivers \
     -Ddataease.path.custom-drivers=../../custom-drivers/ \
     -Ddataease.path.ehcache=./cache-standalone \
     -jar target/CoreApplication.jar > ../../logs/backend-standalone.log 2>&1 &

echo "Backend PID: $!"
```

**Step 5: 等待后端启动完成（检测日志）**

```bash
# 等待最多 60 秒
timeout=60
while [ $timeout -gt 0 ]; do
    if grep -q "Started CoreApplication" logs/backend-standalone.log 2>/dev/null; then
        echo "Backend started successfully!"
        break
    fi
    sleep 2
    timeout=$((timeout - 2))
    echo "Waiting for backend... ($timeout seconds left)"
done

if [ $timeout -le 0 ]; then
    echo "Backend failed to start!"
    cat logs/backend-standalone.log
    exit 1
fi
```

Expected: 输出 "Backend started successfully!"

**Step 6: 验证后端健康状态**

```bash
curl -s http://localhost:8100/de2api/user/info | head -c 200
```

Expected: 返回 JSON 响应（可能包含错误码，但服务已运行）

---

## Task 2: 启动前端开发服务器

**Files:**
- Check: `core/core-frontend/node_modules` (检查依赖)

**Step 1: 验证前端依赖已安装**

```bash
ls -la core/core-frontend/node_modules/.package-lock.json
```

Expected: 文件存在

**Step 2: 停止旧的前端进程（如果有）**

```bash
for pid in $(netstat -ano | grep ":8081" | grep "LISTENING" | awk '{print $5}'); do
    taskkill //F //PID $pid 2>/dev/null || true
done
```

**Step 3: 在后台启动前端服务**

```bash
cd core/core-frontend

npm run dev:win > ../../logs/frontend-dev.log 2>&1 &

echo "Frontend PID: $!"
```

**Step 4: 等待前端启动完成（检测日志）**

```bash
# 等待最多 60 秒
timeout=60
while [ $timeout -gt 0 ]; do
    if grep -q "Local:.*http://localhost:8081" ../logs/frontend-dev.log 2>/dev/null; then
        echo "Frontend started successfully!"
        break
    fi
    sleep 2
    timeout=$((timeout - 2))
    echo "Waiting for frontend... ($timeout seconds left)"
done

if [ $timeout -le 0 ]; then
    echo "Frontend failed to start!"
    cat ../logs/frontend-dev.log
    exit 1
fi
```

Expected: 输出 "Frontend started successfully!"

**Step 5: 验证前端可访问**

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8081
```

Expected: 返回 200

---

## Task 3: 使用 Playwright MCP 打开浏览器并访问登录页

**Step 1: 创建浏览器会话**

使用 Playwright MCP 的 `mcp__Playwright__browser_navigate`:

```json
{
  "url": "http://localhost:8081"
}
```

**Step 2: 获取页面快照**

使用 `mcp__Playwright__browser_snapshot` 检查页面是否加载完成

Expected: 页面包含登录表单元素（用户名、密码输入框）

---

## Task 4: 执行登录操作

**Step 1: 填写用户名**

使用 `mcp__Playwright__browser_type`:
- 找到用户名输入框（placeholder 或 label 包含"用户名"或"username"）
- 输入: `admin`

**Step 2: 填写密码**

使用 `mcp__Playwright__browser_type`:
- 找到密码输入框
- 输入: `123456`

**Step 3: 点击登录按钮**

使用 `mcp__Playwright__browser_click`:
- 找到登录按钮（文本包含"登录"或"Login"）
- 点击

**Step 4: 等待登录完成并验证**

使用 `mcp__Playwright__browser_wait_for`:
- 等待文本 "工作台" 或 "首页" 或 "数据大屏" 出现
- 或等待 URL 变为非登录页

Expected: 成功跳转到首页

**Step 5: 截图保存（可选）**

使用 `mcp__Playwright__browser_take_screenshot` 保存登录成功状态

---

## Task 5: 导航到用户管理页面

**Step 1: 点击权限管理菜单**

使用 `mcp__Playwright__browser_click`:
- 找到包含"权限"或"权限管理"的菜单项
- 点击展开子菜单

**Step 2: 点击用户管理子菜单**

使用 `mcp__Playwright__browser_click`:
- 找到包含"用户"或"用户管理"的菜单项
- 点击

**Step 3: 验证用户列表页面加载**

使用 `mcp__Playwright__browser_wait_for`:
- 等待页面包含"用户列表"或"用户管理"或表格出现

使用 `mcp__Playwright__browser_snapshot` 获取页面内容

Expected: 页面显示用户列表表格，包含至少一个用户（admin）

---

## Task 6: 测试创建新用户

**Step 1: 点击"新建用户"按钮**

使用 `mcp__Playwright__browser_click`:
- 找到包含"新建"或"添加"或"+"的按钮
- 点击

**Step 2: 等待对话框打开**

使用 `mcp__Playwright__browser_wait_for`:
- 等待对话框标题包含"新建用户"或"添加用户"

**Step 3: 填写用户信息**

使用 `mcp__Playwright__browser_fill_form` 或逐个填写:
- 用户名: `testuser001`
- 姓名: `测试用户`
- 邮箱: `testuser001@example.com`
- 电话: `13800138000`
- 密码: `Test@123456`

**Step 4: 选择角色**

使用 `mcp__Playwright__browser_click`:
- 点击角色下拉框
- 选择"普通用户"或"普通成员"角色

**Step 5: 保存用户**

使用 `mcp__Playwright__browser_click`:
- 找到"确定"或"保存"按钮
- 点击

**Step 6: 验证用户创建成功**

使用 `mcp__Playwright__browser_wait_for`:
- 等待提示消息包含"成功"或"创建成功"
- 或等待对话框关闭

使用 `mcp__Playwright__browser_snapshot` 检查用户列表

Expected: 新用户 `testuser001` 出现在列表中

---

## Task 7: 验证用户详情

**Step 1: 搜索或找到新创建的用户**

使用 `mcp__Playwright__browser_type`:
- 在用户列表搜索框输入 `testuser001`

**Step 2: 验证用户信息正确显示**

使用 `mcp__Playwright__browser_snapshot`:
- 检查列表中显示正确的用户名、姓名、邮箱、角色

Expected: 用户信息显示正确

---

## Task 8: 清理测试数据（可选）

**Step 1: 删除测试用户**

使用 `mcp__Playwright__browser_click`:
- 找到测试用户的"删除"按钮
- 点击

**Step 2: 确认删除**

使用 `mcp__Playwright__browser_click`:
- 在确认对话框点击"确定"

**Step 3: 验证删除成功**

使用 `mcp__Playwright__browser_wait_for`:
- 等待提示消息包含"删除成功"
- 或用户不再出现在列表中

---

## Task 9: 关闭浏览器并停止服务

**Step 1: 关闭浏览器会话**

使用 `mcp__Playwright__browser_close`

**Step 2: 停止前端服务**

```bash
for pid in $(netstat -ano | grep ":8081" | grep "LISTENING" | awk '{print $5}'); do
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
2. **测试步骤**: 哪些步骤成功，哪些失败
3. **截图**: 关键步骤的截图路径
4. **日志**: 后端和前端的日志文件路径

---

## 故障排查

### 后端启动失败
- 检查 MySQL 是否运行: `mysql -u root -p -e "SHOW DATABASES;"`
- 检查数据库是否存在: `mysql -u root -p -e "USE dataease10;"`
- 查看日志: `cat logs/backend-standalone.log`

### 前端启动失败
- 检查 Node.js 版本: `node -v` (需要 20+)
- 检查依赖: `ls core/core-frontend/node_modules`
- 查看日志: `cat logs/frontend-dev.log`

### 浏览器测试失败
- 检查服务是否运行: `curl http://localhost:8081` 和 `curl http://localhost:8100`
- 手动访问测试: 在浏览器中打开 http://localhost:8081
- 检查页面元素选择器是否正确
