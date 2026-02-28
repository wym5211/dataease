# 重启前后端并测试登录

## 任务清单

- [x] 1. 停止当前运行的前端和后端进程
- [x] 2. 启动后端服务（standalone模式）
- [x] 3. 启动前端开发服务器
- [x] 4. 等待服务启动完成
- [x] 5. 使用MCP测试登录API
- [x] 6. 验证登录响应

## 执行步骤

### 步骤1：检查并停止现有进程
- 查找Java后端进程
- 查找Node.js前端进程
- 优雅停止或强制终止

### 步骤2：启动后端
```bash
cd core/core-backend
java -jar target/CoreApplication.jar --spring.profiles.active=standalone
```

### 步骤3：启动前端
```bash
cd core/core-frontend
npm run dev:win
```

### 步骤4：测试登录
使用MCP工具调用登录API：
- URL: http://localhost:8100/de2api/login/localLogin
- 方法: POST
- 请求体: { "username": "admin", "password": "DataEase@123456" }

## 执行总结

### 遇到的问题及解决方案

1. **Java版本问题**
   - 错误：后端启动失败，提示需要Java 21
   - 原因：系统PATH中的Java版本过旧
   - 解决：使用完整路径 `C:/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot/bin/java.exe` 启动后端

2. **登录API字段名错误**
   - 错误：第一次测试返回 `code: 10001, msg: "login.validator.pwd"`
   - 原因：使用了错误的字段名 `username`/`password`
   - 解决：查看 `PwdLoginDTO` 源码，改用正确的字段名 `name`/`pwd`

### 测试结果

✅ **后端服务**
- 端口：8100
- 状态：运行中
- Java版本：21.0.6

✅ **前端服务**
- 端口：8081
- 状态：运行中

✅ **登录测试**
- API：POST /de2api/login/localLogin
- 用户：admin
- 响应：成功，返回JWT token
- Token内容：uid=1, oid=1

### 启动命令（参考）

**后端：**
```bash
cd "E:/cursor/dataease/core/core-backend"
"C:/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot/bin/java.exe" -jar target/CoreApplication.jar --spring.profiles.active=standalone
```

**前端：**
```bash
cd "E:/cursor/dataease/core/core-frontend"
npm run dev:win
```

### MCP浏览器测试

**测试步骤：**
1. 使用Playwright MCP工具导航到登录页面
2. 清除localStorage缓存
3. 填写登录表单：
   - 账号：admin
   - 密码：DataEase@123456
4. 点击登录按钮
5. 验证登录成功

**测试结果：**
- ✅ 登录表单正常显示
- ✅ 表单填写成功
- ✅ 登录按钮点击成功
- ✅ 页面跳转到工作台：`/#/workbranch/index`
- ✅ WebSocket连接成功（userId: 1）
- ✅ 用户认证通过

**控制台日志验证：**
```
>>> CONNECT
userId:1
accept-version:1.1,1.0

<<< CONNECTED
version:1.1
heart-beat:0,0

connected to server
>>> SUBSCRIBE
id:sub-0
destination:/user/1/topic
```

**截图记录：**
1. `login-page.png` - 登录页面初始状态
2. `login-form-filled.png` - 表单填写完成
3. `login-success.png` - 登录成功后的工作台页面

### 下一步建议

1. ✅ 使用MCP浏览器成功完成登录测试
2. 可以使用返回的token测试其他需要认证的API
3. 可以测试其他功能模块（仪表板、数据大屏等）
4. 建议配置环境变量以避免每次都使用Java完整路径
