# 用户管理功能设计文档

**日期：** 2026-03-01
**设计者：** Claude Code
**状态：** 已批准

---

## 概述

在 DataEase 权限管理模块中添加用户管理功能，允许管理员查看用户列表、创建新用户、编辑用户信息、删除用户、重置用户密码，以及分配用户角色和分组。该功能采用表格+弹窗模式，提供高效的用户管理体验。

---

## 需求背景

### 当前问题
- 系统缺少统一的用户管理界面
- 管理员无法方便地创建和管理用户
- 用户角色和分组分配需要在数据库层面操作

### 目标
- 提供完整的用户管理界面
- 支持用户的增删改查操作
- 支持角色和分组分配
- 提供搜索和筛选功能
- 确保操作安全性和数据完整性

---

## 功能设计

### 功能范围

**1. 用户列表**
- 分页展示所有用户
- 显示用户名、昵称、邮箱、手机号、角色、分组、状态
- 支持按用户名、昵称、邮箱搜索
- 支持按状态筛选（全部/启用/禁用）
- 支持按角色筛选

**2. 创建用户**
- 填写用户基本信息（用户名、昵称、邮箱、手机号、密码）
- 分配角色（多选）
- 分配分组（多选）
- 设置用户状态（启用/禁用）
- 表单验证（必填、格式、唯一性）

**3. 编辑用户**
- 修改用户信息（昵称、邮箱、手机号、角色、分组、状态）
- 不能修改用户名
- 表单验证

**4. 删除用户**
- 硬删除（从数据库中删除）
- 二次确认对话框
- 安全检查：不能删除当前登录用户
- 安全检查：不能删除admin用户

**5. 重置密码**
- 为用户设置新密码
- 二次确认对话框

### 非功能需求
- 仅管理员角色可访问
- 响应式设计
- 支持国际化（中英文）
- 与现有权限管理模块风格保持一致

---

## 架构设计

### 路由配置

在权限管理模块下添加用户管理路由：

```typescript
{
  path: 'user',
  name: 'permissions-user',
  component: () => import('@/views/permissions/user/index.vue'),
  meta: {
    title: '用户管理',
    roles: ['admin']
  }
}
```

完整路径：`/permissions/user`

### 组件结构

```
views/permissions/user/
├── index.vue                    # 主页面容器
│   ├── components/
│   │   ├── UserTable.vue       # 用户列表表格
│   │   ├── SearchBar.vue       # 搜索和筛选栏
│   │   └── UserDialog.vue      # 创建/编辑用户弹窗
│   └── api/
│       └── index.ts            # API接口定义
```

**组件职责：**

**index.vue（主页面）**
- 状态管理：列表数据、分页、搜索条件
- 事件处理：搜索、筛选、创建、编辑、删除、重置密码
- 弹窗控制：显示/隐藏创建/编辑对话框

**UserTable.vue（表格组件）**
- Props：用户列表数据、loading状态
- Emits：编辑、删除、重置密码事件
- 展示用户列表、分页控件

**SearchBar.vue（搜索栏组件）**
- Props：角色选项列表
- Emits：搜索、状态筛选、角色筛选事件
- 输入框、下拉选择器

**UserDialog.vue（对话框组件）**
- Props：对话框模式（创建/编辑）、用户数据、角色/分组选项
- Emits：确认、取消事件
- 表单验证和提交

### 页面布局

```
┌─────────────────────────────────────────────────┐
│ 权限管理 > 用户管理                              │
├─────────────────────────────────────────────────┤
│ [搜索框________] [状态▼] [角色▼]  [+ 创建用户]  │
├─────────────────────────────────────────────────┤
│ ┌─────┬──────┬──────┬─────┬──────┬──────┐       │
│ │用户名│昵称  │邮箱  │...  │状态  │操作  │       │
│ ├─────┼──────┼──────┼─────┼──────┼──────┤       │
│ │admin│管理员│...  │...  │启用  │编辑  │       │
│ │     │      │      │     │      │删除  │       │
│ │     │      │      │     │      │重置  │       │
│ └─────┴──────┴──────┴─────┴──────┴──────┘       │
│                    [分页器]                      │
└─────────────────────────────────────────────────┘
```

---

## 数据流和API设计

### API接口

