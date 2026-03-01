# 用户管理功能实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 在 DataEase 权限管理模块中添加用户管理功能，支持用户列表查看、创建、编辑、删除、重置密码以及角色和分组分配。

**架构：** 采用表格+弹窗模式，主页面包含搜索栏和用户列表表格，创建和编辑操作通过对话框完成。前端使用 Vue 3 + Element Plus，后端提供 RESTful API。

**技术栈：** Vue 3 (Composition API), Element Plus, TypeScript, Pinia, Vue Router 4, axios

---

## 前置条件

在开始实施前，确保：
- 开发环境已启动（前端 http://localhost:8081，后端 http://localhost:8100）
- 当前分支为 `dev-v2`
- 已阅读设计文档 `docs/plans/2026-03-01-user-management-design.md`

---

### Task 1: 添加路由配置

**文件：**
- Modify: `core/core-frontend/src/router/index.ts`

**Step 1: 打开路由配置文件**

在权限管理路由的 children 数组中添加用户管理路由（约在第 222 行之后）。

**Step 2: 添加路由配置**

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

完整位置应该插入在 `permissions-audit` 路由之后。

**Step 3: 保存文件**

确认路由配置正确，children 数组包含新的路由项。

**Step 4: 提交**

```bash
cd "E:\cursor\dataease"
git add core/core-frontend/src/router/index.ts
git commit -m "feat(user-management): 添加用户管理路由配置"
```

---

### Task 2: 添加菜单入口

**文件：**
- Modify: `core/core-frontend/src/views/permissions/index.vue`

**Step 1: 读取权限管理主页面**

查看现有菜单项的配置方式（菜单授权、资源授权等）。

**Step 2: 添加用户管理菜单项**

在菜单列表中添加"用户管理"入口。根据现有代码模式，可能在 data 中定义菜单数组。

添加菜单项：
```typescript
{
  name: '用户管理',
  path: '/permissions/user',
  icon: 'user' // 或其他合适的图标
}
```

**Step 3: 保存文件**

确认菜单项配置正确。

**Step 4: 提交**

```bash
cd "E:\cursor\dataease"
git add core/core-frontend/src/views/permissions/index.vue
git commit -m "feat(user-management): 添加用户管理菜单入口"
```

---

### Task 3: 创建 TypeScript 类型定义

**文件：**
- Create: `core/core-frontend/src/views/permissions/user/types.ts`

**Step 1: 创建类型定义文件**

定义用户相关的 TypeScript 类型。

**Step 2: 编写类型定义**

```typescript
export interface User {
  userId: string
  username: string
  nickName: string
  email: string
  phone?: string
  roles: Role[]
  groups: Group[]
  status: number
  createTime: string
}

export interface Role {
  roleId: string
  roleName: string
  roleCode: string
}

export interface Group {
  groupId: string
  groupName: string
}

export interface UserForm {
  username?: string
  nickName: string
  email: string
  phone?: string
  password?: string
  roleIds: string[]
  groupIds: string[]
  status: number
}

export interface UserListRequest {
  page: number
  pageSize: number
  keyword?: string
  status?: number
  roleId?: string
}

export interface UserListResponse {
  records: User[]
  total: number
}

export interface UserOptionsResponse {
  roles: Role[]
  groups: Group[]
}
```

**Step 3: 保存文件**

**Step 4: 提交**

```bash
cd "E:\cursor\dataease"
git add core/core-frontend/src/views/permissions/user/types.ts
git commit -m "feat(user-management): 添加用户管理类型定义"
```

---

### Task 4: 创建 API 接口文件

**文件：**
- Create: `core/core-frontend/src/views/permissions/user/api/index.ts`

**Step 1: 创建 API 文件**

**Step 2: 编写 API 函数**

