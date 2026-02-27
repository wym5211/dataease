# DataEase 项目结构

## 目录组织

DataEase 采用多模块 Maven 项目结构，主要目录如下：

```
e:\cursor\dataease/
├── core/                       # 核心代码库
│   ├── core-backend/           # 后端 Spring Boot 应用
│   │   ├── src/main/java/io/dataease/
│   │   │   ├── commons/        # 通用工具类、常量
│   │   │   ├── config/         # Spring 配置类
│   │   │   ├── controller/     # REST API 控制器
│   │   │   ├── service/        # 业务逻辑服务层
│   │   │   ├── dao/            # MyBatis Plus 数据访问层
│   │   │   ├── dto/            # 数据传输对象
│   │   │   └── provider/       # 数据源适配器实现 (EngineProvider)
│   │   └── src/main/resources/ # 配置文件 (application.yml, Mapper XML)
│   └── core-frontend/          # 前端 Vue 3 应用
│       ├── src/
│       │   ├── api/            # Axios API 请求定义
│       │   ├── assets/         # 静态资源 (图片, 样式)
│       │   ├── components/     # 通用 Vue 组件
│       │   ├── views/          # 页面视图 (仪表板, 图表编辑器)
│       │   ├── store/          # Pinia 状态管理
│       │   ├── router/         # Vue Router 路由配置
│       │   └── utils/          # 前端工具函数
│       ├── vite.config.ts      # Vite 构建配置
│       └── package.json        # 前端依赖配置
├── installer/                  # 安装与部署脚本
│   ├── dectl                   # 控制脚本
│   ├── install.sh              # 安装脚本
│   └── docker-compose.yml      # Docker 编排文件
├── mapFiles/                   # 地图 GeoJSON 数据文件
├── docs/                       # 项目文档
└── pom.xml                     # 父级 Maven 配置
```

## 命名约定

### 后端 (Java)
- **类名**：`PascalCase` (e.g., `UserService`, `ChartController`)。
- **方法/变量**：`camelCase` (e.g., `getUserById`, `chartData`)。
- **常量**：`UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_COUNT`)。
- **包名**：全小写，反向域名 (e.g., `io.dataease.service`)。
- **接口实现**：通常以 `Impl` 结尾，或者使用具体的描述性名称。

### 前端 (Vue/TS)
- **组件文件**：`PascalCase` (e.g., `ChartEditor.vue`, `DashboardPanel.vue`)。
- **目录**：`kebab-case` (e.g., `chart-editor`, `data-source`)。
- **TS 类/接口**：`PascalCase` (e.g., `IChartOption`, `User`)。
- **变量/函数**：`camelCase` (e.g., `fetchData`, `handleClick`)。
- **常量**：`UPPER_SNAKE_CASE`。

## 导入模式

### 后端
- 标准 Java 导入顺序。
- 避免使用 `.*` 通配符导入。
- 使用 Lombok 注解 (`@Data`, `@Slf4j`) 简化代码。

### 前端
- 使用 `@/` 别名指向 `src/` 目录。
- 优先导入类型定义 (`import type { ... }`)。
- 组件导入通常放在 `<script setup>` 顶部。

## 代码结构模式

### 后端服务 (Service)
```java
@Service
public class XxxService {
    @Resource
    private XxxMapper xxxMapper;

    public XxxDTO method(XxxRequest request) {
        // 1. 验证输入
        // 2. 业务逻辑处理
        // 3. 调用 DAO 层
        // 4. 返回 DTO
    }
}
```

### 前端组件 (Vue 3 Composition API)
```vue
<template>
  <div class="component-container">...</div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useStore } from '@/store';

// 1. Props & Emits 定义
const props = defineProps<{...}>();
const emit = defineEmits([...]);

// 2. 响应式状态
const data = ref([]);

// 3. 业务逻辑与方法
const loadData = async () => { ... };

// 4. 生命周期钩子
onMounted(() => { loadData(); });
</script>

<style scoped>
/* 样式定义 */
</style>
```

## 模块边界

- **Backend vs Frontend**: 通过 REST API (`/api/v1/...`) 进行通信。前端不直接访问数据库。
- **Core vs Plugins**: DataEase 支持插件机制（如数据源插件），插件通常作为独立的 Jar 包加载，通过定义的接口 (`Provider`) 与核心交互。
- **Service vs DAO**: Service 层封装业务逻辑，DAO 层仅负责数据库 CRUD。Service 层不应返回 PO (Persistent Object) 给 Controller，应转换为 DTO/VO。

## 代码大小指南

- **类/文件大小**：尽量控制在 500 行以内。对于复杂的类（如 `ChartController`），考虑拆分为多个 Helper 或 Service。
- **方法大小**：尽量控制在 50 行以内，保持逻辑清晰。
- **Vue 组件**：如果 `<template>` 超过 300 行，考虑拆分为子组件。

## 仪表板/监控结构

- **前端路由**：`/panel/index` 是仪表板的主要入口。
- **状态管理**：`store/modules/panel` 存储当前仪表板的全局状态（组件列表、样式配置）。
- **组件库**：`views/chart/components` 包含了各种图表的具体实现逻辑。