**1. 获取用户列表**
```
GET /api/permissions/users/list
Query Parameters:
  - page: number (页码，从1开始)
  - pageSize: number (每页条数)
  - keyword?: string (搜索关键词，可选)
  - status?: number (状态筛选：0=禁用, 1=启用，可选)
  - roleId?: string (角色筛选，可选)

Response:
{
  code: 200,
  data: {
    records: User[],
    total: number
  },
  msg: "success"
}

User对象结构:
{
  userId: string,
  username: string,
  nickName: string,
  email: string,
  phone?: string,
  roles: Role[],
  groups: Group[],
  status: number,
  createTime: string
}
```

**2. 获取角色和分组选项**
```
GET /api/permissions/users/options

Response:
{
  code: 200,
  data: {
    roles: Role[],
    groups: Group[]
  },
  msg: "success"
}

Role对象结构:
{
  roleId: string,
  roleName: string,
  roleCode: string
}

Group对象结构:
{
  groupId: string,
  groupName: string
}
```

**3. 创建用户**
```
POST /api/permissions/users/create

Request Body:
{
  username: string,
  nickName: string,
  email: string,
  phone?: string,
  password: string,
  roleIds: string[],
  groupIds: string[],
  status: number
}

Response:
{
  code: 200,
  data: User,
  msg: "用户创建成功"
}

错误响应:
{
  code: 500,
  data: null,
  msg: "用户名已存在"
}
```

**4. 编辑用户**
```
PUT /api/permissions/users/update/{userId}

Request Body:
{
  nickName: string,
  email: string,
  phone?: string,
  roleIds: string[],
  groupIds: string[],
  status: number
}

Response:
{
  code: 200,
  data: User,
  msg: "用户信息已更新"
}
```

**5. 删除用户**
```
DELETE /api/permissions/users/delete/{userId}

Response:
{
  code: 200,
  data: null,
  msg: "用户已删除"
}

错误响应:
{
  code: 500,
  data: null,
  msg: "不能删除当前登录用户" / "不能删除系统管理员"
}
```

**6. 重置密码**
```
PUT /api/permissions/users/reset-password/{userId}

Request Body:
{
  newPassword: string
}

Response:
{
  code: 200,
  data: null,
  msg: "密码已重置"
}
```

### 数据流

```
页面加载
  ↓
并行调用：
  - /list API 获取用户列表
  - /options API 获取角色和分组选项
  ↓
用户操作
  ↓
┌─────────────┬──────────────┬──────────────┐
│ 搜索/筛选   │ 创建/编辑    │ 删除/重置密码 │
└─────────────┴──────────────┴──────────────┘
      ↓              ↓              ↓
  调用/list    调用/create    调用/delete
  或/update     或/update      或/reset-password
      ↓              ↓              ↓
  刷新列表显示  刷新列表显示  刷新列表显示
```

---

## 权限控制设计

### 访问控制

**页面级权限：**
- 路由配置 `roles: ['admin']`
- 路由守卫检查用户角色
- 非管理员跳转到401页面

**操作级权限：**
- 所有登录的管理员都可以执行全部操作
- 前端不做额外的操作权限细分

### 安全规则

**不能删除当前登录用户：**
```typescript
const handleDelete = (user: User) => {
  const currentUser = useUserStore().userInfo

  if (user.username === currentUser.username) {
    ElMessage.error('不能删除当前登录用户')
    return
  }

  // 显示删除确认对话框
  ElMessageBox.confirm('确定要删除该用户吗？此操作不可恢复', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 调用删除API
  })
}
```

**不能删除admin用户：**
```typescript
if (user.username === 'admin') {
  ElMessage.error('不能删除系统管理员账户')
  return
}
```

**后端安全检查：**
- 即使前端检查通过，后端也需要再次验证
- 防止直接调用API绕过前端检查

---

## 表单验证规则

### 前端验证（Element Plus）

**用户名：**
- 必填：`{ required: true, message: '用户名不能为空', trigger: 'blur' }`
- 长度：`{ min: 3, max: 50, message: '长度在 3 到 50 个字符', trigger: 'blur' }`
- 格式：`{ pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字、下划线', trigger: 'blur' }`

**昵称：**
- 必填：`{ required: true, message: '昵称不能为空', trigger: 'blur' }`
- 长度：`{ min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }`

**邮箱：**
- 必填：`{ required: true, message: '邮箱不能为空', trigger: 'blur' }`
- 格式：`{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }`

**手机号：**
- 可选
- 格式：`{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }`