```typescript
import { axiosInstance } from '@/utils/request'
import type {
  User,
  UserForm,
  UserListRequest,
  UserListResponse,
  UserOptionsResponse
} from '../types'

const BASE_URL = '/api/permissions/users'

/**
 * 获取用户列表
 */
export const getUserList = (params: UserListRequest): Promise<UserListResponse> => {
  return axiosInstance.get(`${BASE_URL}/list`, { params })
}

/**
 * 获取角色和分组选项
 */
export const getUserOptions = (): Promise<UserOptionsResponse> => {
  return axiosInstance.get(`${BASE_URL}/options`)
}

/**
 * 创建用户
 */
export const createUser = (data: UserForm): Promise<User> => {
  return axiosInstance.post(`${BASE_URL}/create`, data)
}

/**
 * 编辑用户
 */
export const updateUser = (userId: string, data: UserForm): Promise<User> => {
  return axiosInstance.put(`${BASE_URL}/update/${userId}`, data)
}

/**
 * 删除用户
 */
export const deleteUser = (userId: string): Promise<void> => {
  return axiosInstance.delete(`${BASE_URL}/delete/${userId}`)
}

/**
 * 重置用户密码
 */
export const resetPassword = (userId: string, newPassword: string): Promise<void> => {
  return axiosInstance.put(`${BASE_URL}/reset-password/${userId}`, { newPassword })
}
```

**Step 3: 保存文件**

**Step 4: 提交**

```bash
cd "E:\cursor\dataease"
git add core/core-frontend/src/views/permissions/user/api/index.ts
git commit -m "feat(user-management): 添加用户管理API接口"
```

---

### Task 5: 添加国际化文本

**文件：**
- Modify: `core/core-frontend/src/locales/zh-CN.ts`
- Modify: `core/core-frontend/src/locales/en.ts`
- Modify: `core/core-frontend/src/locales/tw.ts`

**Step 1: 添加中文翻译**

在 `zh-CN.ts` 中添加用户管理相关的翻译键（找到合适的位置，可能在 permissions 或 user 对象中）：

```typescript
user_management: {
  title: '用户管理',
  user_list: '用户列表',
  create_user: '创建用户',
  edit_user: '编辑用户',
  delete_user: '删除用户',
  reset_password: '重置密码',
  username: '用户名',
  nick_name: '昵称',
  email: '邮箱',
  phone: '手机号',
  password: '密码',
  role: '角色',
  group: '分组',
  status: '状态',
  status_enabled: '启用',
  status_disabled: '禁用',
  create_time: '创建时间',
  actions: '操作',
  search_placeholder: '搜索用户名、昵称或邮箱',
  status_all: '全部',
  status_filter: '状态筛选',
  role_filter: '角色筛选',
  confirm_delete: '确定要删除该用户吗？此操作不可恢复',
  confirm_reset_password: '确定要重置该用户的密码吗？',
  delete_success: '用户已删除',
  create_success: '用户创建成功',
  update_success: '用户信息已更新',
  reset_password_success: '密码已重置',
  cannot_delete_self: '不能删除当前登录用户',
  cannot_delete_admin: '不能删除系统管理员账户',
  username_required: '用户名不能为空',
  username_format: '只能包含字母、数字、下划线',
  username_length: '长度在 3 到 50 个字符',
  nick_name_required: '昵称不能为空',
  nick_name_length: '长度在 2 到 50 个字符',
  email_required: '邮箱不能为空',
  email_format: '请输入正确的邮箱地址',
  phone_format: '请输入正确的手机号',
  password_required: '密码不能为空',
  password_length: '密码长度至少6位',
  role_required: '请至少选择一个角色',
  no_users: '暂无用户',
  no_roles: '暂无角色选项',
  no_groups: '暂无分组选项'
}
```

**Step 2: 添加英文翻译**

在 `en.ts` 中添加对应的英文翻译：

