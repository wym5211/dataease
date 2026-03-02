# Standalone模式启动后端计划

## 任务概述
使用standalone模式（MySQL数据库）启动DataEase后端服务

## 前置检查
- [x] jar包已存在：CoreApplication.jar (228MB)
- [x] MySQL服务正在运行（端口3306）
- [x] 配置文件已就绪：application-standalone.yml

## 执行步骤
- [x] 1. 进入core-backend目录
- [x] 2. 使用standalone profile启动应用
- [x] 3. 验证启动日志，确认服务正常运行
- [x] 4. 检查后端API是否可访问（http://localhost:8100）

## 执行结果
✅ 成功启动（耗时15.9秒）
- Java版本：JDK 21.0.6
- 数据库：MySQL 8.0 (dataease10)
- Flyway迁移：已完成（版本2.11.1）
- 端口：8100 (HTTP 200)

## 问题解决
1. ✅ 初始启动失败：Java版本错误（使用Java 8）
2. ✅ 解决方案：切换到JDK 21启动

## 配置信息
- 数据库：MySQL (localhost:3306/dataease10)
- 用户：root / 123456
- 后端端口：8100
- 默认账号：admin / DataEase@123456

## 注意事项
- 首次启动Flyway会自动执行数据库迁移
- 观察启动日志确保无错误

## 后续操作建议
1. 启动前端：`cd core/core-frontend && npm run dev:win`
2. 测试登录API：POST http://localhost:8100/de2api/login/localLogin
3. 查看完整日志：检查后台进程输出

## 启动命令参考
```bash
# Windows下使用JDK 21启动
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
cd E:/cursor/dataease/core/core-backend
java -jar target/CoreApplication.jar --spring.profiles.active=standalone
```
