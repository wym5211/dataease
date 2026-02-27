# DataEase 技术栈

## 项目类型
DataEase 是一个**模块化单体 (Modular Monolith)** 架构的 Web 应用程序，包含后端服务和前端 SPA 应用。支持 Docker 容器化部署和 Kubernetes 集群部署。

## 核心技术

### 主要语言
- **后端语言**：Java 21
- **后端框架**：Spring Boot 3.3.13, Spring Cloud Alibaba 2023.0.1
- **前端语言**：TypeScript 4.9.3, Vue 3.3.4
- **构建工具**：Maven (后端), Vite 4.1.3 (前端)

### 关键依赖/库

#### 后端
- **MyBatis Plus 3.5.6**：ORM 框架，用于数据访问。
- **Apache Calcite**：SQL 解析与优化，用于多数据源适配。
- **Quartz**：任务调度框架，用于数据同步和定时任务。
- **Spring Security**：安全认证与授权。
- **Apache APISIX**：API 网关（部署时使用）。

#### 前端
- **Pinia 2.0.32**：状态管理库。
- **Vue Router 4.1.3**：路由管理。
- **Element Plus**：PC 端 UI 组件库。
- **Vant 4.8.3**：移动端 UI 组件库。
- **ECharts 5.5.1 / AntV G2Plot**：可视化图表库。
- **Axios**：HTTP 客户端。

### 应用程序架构
DataEase 采用前后端分离的开发模式，但在部署时通常打包在一起或通过反向代理协同工作。
- **后端**：`core-backend` 模块包含核心业务逻辑，通过 Maven Profile 区分 `standalone`（单机）和 `distributed`（分布式）模式。
- **前端**：`core-frontend` 模块构建为静态资源，由 Nginx 或后端服务提供服务。
- **数据层**：
    - **元数据存储**：MySQL 8.0（存储用户、仪表板配置等）。
    - **计算引擎**：Apache Doris / ClickHouse（用于加速大数据量查询）。
    - **缓存**：Redis（分布式缓存）+ Ehcache（本地缓存）。

### 数据存储
- **主要存储**：MySQL 8.2.0 (InnoDB 引擎)。
- **分析存储**：支持对接 Doris, ClickHouse, StarRocks 等 OLAP 数据库。
- **缓存**：Redis 7.x, Ehcache。
- **数据格式**：JSON (API 交互), GeoJSON (地图数据)。

### 外部集成
- **API**：RESTful API，遵循 OpenAPI 3 标准。
- **认证**：支持 OIDC, LDAP, CAS, JWT。
- **消息队列**：虽然核心依赖较少，但在分布式部署中可能涉及。

## 开发环境

### 构建和开发工具
- **IDE**：IntelliJ IDEA (后端), VS Code / Cursor (前端)。
- **后端构建**：`mvn clean package`。
- **前端构建**：`npm install && npm run build`。
- **运行环境**：JDK 21, Node.js 18+, Docker。

### 代码质量工具
- **后端**：Checkstyle, SpotBugs, JUnit 5 (单元测试)。
- **前端**：ESLint, Prettier, Vue Test Utils。
- **文档**：SpringDoc (Swagger), Markdown。

### 版本控制和协作
- **VCS**：Git。
- **分支策略**：Github Flow (main 分支稳定，feature 分支开发，PR 合并)。
- **代码审查**：通过 GitHub PR 进行 Code Review。

## 部署和分发
- **目标平台**：Linux (CentOS, Ubuntu), Windows (仅开发), Kubernetes。
- **分发方式**：Docker 镜像, 离线安装包 (Shell 脚本)。
- **安装要求**：8核 16G 内存以上推荐（生产环境）。
- **更新机制**：通过 `dectl` 命令行工具或 Helm Chart 升级。

## 技术要求和约束

### 性能要求
- **查询响应**：亿级数据聚合查询 < 3秒（依赖 OLAP 引擎）。
- **并发用户**：支持 100+ 并发在线编辑。
- **导出性能**：支持百万行级 Excel 导出（异步任务）。

### 兼容性要求
- **浏览器**：Chrome 80+, Firefox, Edge, Safari。
- **数据库**：兼容主流 JDBC 数据源。

### 安全性和合规性
- **安全要求**：全站 HTTPS，敏感数据加密存储（AES），SQL 注入防护。
- **权限控制**：基于 RBAC 的功能权限 + 行/列级数据权限。

## 已知限制
- **单体瓶颈**：在极高并发下，单一后端实例可能成为瓶颈（正在向微服务演进）。
- **内存消耗**：Java 进程和 OLAP 引擎（如 Doris）对内存要求较高。