```typescript
user_management: {
  title: 'User Management',
  user_list: 'User List',
  create_user: 'Create User',
  edit_user: 'Edit User',
  delete_user: 'Delete',
  reset_password: 'Reset Password',
  username: 'Username',
  nick_name: 'Nickname',
  email: 'Email',
  phone: 'Phone',
  password: 'Password',
  role: 'Role',
  group: 'Group',
  status: 'Status',
  status_enabled: 'Enabled',
  status_disabled: 'Disabled',
  create_time: 'Create Time',
  actions: 'Actions',
  search_placeholder: 'Search username, nickname or email',
  status_all: 'All',
  status_filter: 'Status Filter',
  role_filter: 'Role Filter',
  confirm_delete: 'Are you sure to delete this user? This action cannot be undone.',
  confirm_reset_password: 'Are you sure to reset this user\'s password?',
  delete_success: 'User deleted successfully',
  create_success: 'User created successfully',
  update_success: 'User updated successfully',
  reset_password_success: 'Password reset successfully',
  cannot_delete_self: 'Cannot delete current logged in user',
  cannot_delete_admin: 'Cannot delete system administrator account',
  username_required: 'Username is required',
  username_format: 'Only letters, numbers and underscores are allowed',
  username_length: 'Length should be between 3 and 50 characters',
  nick_name_required: 'Nickname is required',
  nick_name_length: 'Length should be between 2 and 50 characters',
  email_required: 'Email is required',
  email_format: 'Please enter a valid email address',
  phone_format: 'Please enter a valid phone number',
  password_required: 'Password is required',
  password_length: 'Password length should be at least 6 characters',
  role_required: 'Please select at least one role',
  no_users: 'No users',
  no_roles: 'No roles available',
  no_groups: 'No groups available'
}
```

**Step 3: 添加繁体中文翻译**

在 `tw.ts` 中添加对应的繁体中文翻译（同中文简体）。

**Step 4: 提交**

```bash
cd "E:\cursor\dataease"
git add core/core-frontend/src/locales/zh-CN.ts core/core-frontend/src/locales/en.ts core/core-frontend/src/locales/tw.ts
git commit -m "feat(user-management): 添加用户管理国际化文本"
```

---

### Task 6: 创建搜索栏组件

**文件：**
- Create: `core/core-frontend/src/views/permissions/user/components/SearchBar.vue`

**Step 1: 创建组件文件**

**Step 2: 编写组件代码**

```vue
<template>
  <div class="search-bar">
    <el-input
      v-model="searchKeyword"
      :placeholder="t('user_management.search_placeholder')"
      clearable
      style="width: 300px; margin-right: 12px"
      @clear="handleSearch"
      @keyup.enter="handleSearch"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>

    <el-select
      v-model="statusFilter"
      :placeholder="t('user_management.status_filter')"
      clearable
      style="width: 120px; margin-right: 12px"
      @change="handleSearch"
    >
      <el-option :label="t('user_management.status_all')" :value="undefined" />
      <el-option :label="t('user_management.status_enabled')" :value="1" />
      <el-option :label="t('user_management.status_disabled')" :value="0" />
    </el-select>

    <el-select
      v-model="roleFilter"
      :placeholder="t('user_management.role_filter')"
      clearable
      multiple
      collapse-tags
      collapse-tags-tooltip
      style="width: 150px; margin-right: 12px"
      @change="handleSearch"
    >
      <el-option
        v-for="role in roleOptions"
        :key="role.roleId"
        :label="role.roleName"
        :value="role.roleId"
      />
    </el-select>

    <el-button type="primary" @click="handleSearch">
      {{ t('common.search') }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from '@/hooks/useI18n'
import { Search } from '@element-plus/icons-vue'
import type { Role } from '../types'

const { t } = useI18n()

defineProps<{
  roleOptions: Role[]
}>()

const emit = defineEmits<{
  search: [keyword: string, status: number | undefined, roleIds: string[]]
}>()

const searchKeyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const roleFilter = ref<string[]>([])

const handleSearch = () => {
  emit('search', searchKeyword.value, statusFilter.value, roleFilter.value)
}
</script>

<style lang="scss" scoped>
.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
</style>
```

**Step 3: 保存文件**

**Step 4: 提交**

```bash
cd "E:\cursor\dataease"
git add core/core-frontend/src/views/permissions/user/components/SearchBar.vue
git commit -m "feat(user-management): 创建搜索栏组件"
```

---

### Task 7: 创建用户表格组件

**文件：**
- Create: `core/core-frontend/src/views/permissions/user/components/UserTable.vue`

**Step 1: 创建组件文件**

**Step 2: 编写组件代码**

