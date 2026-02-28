# 用户设置功能设计文档

**日期：** 2026-03-01
**设计者：** Claude Code
**状态：** 已批准

---

## 概述

为 DataEase 系统添加一个综合的用户设置页面，允许用户管理个人信息、安全设置、个人偏好，并查看分组信息。该页面将集成到用户下拉菜单中，采用侧边标签布局，提供友好的用户体验。

---

## 需求背景

### 当前问题
- 系统缺少统一的用户设置入口
- 用户无法方便地查看和修改个人信息
- 设置功能分散在多个页面（如修改密码单独页面）

### 目标
- 提供统一的用户设置中心
- 整合个人信息、安全设置、偏好设置等功能
- 显示用户分组和角色信息
- 提供清晰直观的侧边标签布局

---

## 功能设计

### 功能范围

**1. 个人信息**
- 查看用户基本信息（用户名、邮箱、手机号）
- 编辑可修改的字段
- 显示只读信息（如用户ID）

**2. 安全设置**
- 修改密码
- 复用现有的密码修改组件
- 原密码验证

**3. 偏好设置**
- 界面主题选择（如适用）
- 语言设置
- 通知偏好（后续扩展）

**4. 分组信息**
- 显示用户所属分组
- 显示用户角色
- 只读展示

### 非功能需求
- 仅登录用户可访问
- 响应式设计（最小宽度 1000px）
- 支持国际化（中英文）
- 与现有系统风格保持一致

---

## 架构设计

### 页面结构

```
用户设置页面 (/user-setting)
├── 布局容器
│   ├── 侧边标签栏 (200px)
│   │   ├── 个人信息
│   │   ├── 安全设置
│   │   ├── 偏好设置
│   │   └── 分组信息
│   └── 内容区域 (864px)
│       └── [动态加载的组件]
```

### 组件划分

**主页面组件**
- 文件：`views/system/user-setting/index.vue`
- 职责：页面容器、标签切换状态管理
- 依赖：Sidebar、各功能组件

**侧边栏组件**
- 文件：`views/system/user-setting/components/Sidebar.vue`
- 职责：显示设置分类标签、处理标签切换
- Props：`activeTab`（当前激活标签）
- Emits：`tab-change`（标签切换事件）

**个人信息组件**
- 文件：`views/system/user-setting/components/PersonalInfo.vue`
- 职责：显示和编辑用户基本信息
- 状态：查看模式 / 编辑模式

**安全设置组件**
- 文件：`views/system/user-setting/components/SecuritySettings.vue`
- 职责：密码修改功能
- 复用：`modify-pwd/UpdatePwd.vue`

**偏好设置组件**
- 文件：`views/system/user-setting/components/Preferences.vue`
- 职责：主题、语言等偏好设置
- 后续扩展点

**分组信息组件**
- 文件：`views/system/user-setting/components/GroupInfo.vue`
- 职责：显示用户分组和角色
- 只读组件

### 数据流

```
用户进入页面
    ↓
从 useUserStore 获取当前用户信息
    ↓
根据 activeTab 显示对应组件
    ↓
组件内部处理各自的业务逻辑
    ↓
保存时调用后端 API（后续实现）
    ↓
显示成功/失败提示
    ↓
刷新用户信息（如需要）
```

---

## 路由配置

### 路由定义

在 `core/core-frontend/src/router/index.ts` 中添加：

```typescript
{
  path: '/user-setting',
  name: 'user-setting',
  component: () => import('@/layout/index.vue'),
  hidden: true,
  meta: {},
  children: [
    {
      path: 'index',
      name: 'us-index',
      component: () => import('@/views/system/user-setting/index.vue'),
      meta: {
        title: '用户设置',
        hidden: true
      }
    }
  ]
}
```

### 菜单集成

在 `layout/components/AccountOperator.vue` 中：

**位置：** 第 99 行之后（修改密码和系统设置之间）

```typescript
linkLoaded([{ id: 3, link: '/user-setting/index', label: t('user.user_setting') }])
```

**菜单优先级：**
- id=2: 修改密码
- id=3: 用户设置（新增）
- id=4: 系统设置
- id=5: 关于

---

## API 接口设计

### 现有接口调查

需要检查以下接口是否已存在：
1. 用户信息查询接口
2. 密码修改接口（`modify-pwd` 已使用）
3. 分组/角色查询接口

### 新增接口（如需要）

**获取用户详细信息**
```
GET /api/user/currentUserInfo
Response: {
  id: string,
  name: string,
  email: string,
  phone: string,
  groups: Array<{id, name}>,
  roles: Array<{id, name}>
}
```

**更新用户信息**
```
PUT /api/user/updateUserInfo
Request: {
  name?: string,
  email?: string,
  phone?: string
}
Response: { success: boolean }
```