**密码：**
- 创建时必填：`{ required: true, message: '密码不能为空', trigger: 'blur' }`
- 长度：`{ min: 6, message: '密码长度至少6位', trigger: 'blur' }`
- 编辑时可选

**角色：**
- 必选：`{ required: true, message: '请至少选择一个角色', trigger: 'change' }`

**分组：**
- 可选

### 后端验证

- 用户名唯一性
- 邮箱唯一性
- 角色ID和分组ID有效性
- 数据长度限制

---

## UI/UX设计

### 表格设计

**列定义：**
- 用户名：150px，左对齐
- 昵称：120px，左对齐
- 邮箱：200px，左对齐
- 手机号：130px，左对齐
- 角色：150px，左对齐（标签显示）
- 分组：150px，左对齐（标签显示）
- 状态：80px，居中
- 操作：180px，右对齐

**功能特性：**
- 排序：支持按用户名、创建时间排序
- 分页：每页10/20/50条，默认20条
- 空状态：无数据时显示"暂无用户"图标和文字
- Loading：数据加载时显示骨架屏

### 状态显示

**启用状态：**
```html
<el-tag type="success" size="small">启用</el-tag>
```

**禁用状态：**
```html
<el-tag type="info" size="small">禁用</el-tag>
```

### 角色和分组显示

使用标签组件展示多个角色/分组：
```html
<el-tag
  v-for="role in user.roles"
  :key="role.roleId"
  size="small"
  class="role-tag"
>
  {{ role.roleName }}
</el-tag>
```

### 搜索栏设计

```
┌────────────────────────────────────────────────┐
│ 🔍 [搜索用户名、昵称或邮箱___________] [全部▼] [角色▼多选] [+ 创建用户] │
└────────────────────────────────────────────────┘
```

- 搜索框：300px宽，占位符提示
- 状态筛选：120px宽，单选（全部/启用/禁用）
- 角色筛选：150px宽，多选
- 创建按钮：主要按钮样式，右对齐

### 对话框设计

**尺寸：** 600px宽

**表单布局：**
```html
<el-form label-width="80px">
  <el-form-item label="用户名"> <el-input /> </el-form-item>
  <el-form-item label="昵称"> <el-input /> </el-form-item>
  <el-form-item label="邮箱"> <el-input /> </el-form-item>
  <el-form-item label="手机号"> <el-input /> </el-form-item>
  <el-form-item label="密码"> <el-input type="password" /> </el-form-item>
  <el-form-item label="角色"> <el-select multiple /> </el-form-item>
  <el-form-item label="分组"> <el-select multiple /> </el-form-item>
  <el-form-item label="状态"> <el-radio-group /> </el-form-item>
</el-form>
```

**底部按钮：**
- 右对齐
- 取消（次要按钮）
- 确定（主要按钮）

### 操作按钮

**编辑：**
```html
<el-button link type="primary">编辑</el-button>
```

**删除：**
```html
<el-button link type="danger">删除</el-button>
```

**重置密码：**
```html
<el-button link type="warning">重置密码</el-button>
```

### Loading状态

**表格加载：**
```html
<el-table v-loading="loading" />
```

**提交加载：**
```html
<el-button :loading="submitting">确定</el-button>
```

### 反馈提示

**成功提示：**
- 创建：`ElMessage.success('用户创建成功')`
- 编辑：`ElMessage.success('用户信息已更新')`
- 删除：`ElMessage.success('用户已删除')`
- 重置密码：`ElMessage.success('密码已重置')`

**错误提示：**
- 网络错误：`ElMessage.error('网络请求失败，请重试')`
- 业务错误：显示后端返回的错误信息

---

## 错误处理设计

### 网络错误

**超时错误：**
```typescript
try {
  await createUser(formData)
} catch (error) {
  if (error.code === 'ECONNABORTED') {
    ElMessage.error('请求超时，请重试')
  }
}
```

**网络断开：**
```typescript
if (!navigator.onLine) {
  ElMessage.error('网络连接已断开，请检查网络')
}
```

### 业务错误

**用户名已存在：**
```typescript
if (error.msg?.includes('用户名已存在')) {
  ElMessage.error('该用户名已被使用')
}
```

**邮箱已存在：**
```typescript
if (error.msg?.includes('邮箱已存在')) {
  ElMessage.error('该邮箱已被注册')
}
```

**权限不足：**
```typescript
if (error.code === 403) {
  ElMessage.error('您没有权限执行此操作')
  // 跳转到401页面
}
```

### 边界情况