```vue
<template>
  <div class="user-table">
    <el-table
      :data="data"
      v-loading="loading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="username" :label="t('user_management.username')" width="150" />
      <el-table-column prop="nickName" :label="t('user_management.nick_name')" width="120" />
      <el-table-column prop="email" :label="t('user_management.email')" width="200" />
      <el-table-column prop="phone" :label="t('user_management.phone')" width="130">
        <template #default="{ row }">
          {{ row.phone || '-' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('user_management.role')" width="150">
        <template #default="{ row }">
          <el-tag
            v-for="role in row.roles"
            :key="role.roleId"
            size="small"
            style="margin-right: 4px"
          >
            {{ role.roleName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('user_management.group')" width="150">
        <template #default="{ row }">
          <el-tag
            v-for="group in row.groups"
            :key="group.groupId"
            size="small"
            type="info"
            style="margin-right: 4px"
          >
            {{ group.groupName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('user_management.status')" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? t('user_management.status_enabled') : t('user_management.status_disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('user_management.actions')" width="180" align="right" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$emit('edit', row)">
            {{ t('common.edit') }}
          </el-button>
          <el-button link type="danger" @click="$emit('delete', row)">
            {{ t('user_management.delete_user') }}
          </el-button>
          <el-button link type="warning" @click="$emit('reset-password', row)">
            {{ t('user_management.reset_password') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 16px; justify-content: flex-end"
      @current-change="$emit('page-change', $event)"
      @size-change="$emit('page-size-change', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from '@/hooks/useI18n'
import type { User } from '../types'

const { t } = useI18n()

defineProps<{
  data: User[]
  loading: boolean
  total: number
}>()

defineEmits<{
  edit: [user: User]
  delete: [user: User]
  'reset-password': [user: User]
  'page-change': [page: number]
  'page-size-change': [pageSize: number]
}>()

const currentPage = ref(1)
const pageSize = ref(20)
</script>

<style lang="scss" scoped>
.user-table {
  :deep(.el-table) {
    font-size: 14px;
  }
}
</style>
```

**Step 3: 保存文件**

**Step 4: 提交**

```bash
cd "E:\cursor\dataease"
git add core/core-frontend/src/views/permissions/user/components/UserTable.vue
git commit -m "feat(user-management): 创建用户表格组件"
```

---

### Task 8: 创建用户对话框组件

**文件：**
- Create: `core/core-frontend/src/views/permissions/user/components/UserDialog.vue`

**Step 1: 创建组件文件**

**Step 2: 编写组件代码**