**更新用户偏好**
```
PUT /api/user/updatePreferences
Request: {
  theme?: string,
  language?: string,
  notifications?: object
}
Response: { success: boolean }
```

---

## 样式设计

### 布局参考

参考页面：`views/system/modify-pwd/index.vue`

### 尺寸规范

- **总容器宽度：** 1084px
- **侧边栏宽度：** 200px
- **内容区域宽度：** 864px
- **外边距：** 16px（与 layout padding 一致）

### 颜色规范

- **背景色：** `#f5f6f7`（与 layout 一致）
- **卡片背景：** `#ffffff`
- **主色调：** 使用 Element Plus Primary 色
- **文字颜色：**
  - 标题：`#1f2329`
  - 正文：`#646a73`
  - 辅助文字：`#8f959e`

### 交互状态

- **标签激活：** 蓝色背景 `rgba(51, 112, 255, 0.2)`
- **标签悬停：** 浅灰背景 `#1f23291a`
- **按钮悬停：** 对应主题色

---

## 国际化

### 需要添加的翻译键

**中文 (`zh-CN`):**
```json
{
  "user": {
    "user_setting": "用户设置",
    "personal_info": "个人信息",
    "security_settings": "安全设置",
    "preferences": "偏好设置",
    "group_info": "分组信息",
    "edit": "编辑",
    "save": "保存",
    "cancel": "取消",
    "username": "用户名",
    "email": "邮箱",
    "phone": "手机号",
    "user_id": "用户ID",
    "groups": "所属分组",
    "roles": "角色"
  }
}
```

**英文 (`en-US`):**
```json
{
  "user": {
    "user_setting": "User Settings",
    "personal_info": "Personal Info",
    "security_settings": "Security",
    "preferences": "Preferences",
    "group_info": "Group Info",
    "edit": "Edit",
    "save": "Save",
    "cancel": "Cancel",
    "username": "Username",
    "email": "Email",
    "phone": "Phone",
    "user_id": "User ID",
    "groups": "Groups",
    "roles": "Roles"
  }
}
```

---

## 实施计划

### 阶段一：前端框架搭建
1. ✅ 设计方案编写并批准
2. ✅ 创建页面和组件文件结构
3. ✅ 实现主页面布局和侧边栏
4. ✅ 实现标签切换逻辑
5. ✅ 添加路由配置
6. ✅ 添加菜单入口
7. ✅ 添加国际化文本

### 阶段二：功能实现
1. ✅ 实现个人信息组件（UI + 数据展示）
2. ✅ 实现安全设置组件（复用密码修改）
3. ✅ 实现偏好设置组件（UI框架）
4. ✅ 实现分组信息组件（只读展示）
5. ✅ 添加表单验证规则

### 阶段三：API 集成
1. ⏳ 调查现有接口
2. ⏳ 创建新接口（如需要）
3. ⏳ 实现数据获取和保存逻辑
4. ⏳ 添加加载状态
5. ⏳ 添加错误处理

### 阶段四：测试与优化
1. ✅ 功能测试
2. ✅ 样式细节调整
3. ✅ 国际化验证
4. ⏳ 浏览器兼容性测试（待人工测试）

---

## 技术栈

- **前端框架：** Vue 3 (Composition API)
- **UI 库：** Element Plus
- **路由：** Vue Router 4
- **状态管理：** Pinia (useUserStore)
- **构建工具：** Vite
- **语言：** TypeScript

---

## 设计原则

### KISS（简单至上）
- 使用现有的布局模式，不过度设计
- 组件职责单一，逻辑清晰

### DRY（杜绝重复）
- 复用现有的密码修改组件
- 复用现有的布局样式

### YAGNI（精益求精）
- 暂不实现过于复杂的偏好设置
- 偏好设置组件先做 UI 框架，后续扩展

### SOLID
- **单一职责：** 每个组件只负责一个功能模块
- **开闭原则：** 通过组件化支持未来扩展
- **依赖倒置：** 依赖 Pinia store 抽象数据层

---

## 风险与缓解

### 风险

1. **后端接口缺失**
   - 风险：可能需要新建多个接口
   - 缓解：优先调查现有接口，分阶段实现

2. **用户分组信息获取复杂**
   - 风险：权限模块可能有复杂的数据结构
   - 缓解：先做简单展示，后续优化

3. **样式一致性**
   - 风险：可能与系统其他页面风格不统一
   - 缓解：严格参考现有页面样式

---

## 后续优化方向

1. **功能增强**
   - 添加头像上传
   - 添加两步验证
   - 添加登录历史查看

2. **用户体验**
   - 添加设置搜索功能
   - 添加设置快捷入口
   - 优化移动端体验（独立项目）

3. **性能优化**
   - 组件懒加载
   - 数据缓存优化