**删除最后一个管理员：**
```typescript
if (error.msg?.includes('至少需要保留一个管理员')) {
  ElMessage.error('系统至少需要保留一个管理员账户')
}
```

**角色或分组选项为空：**
```typescript
if (roleOptions.length === 0) {
  ElMessage.warning('暂无角色选项，请先创建角色')
}
```

---

## 测试策略

### 功能测试清单

- ✅ 用户列表正确显示所有用户
- ✅ 搜索功能：按用户名、昵称、邮箱搜索
- ✅ 状态筛选：全部/启用/禁用
- ✅ 角色筛选：多选角色过滤用户
- ✅ 分页功能：切换每页条数、翻页
- ✅ 创建用户：填写完整表单 → 提交成功 → 列表刷新
- ✅ 编辑用户：修改信息 → 提交成功 → 列表更新
- ✅ 删除用户：确认删除 → 用户从列表消失
- ✅ 重置密码：输入新密码 → 提交成功
- ✅ 角色多选功能正常
- ✅ 分组多选功能正常
- ✅ 表单验证：必填项、格式、长度
- ✅ 状态切换：启用/禁用

### 边界测试清单

- ✅ 不能删除当前登录用户
- ✅ 不能删除admin用户
- ✅ 用户名唯一性验证
- ✅ 邮箱唯一性验证
- ✅ 创建时密码必填，编辑时可选
- ✅ 空列表状态显示
- ✅ 角色选项为空的处理
- ✅ 分组选项为空的处理

### UI/UX测试清单

- ✅ 响应式布局正常
- ✅ Loading状态正确显示
- ✅ 成功提示正确显示
- ✅ 错误提示正确显示
- ✅ 确认对话框正确显示
- ✅ 表格排序功能正常
- ✅ 国际化文本正确（中英文）

### 权限测试清单

- ✅ 非管理员无法访问页面（跳转401）
- ✅ 管理员可以访问页面
- ✅ 管理员可以执行所有操作

### 浏览器兼容性测试

- Chrome 120+
- Firefox 120+
- Edge 120+

---

## 技术栈

**前端框架：** Vue 3 (Composition API + `<script setup>`)

**UI库：** Element Plus
- el-table：表格
- el-dialog：对话框
- el-form：表单
- el-input：输入框
- el-select：下拉选择器
- el-button：按钮
- el-tag：标签
- el-message：消息提示
- el-message-box：确认对话框
- el-pagination：分页器

**状态管理：** Pinia
- useUserStore：获取当前用户信息

**路由：** Vue Router 4

**HTTP客户端：** axios
- 使用项目现有的请求封装

**国际化：** vue-i18n
- 支持中文、英文

**表单验证：** Element Plus Form验证 + 自定义规则

**TypeScript：** 类型安全

---

## 设计原则

### KISS（简单至上）
- 复用现有的权限管理页面布局
- 使用Element Plus标准组件
- API命名清晰直观

### DRY（杜绝重复）
- 复用权限管理模块的布局组件
- 表单验证规则提取为常量
- 用户类型定义统一管理

### YAGNI（精益求精）
- 第一版实现核心功能
- 用户导入/导出功能留待后续
- 高级筛选功能留待后续
- 批量操作功能留待后续

### SOLID
- 组件职责单一
- 通过props/emits实现松耦合
- 接口清晰，易于扩展

---

## 后续优化方向

### 功能增强
- 用户导入/导出（Excel）
- 批量删除用户
- 批量分配角色
- 用户登录历史查看
- 用户活动日志

### 用户体验
- 表格列宽拖拽调整
- 表格列显示/隐藏配置
- 快捷键支持
- 更丰富的筛选条件

### 性能优化
- 虚拟滚动（大数据量）
- 列表数据缓存
- 防抖搜索

---

## 附录

### 相关文档
- 项目 CLAUDE.md
- 用户设置功能设计文档（2026-03-01-user-setting-design.md）

### 参考文件
- `views/permissions/menu/index.vue` - 权限管理页面参考
- `views/system/parameter/basic/BasicInfo.vue` - 系统参数管理参考

### Element Plus文档
- [Table 表格](https://element-plus.org/zh-CN/component/table.html)
- [Dialog 对话框](https://element-plus.org/zh-CN/component/dialog.html)
- [Form 表单](https://element-plus.org/zh-CN/component/form.html)

---

**文档版本：** 1.0
**最后更新：** 2026-03-01
**实施状态：** 待实施