```vue
<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? t('user_management.edit_user') : t('user_management.create_user')"
    width="600px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="80px"
    >
      <el-form-item :label="t('user_management.username')" prop="username">
        <el-input
          v-model="formData.username"
          :placeholder="t('user_management.username')"
          :disabled="isEdit"
        />
      </el-form-item>

      <el-form-item :label="t('user_management.nick_name')" prop="nickName">
        <el-input
          v-model="formData.nickName"
          :placeholder="t('user_management.nick_name')"
        />
      </el-form-item>

      <el-form-item :label="t('user_management.email')" prop="email">
        <el-input
          v-model="formData.email"
          :placeholder="t('user_management.email')"
        />
      </el-form-item>

      <el-form-item :label="t('user_management.phone')" prop="phone">
        <el-input
          v-model="formData.phone"
          :placeholder="t('user_management.phone')"
        />
      </el-form-item>

      <el-form-item v-if="!isEdit" :label="t('user_management.password')" prop="password">
        <el-input
          v-model="formData.password"
          type="password"
          :placeholder="t('user_management.password')"
          show-password
        />
      </el-form-item>

      <el-form-item :label="t('user_management.role')" prop="roleIds">
        <el-select
          v-model="formData.roleIds"
          multiple
          :placeholder="t('user_management.role')"
          style="width: 100%"
        >
          <el-option
            v-for="role in roleOptions"
            :key="role.roleId"
            :label="role.roleName"
            :value="role.roleId"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('user_management.group')" prop="groupIds">
        <el-select
          v-model="formData.groupIds"
          multiple
          :placeholder="t('user_management.group')"
          style="width: 100%"
        >
          <el-option
            v-for="group in groupOptions"
            :key="group.groupId"
            :label="group.groupName"
            :value="group.groupId"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('user_management.status')" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :label="1">{{ t('user_management.status_enabled') }}</el-radio>
          <el-radio :label="0">{{ t('user_management.status_disabled') }}</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useI18n } from '@/hooks/useI18n'
import { ElMessage } from 'element-plus-secondary'
import type { FormInstance, FormRules } from 'element-plus-secondary'
import type { User, Role, Group, UserForm } from '../types'

const { t } = useI18n()

const props = defineProps<{
  modelValue: boolean
  isEdit: boolean
  user?: User
  roleOptions: Role[]
  groupOptions: Group[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [data: UserForm]
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const formData = reactive<UserForm>({
  username: '',
  nickName: '',
  email: '',
  phone: '',
  password: '',
  roleIds: [],
  groupIds: [],
  status: 1
})

const formRules: FormRules = {
  username: [
    { required: true, message: t('user_management.username_required'), trigger: 'blur' },
    { min: 3, max: 50, message: t('user_management.username_length'), trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: t('user_management.username_format'), trigger: 'blur' }
  ],
  nickName: [
    { required: true, message: t('user_management.nick_name_required'), trigger: 'blur' },
    { min: 2, max: 50, message: t('user_management.nick_name_length'), trigger: 'blur' }
  ],
  email: [
    { required: true, message: t('user_management.email_required'), trigger: 'blur' },
    { type: 'email', message: t('user_management.email_format'), trigger: ['blur', 'change'] }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: t('user_management.phone_format'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('user_management.password_required'), trigger: 'blur' },
    { min: 6, message: t('user_management.password_length'), trigger: 'blur' }
  ],
  roleIds: [
    { required: true, message: t('user_management.role_required'), trigger: 'change' }
  ]
}

const visible = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

watch(() => props.user, (user) => {
  if (user && props.isEdit) {
    Object.assign(formData, {
      username: user.username,
      nickName: user.nickName,
      email: user.email,
      phone: user.phone || '',
      roleIds: user.roles.map(r => r.roleId),
      groupIds: user.groups.map(g => g.groupId),
      status: user.status
    })
  } else {
    resetForm()
  }
})

const resetForm = () => {
  Object.assign(formData, {
    username: '',
    nickName: '',
    email: '',
    phone: '',
    password: '',
    roleIds: [],
    groupIds: [],
    status: 1
  })
  formRef.value?.clearValidate()
}

const handleClose = () => {
  visible.value = false
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate((valid) => {
    if (valid) {
      submitting.value = true
      emit('confirm', { ...formData })
      submitting.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
:deep(.el-dialog__body) {
  padding: 20px;
}
</style>
```

**Step 3: 保存文件**

**Step 4: 提交**

```bash
cd "E:\cursor\dataease"
git add core/core-frontend/src/views/permissions/user/components/UserDialog.vue
git commit -m "feat(user-management): 创建用户对话框组件"
```

---

### Task 9: 创建主页面组件

**文件：**
- Create: `core/core-frontend/src/views/permissions/user/index.vue`

**Step 1: 创建主页面文件**

**Step 2: 编写主页面代码**

