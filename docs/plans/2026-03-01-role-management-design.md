# 角色管理功能设计文档

**日期：** 2026-03-01
**状态：** 已确认，待实施
**相关功能：** 用户管理（已实施）

---

## 1. 功能概述

### 1.1 目标
实现系统角色管理功能，包括角色的增删改查和权限分配。

### 1.2 功能范围
- **角色列表管理**：创建、编辑、删除、查询角色
- **权限分配**：为角色分配菜单权限和资源权限
- **状态管理**：支持启用/禁用角色

### 1.3 页面位置
- 路由：`/permissions/role`
- 菜单：权限管理 > 角色管理
- 与用户管理同级

---

## 2. 页面设计

### 2.1 布局结构

```
┌─────────────────────────────────────────────┐
│  角色管理                                      │
├─────────────────────────────────────────────┤
│  [角色列表]  [权限分配]                          │
├─────────────────────────────────────────────┤
│                                             │
│  标签页1: 角色列表                              │
│  ┌─────────────────────────────────────┐    │
│  │ [搜索框] [状态筛选] [创建角色]        │    │
│  ├─────────────────────────────────────┤    │
│  │ 角色名 | 角色编码 | 描述 | 状态 | 操作 │    │
│  │ 管理员  │  admin   │ ... │ 启用 │ [编辑] │    │
│  │ 普通用户│  user    │ ... │ 启用 │ [删除] │    │
│  ├─────────────────────────────────────┤    │
│  │ [分页]                              │    │
│  └─────────────────────────────────────┘    │
│                                             │
│  标签页2: 权限分配                              │
│  ┌─────────────────────────────────────┐    │
│  │ 当前角色: [管理员 ▼]                 │    │
│  ├─────────────────────────────────────┤    │
│  │ [菜单权限] [资源权限]                │    │
│  │                                     │    │
│  │ ☑ 权限管理                         │    │
│  │   ☑ 用户管理                       │    │
│  │   ☐ 角色管理                       │    │
│  │ ☑ 工作台                           │    │
│  └─────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

### 2.2 组件结构

```
views/permissions/role/
├── index.vue                 # 主页面（标签页容器）
├── components/
│   ├── RoleList.vue          # 角色列表标签页
│   ├── RoleDialog.vue        # 创建/编辑角色对话框
│   ├── PermissionAssign.vue  # 权限分配标签页
│   ├── MenuPermissionTree.vue # 菜单权限树
│   └── ResourcePermission.vue # 资源权限配置
├── types.ts                  # 类型定义
└── api.ts                    # API 封装
```

---

## 3. 数据模型

### 3.1 角色实体

```typescript
interface Role {
  id: string
  name: string           // 角色名称
  code: string           // 角色编码
  description?: string   // 描述
  status: number         // 0-禁用, 1-启用
  createTime: string
}

interface RoleForm {
  name: string
  code: string
  description?: string
  status: number
}

interface RoleListRequest {
  page: number
  pageSize: number
  keyword?: string
  status?: number
}

interface RoleListResponse {
  records: Role[]
  total: number
}
```

### 3.2 权限实体

```typescript
interface MenuPermission {
  menuId: string
  menuName: string
  checked: boolean
  children?: MenuPermission[]
}

interface ResourcePermission {
  resourceId: string
  resourceName: string
  actions: string[]
}

interface RolePermission {
  roleId: string
  menuIds: string[]
  resourceIds: string[]
}
```

---

## 4. API 接口

### 4.1 已有接口（复用）

```typescript
// @/api/permissions/role.ts
GET    /role/options          # 获取角色选项
POST   /role/byCurOrg         # 获取角色列表
POST   /role/create           # 创建角色
PUT    /role/update/:id       # 更新角色
DELETE /role/delete/:id       # 删除角色
```

### 4.2 需要新增的接口

```typescript
// 权限相关接口
GET    /role/permission/:roleId       # 获取角色权限
POST   /role/permission/:roleId       # 设置角色权限
GET    /menu/tree                     # 获取菜单树
GET    /resource/list                 # 获取资源列表
```

---

## 5. 交互设计

### 5.1 标签页切换逻辑

```typescript
// 切换到"权限分配"标签页时
const handleTabChange = (tab: string) => {
  if (tab === 'permission' && !selectedRole.value) {
    ElMessage.warning('请先选择一个角色')
    activeTab.value = 'list'
    return
  }
  activeTab.value = tab
}
```

### 5.2 权限分配流程

1. 在角色列表中点击"分配权限"按钮
2. 自动切换到"权限分配"标签页
3. 自动选中该角色
4. 加载该角色的现有权限
5. 用户修改权限后点击保存

### 5.3 权限树行为

- 勾选父节点：自动勾选所有子节点
- 取消勾选父节点：自动取消所有子节点
- 部分勾选子节点：父节点显示半选状态

---

## 6. 路由配置

```typescript
// router/index.ts
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

## 7. 国际化

```typescript
// locales/zh-CN.ts
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

// locales/en.ts
role: {
  title: 'Role Management',
  list: 'Role List',
  permission: 'Permission Assignment',
  name: 'Role Name',
  code: 'Role Code',
  description: 'Description',
  status: 'Status',
  enabled: 'Enabled',
  disabled: 'Disabled',
  createRole: 'Create Role',
  editRole: 'Edit Role',
  deleteRole: 'Delete Role',
  deleteConfirm: 'Are you sure to delete role "{name}"?',
  assignPermission: 'Assign Permission',
  selectRole: 'Please select a role',
  selectRoleFirst: 'Please select a role first',
  menuPermission: 'Menu Permission',
  resourcePermission: 'Resource Permission',
  savePermission: 'Save Permission',
  permissionSaved: 'Permission saved successfully'
}
```

---

## 8. 依赖关系

### 8.1 需要复用的组件
- `el-tabs`, `el-tab-pane` - 标签页
- `el-table`, `el-pagination` - 表格和分页
- `el-dialog` - 对话框
- `el-form`, `el-form-item` - 表单
- `el-input`, `el-select` - 输入组件
- `el-tree` - 权限树
- `el-button`, `el-icon` - 按钮和图标

### 8.2 需要复用的 API
- `@/api/permissions/role.ts` - 角色 CRUD 接口
- `@/config/axios.ts` - axios 实例

### 8.3 需要新增的 API
- 权限查询和保存接口

---

## 9. 注意事项

### 9.1 数据格式适配
后端 API 返回的角色数据格式为 `{id, name, code}`，前端需要适配为 `{roleId, roleName, roleCode}`。

### 9.2 权限缓存
角色权限数据可以缓存，减少重复请求。

### 9.3 并发处理
保存权限时，如果用户切换角色，需要处理并发保存的问题。

---

## 10. 验收标准

- [ ] 角色列表页面正常显示，支持分页和搜索
- [ ] 可以创建、编辑、删除角色
- [ ] 可以启用/禁用角色
- [ ] 权限分配标签页可以正常切换
- [ ] 可以为角色分配菜单权限
- [ ] 可以为角色分配资源权限
- [ ] 权限保存后下次打开正确显示
- [ ] 国际化支持中英文
- [ ] 代码通过 ESLint 和 Stylelint 检查

---

**设计确认日期：** 2026-03-01
**设计确认人：** 用户
