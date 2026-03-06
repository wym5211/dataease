---
name: de-backend
description: 编译和启动 DataEase 后端服务。支持 standalone 和 desktop 两种模式，自动处理 Java 环境配置。
---

# DataEase 后端开发工具

快速编译和启动 DataEase 后端服务。

## 功能

- 一键编译后端代码
- 支持 standalone (MySQL) 和 desktop (H2) 模式启动
- 自动配置 Java 21 环境

## 使用方法

### 编译后端

```bash
cd core/core-backend
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME/bin:/c/Program Files/JetBrains/IntelliJ IDEA 2023.2.1/plugins/maven/lib/maven3/bin:$PATH"
mvn clean package -DskipTests -P standalone
```

### 启动后端 (Standalone 模式)

```bash
cd core/core-backend
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
java -jar target/CoreApplication.jar --spring.profiles.active=standalone
```

### 启动后端 (Desktop 模式)

```bash
cd core/core-backend
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
java -jar target/CoreApplication.jar --spring.profiles.active=desktop
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
