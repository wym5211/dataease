# DataEase 社区版与企业版（X-Pack）判别逻辑汇总

本文档列出了 DataEase 前后端代码中所有用于区分社区版（Community/Desktop）与企业版（Enterprise/X-Pack/Distributed）的判断逻辑及其功能说明。

## 1. 后端 (Java)

后端主要通过 `ModelUtils.isDesktop()` 判断运行模式，以及通过 `@XpackInteract` 注解实现功能替换。

| 文件路径 (相对 core-backend) | 代码/机制 | 功能说明 |
| :--- | :--- | :--- |
| `src/main/java/io/dataease/utils/ModelUtils.java` | `isDesktop()` | **核心判断方法**。通过 Spring Profile (`spring.profiles.active`) 判断是否为 `desktop` 模式（通常对应社区版/单机版）。 |
| `src/main/java/io/dataease/auth/filter/TokenFilter.java` | `ModelUtils.isDesktop()` | **鉴权放行**。如果是桌面模式，直接设置默认桌面用户，跳过后续 Token 校验。 |
| `src/main/java/io/dataease/auth/filter/CommunityTokenFilter.java` | `ModelUtils.isDesktop() \|\| !LicenseUtil.licenseValid()` | **企业鉴权跳过**。如果是桌面模式或 License 无效（社区版默认无效），则跳过企业版的高级 Token 校验逻辑。 |
| `src/main/java/io/dataease/permissions/utils/PermissionUtils.java` | `ModelUtils.isDesktop()` | **权限校验放行**。在 `hasPermissions`, `hasRoles`, `hasUrlPermission`, `checkAuthentication` 等方法中，如果是桌面模式，直接返回 `true`，即不进行权限控制。 |
| `src/main/java/io/dataease/datasource/manage/EngineManage.java` | `ModelUtils.isDesktop()` | **默认引擎选择**。初始化时，桌面模式默认使用 H2 数据库，非桌面模式（服务器/企业版）默认使用 MySQL。 |
| `src/main/java/io/dataease/constant/StaticResourceConstants.java` | `ModelUtils.isDesktop()` | **数据目录路径**。桌面模式使用配置文件中的 `dataease.path.data`，非桌面模式使用默认 `/opt/dataease2.0/data`。 |
| `src/main/java/io/dataease/datasource/provider/ExcelUtils.java` | `ModelUtils.isDesktop()` | **Excel 路径**。同上，区分 Excel 文件存储路径。 |
| `src/main/java/io/dataease/visualization/utils/VisualizationExcelUtils.java` | `ModelUtils.isDesktop()` | **报表路径**。同上，区分报表导出文件的存储路径。 |
| `src/main/java/io/dataease/visualization/server/VisualizationLinkJumpService.java` | `ModelUtils.isDesktop()` | **跳转信息查询**。在查询仪表板跳转信息时，将此标志传递给 Mapper，可能用于过滤不同模式下的数据可见性。 |
| `src/main/java/io/dataease/visualization/manage/CoreVisualizationManage.java` | `!ModelUtils.isDesktop()` | **删除关联资源**。在删除可视化资源时，如果不是桌面模式，会额外尝试删除 X-Pack 相关的分享资源 (`xpackShareManage`)。 |
| `src/main/java/io/dataease/visualization/manage/CoreVisualizationManage.java` | `@XpackInteract` | **功能增强/替换**。在资源树查询 (`tree`)、删除、移动、保存等方法上使用。若企业版模块加载，将执行企业版逻辑（如权限过滤、审批流等）。 |
| `src/main/java/io/dataease/home/manage/DeIndexManage.java` | `@XpackInteract` | **模式查询接口**。`xpackModel()` 方法默认返回 `null`，企业版会替换此方法返回 `true`/`false` 以告知前端是否开启了 X-Pack 功能。 |
| `src/main/java/io/dataease/system/manage/CorePermissionManage.java` | `@XpackInteract` | **权限检查替换**。`checkAuth` 方法。企业版可能有更复杂的行列权限或细粒度控制，通过此注解替换社区版的简单逻辑。 |
| `src/main/java/io/dataease/system/manage/CoreUserManage.java` | `@XpackInteract` | **用户名称获取**。`getUserName` 方法。社区版可能只返回简单名称，企业版可能涉及组织架构信息。 |
| `src/main/java/io/dataease/menu/manage/MenuManage.java` | `@XpackInteract` | **菜单查询**。`query` 方法。企业版会根据 License 和权限动态过滤或添加菜单项。 |
| `src/main/java/io/dataease/home/RestIndexController.java` | `ModelUtils.isDesktop()` | **前端接口**。`/model` 接口直接返回当前是否为桌面模式，供前端缓存使用。 |

## 2. 前端 (Vue/TypeScript)

前端主要通过缓存中的标志位 (`app.desktop`) 和动态组件加载机制 (`XpackComponent`) 来区分版本。

| 文件路径 (相对 core-frontend) | 代码/机制 | 功能说明 |
| :--- | :--- | :--- |
| `src/utils/ModelUtil.ts` | `wsCache.get('app.desktop')` | **核心判断方法**。`isDesktop()` 工具函数，从浏览器缓存中读取后端返回的运行模式。 |
| `src/views/login/index.vue` | `XpackComponent` | **登录增强**。使用 `<XpackComponent>` 动态加载登录处理器（如 LDAP、CAS、OIDC 等企业版登录方式）和密码验证逻辑。 |
| `src/layout/components/AccountOperator.vue` | `wsCache.get('app.desktop')` | **菜单隐藏**。如果是桌面模式，隐藏顶部导航栏用户头像下的“修改密码”和“用户设置”入口（桌面模式通常单用户或简化管理）。 |
| `src/views/system/parameter/index.vue` | `isDesktop()` | **Tab 隐藏**。如果是桌面模式，隐藏“第三方嵌入 (Third Party Embed)”设置页签。 |
| `src/views/about/index.vue` | `result?.license?.edition` | **版本展示**。在“关于”弹窗中，根据后端返回的 License 信息显示“标准版”、“专业版”、“企业版”或“嵌入版”。 |
| `src/views/about/index.vue` | `back2Community` | **版本回退**。提供“返回社区版”功能，允许用户在 License 过期或需要时切回社区版模式。 |
| `src/components/plugin/src/index.vue` | `xpackModelApi()` | **企业版检测**。在组件挂载时调用后端接口检查是否为分布式/企业模式 (`distributed`)，并据此决定是否加载 X-Pack 插件资源。 |
| `src/components/visualization/DePreviewPopDialog.vue` | `XpackComponent` | **预览增强**。使用 `<XpackComponent>` 加载 `IframeSelf`，可能用于企业版的高级嵌入或预览功能。 |
| `src/components/plugin/src/PluginComponent.vue` | `window['DEXPack']` | **插件加载**。检查全局 `DEXPack` 对象，如果存在则加载对应的企业版组件实现。 |

## 总结

- **Community/Desktop (社区版/桌面模式)**: 
    - 特征：`ModelUtils.isDesktop() == true`。
    - 行为：使用 H2 数据库，文件路径使用本地配置，跳过几乎所有权限和 Token 校验，UI 上隐藏部分高级设置和用户管理功能。
- **Enterprise/Distributed (企业版/分布式模式)**: 
    - 特征：`ModelUtils.isDesktop() == false` 且 License 有效，通常配合 `@XpackInteract` 加载额外 Jar 包。
    - 行为：使用 MySQL，严格的权限控制 (RBAC/行列权限)，动态加载 X-Pack 前端组件，支持复杂的登录方式和分享功能。