```vue
<template>
  <div class="user-management">
    <div class="header">
      <h2>{{ t('user_management.title') }}</h2>
    </div>

    <search-bar
      :role-options="roleOptions"
      @search="handleSearch"
    />

    <user-table
      :data="userList"
      :loading="loading"
      :total="total"
      @edit="handleEdit"
      @delete="handleDelete"
      @reset-password="handleResetPassword"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
    />

    <user-dialog
      v-model="dialogVisible"
      :is-edit="isEditMode"
      :user="currentUser"
      :role-options="roleOptions"
      :group-options="groupOptions"
      @confirm="handleDialogConfirm"
    />

    <reset-password-dialog
      v-model="resetPasswordVisible"
      :user="currentUser"
      @confirm="handleResetPasswordConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useI18n } from '@/hooks/useI18n'
import { useUserStore } from '@/store/modules/user'
import { ElMessage, ElMessageBox } from 'element-plus-secondary'
import SearchBar from './components/SearchBar.vue'
import UserTable from './components/UserTable.vue'
import UserDialog from './components/UserDialog.vue'
import { getUserList, getUserOptions, createUser, updateUser, deleteUser, resetPassword } from './api'
import type { User, Role, Group, UserForm } from './types'

const { t } = useI18n()
const userStore = useUserStore()

// 状态
const loading = ref(false)
const userList = ref<User[]>([])
const total = ref(0)
const roleOptions = ref<Role[]>([])
const groupOptions = ref<Group[]>([])

// 分页和搜索
const searchParams = reactive({
  page: 1,
  pageSize: 20,
  keyword: '',
  status: undefined as number | undefined,
  roleId: undefined as string | undefined
})

// 对话框
const dialogVisible = ref(false)
const isEditMode = ref(false)
const currentUser = ref<User>()
const resetPasswordVisible = ref(false)

// 加载用户列表
const loadUserList = async () => {
  loading.value = true
  try {
    const { data } = await getUserList(searchParams)
    userList.value = data.records
    total.value = data.total
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 加载角色和分组选项
const loadOptions = async () => {
  try {
    const { data } = await getUserOptions()
    roleOptions.value = data.roles
    groupOptions.value = data.groups
  } catch (error) {
    ElMessage.error('加载选项失败')
  }
}

// 搜索处理
const handleSearch = (keyword: string, status: number | undefined, roleIds: string[]) => {
  searchParams.keyword = keyword
  searchParams.status = status
  searchParams.roleId = roleIds.length > 0 ? roleIds[0] : undefined
  searchParams.page = 1
  loadUserList()
}

// 分页处理
const handlePageChange = (page: number) => {
  searchParams.page = page
  loadUserList()
}

const handlePageSizeChange = (pageSize: number) => {
  searchParams.pageSize = pageSize
  searchParams.page = 1
  loadUserList()
}

// 创建用户
const handleCreate = () => {
  isEditMode.value = false
  currentUser.value = undefined
  dialogVisible.value = true
}

// 编辑用户
const handleEdit = (user: User) => {
  isEditMode.value = true
  currentUser.value = user
  dialogVisible.value = true
}

// 对话框确认
const handleDialogConfirm = async (formData: UserForm) => {
  try {
    if (isEditMode.value && currentUser.value) {
      await updateUser(currentUser.value.userId, formData)
      ElMessage.success(t('user_management.update_success'))
    } else {
      await createUser(formData)
      ElMessage.success(t('user_management.create_success'))
    }
    dialogVisible.value = false
    loadUserList()
  } catch (error: any) {
    ElMessage.error(error.msg || '操作失败')
  }
}

// 删除用户
const handleDelete = async (user: User) => {
  const currentUserInfo = userStore.userInfo

  // 安全检查
  if (user.username === currentUserInfo.username) {
    ElMessage.error(t('user_management.cannot_delete_self'))
    return
  }

  if (user.username === 'admin') {
    ElMessage.error(t('user_management.cannot_delete_admin'))
    return
  }

  try {
    await ElMessageBox.confirm(
      t('user_management.confirm_delete'),
      t('common.tip'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )

    await deleteUser(user.userId)
    ElMessage.success(t('user_management.delete_success'))
    loadUserList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.msg || '删除失败')
    }
  }
}

// 重置密码
const handleResetPassword = (user: User) => {
  currentUser.value = user
  resetPasswordVisible.value = true
}

// 重置密码确认
const handleResetPasswordConfirm = async (newPassword: string) => {
  if (!currentUser.value) return

  try {
    await resetPassword(currentUser.value.userId, newPassword)
    ElMessage.success(t('user_management.reset_password_success'))
    resetPasswordVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.msg || '重置密码失败')
  }
}

// 初始化
onMounted(() => {
  loadUserList()
  loadOptions()
})
</script>

<style lang="scss" scoped>
.user-management {
  padding: 24px;
  background: #f5f6f7;
  min-height: calc(100vh - 100px);

  .header {
    margin-bottom: 24px;

    h2 {
      font-size: 20px;
      font-weight: 500;
      color: #1f2329;
      margin: 0;
    }
  }
}
</style>
```

