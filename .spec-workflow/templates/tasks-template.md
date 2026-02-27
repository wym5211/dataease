# DataEase 任务文档模板

- [ ] 1. 创建后端数据模型 (Java)
  - 文件：`core/core-backend/src/main/java/io/dataease/dto/entity/[Feature]Entity.java`
  - 定义 MyBatis Plus 实体类
  - 目的：映射数据库表结构
  - _利用：`io.dataease.commons.model.BaseEntity`_
  - _提示：角色：Java Spring Boot 开发人员 | 任务：创建 [Feature] 的实体类，使用 Lombok 注解，继承 BaseEntity，映射数据库字段 | 限制：遵循 MyBatis Plus 规范，添加必要的 Swagger 注解_

- [ ] 2. 创建后端 Mapper 接口
  - 文件：`core/core-backend/src/main/java/io/dataease/dao/[Feature]Mapper.java`
  - 定义数据访问接口
  - 目的：提供 CRUD 操作
  - _利用：`com.baomidou.mybatisplus.core.mapper.BaseMapper`_
  - _提示：角色：Java 后端开发人员 | 任务：创建 Mapper 接口，继承 BaseMapper<[Feature]Entity> | 限制：如果需要复杂查询，在 XML 中编写 SQL_

- [ ] 3. 实现后端服务逻辑 (Service)
  - 文件：`core/core-backend/src/main/java/io/dataease/service/[Feature]Service.java`
  - 实现业务逻辑，处理事务
  - 目的：封装业务规则
  - _利用：`io.dataease.commons.exception.DEException`_
  - _提示：角色：Java 后端开发人员 | 任务：创建 Service 类，注入 Mapper，实现业务方法 | 限制：使用 `@Transactional` 处理事务，抛出自定义异常 DEException，参数校验_

- [ ] 4. 创建后端控制器 (Controller)
  - 文件：`core/core-backend/src/main/java/io/dataease/controller/[Feature]Controller.java`
  - 定义 REST API 接口
  - 目的：暴露服务给前端
  - _利用：`io.swagger.v3.oas.annotations.Operation`_
  - _提示：角色：Java 后端开发人员 | 任务：创建 Controller，定义 RESTful 端点，使用 `@RestController` 和 `@RequestMapping` | 限制：添加 Swagger 文档注解，统一返回 `ResultHolder` 格式_

- [ ] 5. 定义前端 API 接口 (TypeScript)
  - 文件：`core/core-frontend/src/api/[feature].ts`
  - 定义 Axios 请求函数
  - 目的：封装后端调用
  - _利用：`core/core-frontend/src/utils/request.ts`_
  - _提示：角色：Vue 前端开发人员 | 任务：编写 API 请求函数，定义 Request/Response 类型接口 | 限制：使用项目封装的 `request` 实例，处理 URL 前缀_

- [ ] 6. 创建前端 Store (Pinia)
  - 文件：`core/core-frontend/src/store/modules/[feature].ts`
  - 定义状态管理
  - 目的：管理功能相关的全局状态
  - _利用：`pinia`_
  - _提示：角色：Vue 前端开发人员 | 任务：创建 Pinia Store，定义 state, getters, actions | 限制：使用 Composition API 风格 (`defineStore`)_

- [ ] 7. 实现前端组件 (Vue 3)
  - 文件：`core/core-frontend/src/views/[feature]/index.vue`
  - 实现 UI 界面和交互
  - 目的：用户操作界面
  - _利用：`Element Plus`, `src/components/`_
  - _提示：角色：Vue 前端开发人员 | 任务：编写 Vue 组件，使用 `<script setup lang="ts">`，调用 Store 和 API | 限制：遵循项目 UI 规范，使用 Element Plus 组件，处理加载状态和错误提示_

- [ ] 8. 编写单元测试 (可选)
  - 文件：`core/core-backend/src/test/java/io/dataease/...`
  - 编写后端 Service 测试
  - 目的：验证业务逻辑
  - _利用：`JUnit 5`, `Mockito`_
  - _提示：角色：QA 工程师 | 任务：为关键业务逻辑编写单元测试 | 限制：Mock 数据库依赖，测试边界条件_
