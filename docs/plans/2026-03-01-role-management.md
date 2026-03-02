# 角色管理功能实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans or superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** 实现系统角色管理功能，包括角色的增删改查和权限分配。

**Architecture:** 采用标签页设计，角色列表和权限分配分离。角色管理使用表格+对话框模式，与用户管理保持一致。权限分配使用树形结构展示菜单权限。

**Tech Stack:** Vue 3 + TypeScript + Element Plus + Pinia

**参考实现:** 用户管理功能 (`views/permissions/user/`)

---

## 前置依赖检查

### Task 0: 检查后端 API 可用性

**检查项:**
- [ ] `GET /role/options` - 获取角色选项
- [ ] `POST /role/byCurOrg` - 获取角色列表
- [ ] `POST /role/create` - 创建角色
- [ ] `PUT /role/update/:id` - 更新角色
- [ ] `DELETE /role/delete/:id` - 删除角色
- [ ] `GET /role/permission/:roleId` - 获取角色权限（待确认）
- [ ] `POST /role/permission/:roleId` - 设置角色权限（待确认）
- [ ] `GET /menu/tree` - 获取菜单树（待确认）

**如果权限相关 API 不存在:**
- 先实现角色 CRUD 功能
- 权限分配功能标记为待定

---

## 第一阶段：基础结构搭建

### Task 1: 创建目录结构

**命令:**
```bash
mkdir -p core/core-frontend/src/views/permissions/role/components
```

**验证:**
```bash
ls -la core/core-frontend/src/views/permissions/role/
```

---

### Task 2: 创建类型定义文件

**文件:** `core/core-frontend/src/views/permissions/role/types.ts`

**内容:**
```typescript
// 角色实体
export interface Role {
  id: string
  name: string
  code: string
  description?: string
  status: number
  createTime: string
}

// 角色表单
export interface RoleForm {
  name: string
  code: string
  description?: string
  status: number
}

// 角色列表请求
export interface RoleListRequest {
  page: number
  pageSize: number
  keyword?: string
  status?: number
}

// 角色列表响应
export interface RoleListResponse {
  records: Role[]
  total: number
}

// 菜单权限
export interface MenuPermission {
  menuId: string
  menuName: string
  checked: boolean
  children?: MenuPermission[]
}

// 资源权限
export interface ResourcePermission {
  resourceId: string
  resourceName: string
  actions: string[]
}

// 角色权限
export interface RolePermission {
  roleId: string
  menuIds: string[]
  resourceIds: string[]
}
```

**验证:** TypeScript 无编译错误

---

### Task 3: 创建 API 封装

**文件:** `core/core-frontend/src/views/permissions/role/api.ts`

**内容:**
```typescript
import request from '@/config/axios'
import type {
  Role,
  RoleForm,
  RoleListRequest,
  RoleListResponse,
  MenuPermission,
  ResourcePermission
} from './types'

const BASE_URL = '/role'

// 获取角色列表
export const getRoleList = (params: RoleListRequest): Promise<RoleListResponse> => {
  return request.post({
    url: `${BASE_URL}/byCurOrg`,
    data: { keyword: params.keyword || '' }
  })
}

// 获取角色选项
export const getRoleOptions = (): Promise<Role[]> => {
  return request.get({ url: `${BASE_URL}/options` })
}

// 创建角色
export const createRole = (data: RoleForm): Promise<Role> => {
  return request.post({ url: `${BASE_URL}/create`, data })
}

// 更新角色
export const updateRole = (id: string, data: RoleForm): Promise<Role> => {
  return request.put({ url: `${BASE_URL}/update/${id}`, data })
}

// 删除角色
export const deleteRole = (id: string): Promise<void> => {
  return request.delete({ url: `${BASE_URL}/delete/${id}` })
}

// 获取角色权限
export const getRolePermissions = (roleId: string): Promise<{
  menus: MenuPermission[]
  resources: ResourcePermission[]
}> => {
  return request.get({ url: `${BASE_URL}/permission/${roleId}` })
}

// 保存角色权限
export const saveRolePermissions = (
  roleId: string,
  data: { menuIds: string[]; resourceIds: string[] }
): Promise<void> => {
  return request.post({ url: `${BASE_URL}/permission/${roleId}`, data })
}

// 获取菜单树
export const getMenuTree = (): Promise<MenuPermission[]> => {
  return request.get({ url: '/menu/tree' })
}

// 获取资源列表
export const getResourceList = (): Promise<ResourcePermission[]> => {
  return request.get({ url: '/resource/list' })
}
```

**验证:** TypeScript 无编译错误

---

## 第二阶段：角色列表功能

### Task 4: 创建 RoleDialog 组件

**文件:** `core/core-frontend/src/views/permissions/role/components/RoleDialog.vue`

**功能:** 创建/编辑角色的对话框

**参考:** `views/permissions/user/components/UserDialog.vue`

**Props:**
- `modelValue: boolean` - 对话框显示状态
- `role?: Role` - 编辑时的角色数据

**Events:**
- `update:modelValue` - 更新显示状态
- `success` - 操作成功

**表单字段:**
- 角色名称 (name) - 必填
- 角色编码 (code) - 必填
- 描述 (description) - 可选
- 状态 (status) - 单选：启用/禁用

---

### Task 5: 创建 RoleList 组件