**Step 3: 保存文件**

**Step 4: 提交**

```bash
cd "E:\cursor\dataease"
git add core/core-frontend/src/views/permissions/user/index.vue
git commit -m "feat(user-management): 创建用户管理主页面"
```

---

### Task 10: 创建重置密码对话框组件

**文件：**
- Create: `core/core-frontend/src/views/permissions/user/components/ResetPasswordDialog.vue`

**Step 1: 创建组件文件**

**Step 2: 编写组件代码**

```vue
<template>
  <el-dialog
    v-model="visible"
    :title="t('user_management.reset_password')"
    width="400px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="80px"
    >
      <el-form-item :label="t('user_management.username')">
        <el-input :value="user?.username" disabled />
      </el-form-item>

      <el-form-item :label="t('user_management.password')" prop="newPassword">
        <el-input
          v-model="formData.newPassword"
          type="password"
          :placeholder="t('user_management.password')"
          show-password
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useI18n } from '@/hooks/useI18n'
import type { FormInstance, FormRules } from 'element-plus-secondary'
import type { User } from '../types'

const { t } = useI18n()

const props = defineProps<{
  modelValue: boolean
  user?: User
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [newPassword: string]
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const formData = reactive({
  newPassword: ''
})

const formRules: FormRules = {
  newPassword: [
    { required: true, message: t('user_management.password_required'), trigger: 'blur' },
    { min: 6, message: t('user_management.password_length'), trigger: 'blur' }
  ]
}

const visible = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(visible, (val) => {
  emit('update:modelValue', val)
  if (!val) {
    formData.newPassword = ''
    formRef.value?.clearValidate()
  }
})

const handleClose = () => {
  visible.value = false
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate((valid) => {
    if (valid) {
      emit('confirm', formData.newPassword)
    }
  })
}
</script>
```

**Step 3: 保存文件**

**Step 4: 提交**

```bash
cd "E:\cursor\dataease"
git add core/core-frontend/src/views/permissions/user/components/ResetPasswordDialog.vue
git commit -m "feat(user-management): 创建重置密码对话框组件"
```

---

### Task 11: 添加"创建用户"按钮

**文件：**
- Modify: `core/core-frontend/src/views/permissions/user/index.vue`

**Step 1: 修改主页面**

在 header 部分添加"创建用户"按钮：

```vue
<div class="header">
  <h2>{{ t('user_management.title') }}</h2>
  <el-button type="primary" @click="handleCreate">
    {{ t('user_management.create_user') }}
  </el-button>
</div>
```

同时添加样式：

```scss
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  h2 {
    font-size: 20px;
    font-weight: 500;
    color: #1f2329;
    margin: 0;
  }
}
```

**Step 2: 保存文件**

**Step 3: 提交**

```bash
cd "E:\cursor\dataease"
git add core/core-frontend/src/views/permissions/user/index.vue
git commit -m "feat(user-management): 添加创建用户按钮"
```

---

### Task 12: 前端功能测试

**文件：** 无（测试步骤）

**Step 1: 启动开发服务器**

确认前端开发服务器正在运行：
```bash
cd core/core-frontend
npm run dev:win
```

**Step 2: 访问用户管理页面**

在浏览器中打开：http://localhost:8081/#/permissions/user

**Step 3: 功能测试**

逐项测试以下功能：

1. **页面加载**
   - ✅ 页面正常显示
   - ✅ 表格显示用户列表（如果后端API已实现）
   - ✅ 搜索栏、分页器正常显示

2. **搜索功能**
   - ✅ 输入关键词搜索
   - ✅ 状态筛选
   - ✅ 角色筛选

3. **创建用户**
   - ✅ 点击"创建用户"按钮，对话框正常显示
   - ✅ 表单验证正常工作
   - ✅ 提交后显示成功/失败提示

