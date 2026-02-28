# 切换到独立模式任务清单

## 任务列表

- [x] 1. 修改 pom.xml，将默认 profile 从 desktop 改为 standalone
- [x] 2. 更新 application-override.yml，添加 MySQL 数据源配置
- [ ] 3. 准备 MySQL 数据库（创建 dataease10 数据库）
- [ ] 4. 重新构建后端项目
- [ ] 5. 启动测试

## 变更说明

### 1. 修改构建配置
将 pom.xml 中的默认激活 profile 从 `desktop` 改为 `standalone`，使应用使用 MySQL 数据库而非 H2。

### 2. 添加数据源配置
在 application-override.yml 中添加：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dataease10?autoReconnect=false&useUnicode=true&characterEncoding=UTF-8&characterSetResults=UTF-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_password
  flyway:
    enabled: true
    table: de_standalone_version
    validate-on-migrate: false
    locations: classpath:db/migration
    baseline-on-migrate: true
    out-of-order: true
```

### 3. 数据库准备
需要创建 `dataease10` 数据库，Flyway 会自动执行迁移脚本创建表结构。

## 注意事项
- 桌面模式的数据（H2）不会自动迁移到 MySQL
- 需要重新执行初始化（包括创建管理员账号）