**文件:** `core/core-frontend/src/views/permissions/role/components/RoleList.vue`

**功能:** 角色列表表格 + 搜索栏

**功能点:**
- 搜索框（按角色名称/编码搜索）
- 状态筛选下拉框（全部/启用/禁用）
- "创建角色"按钮
- 表格列：角色名称、角色编码、描述、状态、操作
- 操作列：编辑、删除、分配权限
- 分页器

**参考:** `views/permissions/user/index.vue` 中的表格部分

---

## 第三阶段：权限分配功能

### Task 6: 创建 MenuPermissionTree 组件

**文件:** `core/core-frontend/src/views/permissions/role/components/MenuPermissionTree.vue`

**功能:** 菜单权限树形选择

**Props:**
- `roleId: string` - 角色ID
- `modelValue: string[]` - 选中的菜单ID列表

**使用组件:** `el-tree`

**特性:**
- 显示父子层级关系
- 支持勾选/取消勾选
- 父节点勾选自动勾选所有子节点
- 显示半选状态

---

### Task 7: 创建 ResourcePermission 组件

**文件:** `core/core-frontend/src/views/permissions/role/components/ResourcePermission.vue`

**功能:** 资源权限配置

**Props:**
- `roleId: string` - 角色ID
- `modelValue: ResourcePermission[]` - 资源权限数据

**使用组件:** `el-table` + 自定义操作列

**表格列:**
- 资源名称
- 权限操作（多选：查看/编辑/删除/导出等）

---

### Task 8: 创建 PermissionAssign 组件

**文件:** `core/core-frontend/src/views/permissions/role/components/PermissionAssign.vue`

**功能:** 权限分配标签页主体

**功能点:**
- 角色选择器（下拉框选择要配置权限的角色）
- 子标签页：菜单权限 / 资源权限
- "保存权限"按钮
- 保存成功提示

**状态提示:**
- 未选择角色时显示提示"请先选择一个角色"

---

## 第四阶段：主页面集成

### Task 9: 创建 index.vue 主页面

**文件:** `core/core-frontend/src/views/permissions/role/index.vue`

**功能:** 标签页容器，整合 RoleList 和 PermissionAssign

**标签页:**
- "角色列表" - RoleList 组件
- "权限分配" - PermissionAssign 组件

**交互:**
- 点击角色列表的"分配权限" → 切换到权限分配标签页并选中该角色

---

## 第五阶段：路由和国际化

### Task 10: 添加路由配置

**文件:** `core/core-frontend/src/router/index.ts`

**添加位置:** 在 `permissions-user` 路由之后

**路由配置:**
```typescript
{
  path: 'role',
  name: 'permissions-role',
  component: () => import('@/views/permissions/role/index.vue'),
  meta: {
    title: '角色管理',
    icon: 'UserFilled'
  }
}
```

---

### Task 11: 添加国际化

**文件 1:** `core/core-frontend/src/locales/zh-CN.ts`

**添加内容:**
```typescript
role: {
  title: '角色管理',
  list: '角色列表',
  permission: '权限分配',
  name: '角色名称',
  code: '角色编码',
  description: '描述',
  status: '状态',
  enabled: '启用',
  disabled: '禁用',
  createRole: '创建角色',
  editRole: '编辑角色',
  deleteRole: '删除角色',
  deleteConfirm: '确定要删除角色"{name}"吗？',
  assignPermission: '分配权限',
  selectRole: '请选择角色',
  selectRoleFirst: '请先选择一个角色',
  menuPermission: '菜单权限',
  resourcePermission: '资源权限',
  savePermission: '保存权限',
  permissionSaved: '权限保存成功'
}
```

**文件 2:** `core/core-frontend/src/locales/en.ts`

**添加对应的英文翻译**

---

## 第六阶段：测试和优化

### Task 12: 功能测试

**测试项:**
- [ ] 角色列表加载正常
- [ ] 搜索功能正常
- [ ] 状态筛选正常
- [ ] 创建角色成功
- [ ] 编辑角色成功
- [ ] 删除角色成功（确认对话框正常）
- [ ] 分页功能正常
- [ ] 切换到权限分配标签页正常
- [ ] 选择角色后加载权限正常
- [ ] 保存权限成功

---

### Task 13: 代码检查

**命令:**
```bash
cd core/core-frontend
npm run lint
npm run lint:stylelint
```

**修复所有报错**

---

## 任务汇总

| 阶段 | 任务数 | 说明 |
|------|--------|------|
| 前置检查 | 1 | 确认后端 API 可用性 |
| 基础结构 | 3 | 目录、类型、API |
| 角色列表 | 2 | RoleDialog、RoleList |
| 权限分配 | 3 | 权限树、资源权限、权限分配页 |
| 主页面 | 1 | index.vue |
| 路由国际化 | 2 | 路由配置、翻译 |
| 测试优化 | 2 | 功能测试、代码检查 |
| **总计** | **14** | |

---

## 实施建议

1. **先确认后端 API**: 特别是权限相关接口，如果不存在，先实现角色 CRUD
2. **参考用户管理**: 大量代码可以参考已完成的用户管理功能
3. **小步提交**: 每个 Task 完成后及时提交
4. **测试驱动**: 每个功能点完成后立即测试

---

**计划创建日期:** 2026-03-01
**预计实施时间:** 4-6 小时