4. **编辑用户**
   - ✅ 点击"编辑"按钮，对话框预填充用户数据
   - ✅ 修改后提交成功

5. **删除用户**
   - ✅ 点击"删除"按钮，显示确认对话框
   - ✅ 删除当前登录用户时提示错误
   - ✅ 删除admin用户时提示错误
   - ✅ 删除其他用户成功

6. **重置密码**
   - ✅ 点击"重置密码"按钮，对话框正常显示
   - ✅ 输入新密码并提交成功

7. **分页功能**
   - ✅ 切换每页条数
   - ✅ 翻页功能正常

8. **国际化**
   - ✅ 中英文切换正常
   - ✅ 所有文本正确显示

**Step 4: 浏览器控制台检查**

打开浏览器开发者工具（F12），检查：
- ✅ 无严重的JavaScript错误
- ✅ 网络请求正常（如果后端API已实现）
- ✅ 控制台无大量警告

**Step 5: 记录测试结果**

将测试结果记录在 `docs/tests/user-management-frontend-test-report.md`：

```markdown
# 用户管理前端测试报告

**测试日期：** 2026-03-01
**测试环境：** http://localhost:8081

## 测试结果

| 功能项 | 状态 | 备注 |
|--------|------|------|
| 页面加载 | ⬜ 通过 | |
| 搜索功能 | ⬜ 通过 | |
| 创建用户 | ⬜ 通过 | |
| 编辑用户 | ⬜ 通过 | |
| 删除用户 | ⬜ 通过 | |
| 重置密码 | ⬜ 通过 | |
| 分页功能 | ⬜ 通过 | |
| 国际化 | ⬜ 通过 | |
| 浏览器控制台 | ⬜ 通过 | |

## 发现的问题

（记录测试中发现的问题）

## 浏览器兼容性

- Chrome: ⬜ 通过
- Firefox: ⬜ 未测试
- Edge: ⬜ 未测试
```

**Step 6: 提交测试报告**

```bash
cd "E:\cursor\dataease"
git add docs/tests/user-management-frontend-test-report.md
git commit -m "test(user-management): 添加前端测试报告"
```

---

## 后续工作（后端实现）

前端功能完成后，需要实现后端API：

### 后端待实现接口

1. `GET /api/permissions/users/list` - 获取用户列表
2. `GET /api/permissions/users/options` - 获取角色和分组选项
3. `POST /api/permissions/users/create` - 创建用户
4. `PUT /api/permissions/users/update/{userId}` - 更新用户
5. `DELETE /api/permissions/users/delete/{userId}` - 删除用户
6. `PUT /api/permissions/users/reset-password/{userId}` - 重置密码

### 后端实施建议

- 创建 UserController 处理用户管理请求
- 创建 UserService 处理业务逻辑
- 创建 UserRepository 处理数据库操作
- 实现权限验证（仅管理员可访问）
- 实现数据验证（用户名唯一性、邮箱唯一性等）
- 实现安全检查（不能删除当前用户和admin用户）

---

## 完成检查清单

前端部分：

- ⬜ 路由配置已添加
- ⬜ 菜单入口已添加
- ⬜ TypeScript类型定义已创建
- ⬜ API接口文件已创建
- ⬜ 国际化文本已添加
- ⬜ SearchBar组件已创建
- ⬜ UserTable组件已创建
- ⬜ UserDialog组件已创建
- ⬜ ResetPasswordDialog组件已创建
- ⬜ 主页面已创建
- ⬜ 创建用户按钮已添加
- ⬜ 前端功能测试已完成

---

## 注意事项

1. **YAGNI原则：** 第一版只实现核心功能，导入/导出、批量操作等留待后续
2. **DRY原则：** 复用现有的权限管理布局和组件模式
3. **KISS原则：** 使用Element Plus标准组件，不自定义复杂组件
4. **安全性：** 前端检查是用户体验优化，真正的安全控制必须在后端实现
5. **测试：** 前端测试使用模拟数据即可，待后端API实现后再进行完整测试

---

**实施计划版本：** 1.0
**创建日期：** 2026-03-01
**预计工时：** 前端部分 4-6 小时
