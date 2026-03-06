---
description: '编译和启动 DataEase 后端服务。自动检测并关闭已运行的后端进程，支持 standalone 和 desktop 两种模式。'
---

$ARGUMENTS

## 功能

快速编译和启动 DataEase 后端服务，自动处理以下场景：
- 检测端口 8100 是否被占用
- 自动关闭已运行的后端进程
- 编译并启动新实例

## 使用方法

### 完整流程（编译 + 启动）

```bash
# 1. 检测并关闭已运行的后端
echo "检查端口 8100 是否被占用..."
PID=$(netstat -ano | grep ':8100' | grep 'LISTENING' | awk '{print $5}' | head -1)
if [ -n "$PID" ] && [ "$PID" != "0" ]; then
    echo "发现后端进程 PID: $PID，正在关闭..."
    taskkill /F /PID $PID 2>/dev/null || kill -9 $PID 2>/dev/null
    sleep 2
    echo "后端进程已关闭"
else
    echo "端口 8100 未被占用"
fi

# 2. 编译后端
cd core/core-backend
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME/bin:/c/Program Files/JetBrains/IntelliJ IDEA 2023.2.1/plugins/maven/lib/maven3/bin:$PATH"
mvn clean package -DskipTests -P standalone

# 3. 启动后端 (Standalone 模式)
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
nohup java -jar target/CoreApplication.jar --spring.profiles.active=standalone > backend.log 2>&1 &
echo "后端服务已启动，日志查看: tail -f backend.log"
```

### 仅关闭后端进程

```bash
echo "检查端口 8100 是否被占用..."
PID=$(netstat -ano | grep ':8100' | grep 'LISTENING' | awk '{print $5}' | head -1)
if [ -n "$PID" ] && [ "$PID" != "0" ]; then
    echo "发现后端进程 PID: $PID，正在关闭..."
    taskkill /F /PID $PID 2>/dev/null || kill -9 $PID 2>/dev/null
    echo "后端进程已关闭"
else
    echo "未找到运行的后端进程"
fi
```

### 仅启动后端 (不编译)

```bash
# 先关闭已运行的实例
echo "检查端口 8100 是否被占用..."
PID=$(netstat -ano | grep ':8100' | grep 'LISTENING' | awk '{print $5}' | head -1)
if [ -n "$PID" ] && [ "$PID" != "0" ]; then
    echo "发现后端进程 PID: $PID，正在关闭..."
    taskkill /F /PID $PID 2>/dev/null || kill -9 $PID 2>/dev/null
    sleep 2
fi

# 启动
cd core/core-backend
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
java -jar target/CoreApplication.jar --spring.profiles.active=standalone
```

### 启动后端 (Desktop 模式)

```bash
# 先关闭已运行的实例
PID=$(netstat -ano | grep ':8100' | grep 'LISTENING' | awk '{print $5}' | head -1)
if [ -n "$PID" ] && [ "$PID" != "0" ]; then
    taskkill /F /PID $PID 2>/dev/null || kill -9 $PID 2>/dev/null
    sleep 2
fi

# 启动
cd core/core-backend
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
java -jar target/CoreApplication.jar --spring.profiles.active=desktop
```

## 快捷命令

### Windows PowerShell
```powershell
# 关闭占用 8100 端口的进程
$proc = Get-NetTCPConnection -LocalPort 8100 -ErrorAction SilentlyContinue | Select-Object -First 1
if ($proc) {
    Write-Host "关闭进程 PID: $($proc.OwningProcess)"
    Stop-Process -Id $proc.OwningProcess -Force
}
```

### Linux/Mac
```bash
# 查找并关闭进程
PID=$(lsof -ti:8100)
if [ -n "$PID" ]; then
    kill -9 $PID
    echo "已关闭进程 $PID"
fi
```

## 环境要求

- Java 21 (Eclipse Temurin 推荐)
- Maven 3.8+
- MySQL 8.0 (standalone 模式)

## 默认配置

- 端口: 8100
- 默认账号: admin
- 默认密码: DataEase@123456

## 启动模式说明

| 模式 | 数据库 | 适用场景 |
|------|--------|----------|
| standalone | MySQL | 多人协作/生产环境 |
| desktop | H2 | 本地开发/单用户 |
| distributed | MySQL | 企业分布式版 |