---

## 附录

### 参考文件

- `core/core-frontend/src/views/system/modify-pwd/index.vue` - 布局参考
- `core/core-frontend/src/layout/components/AccountOperator.vue` - 菜单集成
- `core/core-frontend/src/router/index.ts` - 路由配置

### 相关文档

- 项目 CLAUDE.md
- Vue 3 官方文档
- Element Plus 官方文档

---

## 使用说明

### 访问入口

用户设置页面已集成到系统用户菜单中，访问路径如下：

1. **通过菜单访问**：
   - 点击右上角用户头像
   - 在下拉菜单中选择"用户设置"
   - 进入用户设置页面

2. **直接 URL 访问**：
   - 开发环境：`http://localhost:8081/#/user-setting/index`
   - 生产环境：`http://your-domain/#/user-setting/index`

### 功能使用指南

#### 1. 个人信息

**功能说明：**
- 查看和编辑用户基本信息
- 支持修改的字段：昵称、邮箱、手机号、性别、部门、职位、语言
- 用户名为只读字段，不可修改

**操作步骤：**
1. 进入"个人信息"标签页
2. 点击右上角"编辑"按钮
3. 修改需要更新的字段
4. 点击"保存"按钮提交更改
5. 或点击"取消"按钮放弃修改

**表单验证规则：**
- 昵称：必填，长度 2-50 字符
- 邮箱：可选，需符合邮箱格式（如：user@example.com）
- 手机号：可选，需为 1 开头的 11 位数字

**注意事项：**
- 当前版本数据为模拟数据，保存操作仅在前端模拟
- 后续版本将接入后端 API 实现真实数据保存

#### 2. 安全设置

**功能说明：**
- 修改登录密码
- 复用现有的密码修改组件
- 需要输入原密码进行验证

**操作步骤：**
1. 进入"安全设置"标签页
2. 输入原密码
3. 输入新密码
4. 确认新密码
5. 点击"确定"按钮提交

**密码要求：**
- 长度至少 6 位
- 不能与原密码相同
- 新密码和确认密码必须一致

#### 3. 偏好设置

**功能说明：**
- 当前版本为占位页面
- 后续将支持主题、语言、通知等偏好设置
- 显示"偏好设置功能开发中..."提示

**后续计划：**
- 界面主题切换（浅色/深色）
- 语言偏好设置
- 通知偏好管理
- 界面布局设置

#### 4. 分组信息

**功能说明：**
- 查看用户所属分组
- 查看用户角色
- 只读展示，不可修改

**显示信息：**
- 角色：用户在系统中的角色（如：管理员）
- 分组：用户所属的组织分组

**注意事项：**
- 当前版本显示模拟数据
- 后续版本将从权限模块获取真实数据

### 界面特性

#### 响应式设计
- 推荐最小分辨率：1000px
- 最佳分辨率：1920x1080 或更高
- 低于最小分辨率时会出现横向滚动条

#### 标签切换
- 点击左侧标签切换不同功能模块
- 当前激活标签以蓝色背景高亮显示
- 支持键盘 Tab 键导航

#### 国际化支持
- 支持中文（简体）和英文
- 切换系统语言后页面内容即时更新
- 所有提示信息、标签、按钮均支持国际化

### 快捷键

- `Tab`：在表单字段间切换
- `Enter`：提交表单（在密码输入框中）
- `Esc`：取消编辑模式

### 浏览器兼容性

**推荐浏览器：**
- Chrome 120+
- Firefox 120+
- Edge 120+

**不支持：**
- IE 11 及更早版本

### 性能优化

- 组件采用懒加载，提升首屏加载速度
- 静态资源经过压缩和优化
- 使用 Vue 3 Composition API 提升性能

### 常见问题

**Q1: 为什么保存个人信息后刷新页面数据又恢复了？**
A: 当前版本使用模拟数据，数据未持久化到后端。后续版本将接入后端 API 实现真实数据保存。

**Q2: 偏好设置为什么显示"开发中"？**
A: 偏好设置功能将在后续版本中逐步实现，当前版本仅提供 UI 框架。

**Q3: 如何修改用户名？**
A: 用户名为系统唯一标识，当前版本不支持修改。如需修改，请联系系统管理员。

**Q4: 分组信息显示不正确怎么办？**
A: 当前版本显示模拟数据，请等待后续版本接入权限模块 API。

### 技术支持

如有问题或建议，请通过以下方式反馈：
- GitHub Issues: [DataEase Issues](https://github.com/dataease/dataease/issues)
- 官方论坛: [DataEase Forum](https://bbs.fit2cloud.com/)

---

**文档版本：** 1.1
**最后更新：** 2026-03-01
**实施状态：** 前端基础功能已完成，待后端 API 集成
