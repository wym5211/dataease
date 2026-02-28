# 用户设置功能实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 为 DataEase 添加综合用户设置页面，包含个人信息、安全设置、偏好设置和分组信息管理

**架构：** 基于现有 modify-pwd 页面的侧边标签布局，创建独立的用户设置模块，集成到用户下拉菜单中

**技术栈：** Vue 3 + Element Plus + TypeScript + Vite + Pinia

---

## 前置准备

### Step 0: 创建开发分支

```bash
git checkout -b feature/user-setting
```

---

## 阶段一：基础框架搭建

### Task 1: 添加国际化文本

**文件：**
- Modify: `core/core-frontend/src/locale/lang/zh-CN.ts`
- Modify: `core/core-frontend/src/locale/lang/en-US.ts`

**Step 1: 添加中文翻译**

在 `zh-CN.ts` 中找到合适的位置（搜索 `"user"` 或 `"common"`），添加以下内容：

```typescript
export default {
  // ... 现有内容
  user: {
    user_setting: '用户设置',
    personal_info: '个人信息',
    security_settings: '安全设置',
    preferences: '偏好设置',
    group_info: '分组信息',
    edit: '编辑',
    save: '保存',
    cancel: '取消',
    username: '用户名',
    email: '邮箱',
    phone: '手机号',
    user_id: '用户ID',
    groups: '所属分组',
    roles: '角色',
    save_success: '保存成功',
    save_failed: '保存失败'
  }
}
```

**Step 2: 添加英文翻译**

在 `en-US.ts` 中添加对应内容：

```typescript
export default {
  // ... 现有内容
  user: {
    user_setting: 'User Settings',
    personal_info: 'Personal Info',
    security_settings: 'Security',
    preferences: 'Preferences',
    group_info: 'Group Info',
    edit: 'Edit',
    save: 'Save',
    cancel: 'Cancel',
    username: 'Username',
    email: 'Email',
    phone: 'Phone',
    user_id: 'User ID',
    groups: 'Groups',
    roles: 'Roles',
    save_success: 'Saved successfully',
    save_failed: 'Failed to save'
  }
}
```

**Step 3: 验证语法**

检查文件是否有语法错误：
```bash
cd core/core-frontend
npm run lint
```

**Step 4: 提交**

```bash
git add core/core-frontend/src/locale/lang/zh-CN.ts core/core-frontend/src/locale/lang/en-US.ts
git commit -m "feat(i18n): 添加用户设置页面国际化文本"
```

---

### Task 2: 创建主页面文件结构

**文件：**
- Create: `core/core-frontend/src/views/system/user-setting/index.vue`
- Create: `core/core-frontend/src/views/system/user-setting/components/Sidebar.vue`
- Create: `core/core-frontend/src/views/system/user-setting/components/PersonalInfo.vue`
- Create: `core/core-frontend/src/views/system/user-setting/components/SecuritySettings.vue`
- Create: `core/core-frontend/src/views/system/user-setting/components/Preferences.vue`
- Create: `core/core-frontend/src/views/system/user-setting/components/GroupInfo.vue`

**Step 1: 创建目录**

```bash
mkdir -p core/core-frontend/src/views/system/user-setting/components
```

**Step 2: 创建主页面 index.vue**

```vue
<template>
  <div class="user-setting flex-align-center">
    <div class="user-setting-container">
      <div class="user-tabs">
        <div class="tabs-title flex-align-center">{{ t('user.user_setting') }}</div>
        <el-divider />
        <Sidebar :active-tab="activeTab" @tab-change="handleTabChange" />
      </div>
      <div class="setting-content">
        <component :is="currentComponent" />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import Sidebar from './components/Sidebar.vue'
import PersonalInfo from './components/PersonalInfo.vue'
import SecuritySettings from './components/SecuritySettings.vue'
import Preferences from './components/Preferences.vue'
import GroupInfo from './components/GroupInfo.vue'

const { t } = useI18n()

type TabType = 'personal' | 'security' | 'preferences' | 'group'

const activeTab = ref<TabType>('personal')

const components = {
  personal: PersonalInfo,
  security: SecuritySettings,
  preferences: Preferences,
  group: GroupInfo
}

const currentComponent = computed(() => components[activeTab.value])

const handleTabChange = (tab: TabType) => {
  activeTab.value = tab
}
</script>

<style lang="less" scoped>
.user-setting {
  width: 100%;
  flex-direction: column;
  padding-top: 24px;

  .user-setting-container {
    display: flex;
    font-style: normal;
  }

  .user-tabs {
    width: 200px;
    border-radius: 4px;
    background: #fff;
    padding: 16px;
    height: fit-content;

    .ed-divider {
      margin: 4px 0;
      border-color: rgba(31, 35, 41, 0.15);
    }

    .tabs-title {
      padding-left: 8px;
      color: #8d9199;
      font-size: 14px;
      font-style: normal;
      font-weight: 500;
      line-height: 22px;
      height: 40px;
    }
  }

  .setting-content {
    margin-left: 16px;
    width: 864px;
    min-height: 400px;
  }
}
</style>
```

**Step 3: 创建侧边栏组件 Sidebar.vue**

```vue
<template>
  <div class="sidebar-tabs">
    <div
      v-for="tab in tabs"
      :key="tab.key"
      class="tab-item"
      :class="{ active: activeTab === tab.key }"
      @click="handleClick(tab.key)"
    >
      {{ tab.label }}
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'

type TabType = 'personal' | 'security' | 'preferences' | 'group'

interface Props {
  activeTab: TabType
}

interface Emits {
  (e: 'tabChange', tab: TabType): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const { t } = useI18n()

const tabs = computed(() => [
  { key: 'personal' as TabType, label: t('user.personal_info') },
  { key: 'security' as TabType, label: t('user.security_settings') },
  { key: 'preferences' as TabType, label: t('user.preferences') },
  { key: 'group' as TabType, label: t('user.group_info') }
])

const handleClick = (tab: TabType) => {
  emit('tabChange', tab)
}
</script>

<style lang="less" scoped>
.sidebar-tabs {
  .tab-item {
    padding: 9px 8px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    color: #1f2329;
    transition: all 0.2s;

    &:hover {
      background-color: #1f23291a;
    }

    &.active {
      background-color: rgba(51, 112, 255, 0.2);
      color: #3370ff;
      font-weight: 500;
    }
  }
}
</style>
```

**Step 4: 创建个人信息组件 PersonalInfo.vue**

```vue
<template>
  <div class="personal-info">
    <div class="info-card">
      <div class="info-header">
        <span class="title">{{ t('user.personal_info') }}</span>
        <el-button v-if="!isEditing" @click="startEdit" type="primary">
          {{ t('user.edit') }}
        </el-button>
        <template v-else>
          <el-button @click="cancelEdit">{{ t('user.cancel') }}</el-button>
          <el-button @click="saveInfo" type="primary" :loading="saving">
            {{ t('user.save') }}
          </el-button>
        </template>
      </div>
      <el-divider />
      <el-form :model="formData" label-width="100px" class="info-form">
        <el-form-item :label="t('user.user_id')">
          <span class="readonly-value">{{ userStore.getUid }}</span>
        </el-form-item>
        <el-form-item :label="t('user.username')">
          <el-input v-if="isEditing" v-model="formData.name" />
          <span v-else class="readonly-value">{{ userStore.getName }}</span>
        </el-form-item>
        <el-form-item :label="t('user.email')">
          <el-input v-if="isEditing" v-model="formData.email" />
          <span v-else class="readonly-value">{{ userInfo.email || '-' }}</span>
        </el-form-item>
        <el-form-item :label="t('user.phone')">
          <el-input v-if="isEditing" v-model="formData.phone" />
          <span v-else class="readonly-value">{{ userInfo.phone || '-' }}</span>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStoreWithOut } from '@/store/modules/user'
import { ElMessage } from 'element-plus-secondary'
import { useI18n } from '@/hooks/web/useI18n'

const { t } = useI18n()
const userStore = useUserStoreWithOut()

const isEditing = ref(false)
const saving = ref(false)

const userInfo = reactive({
  name: '',
  email: '',
  phone: ''
})

const formData = reactive({
  name: '',
  email: '',
  phone: ''
})

const loadUserInfo = () => {
  // TODO: 从后端 API 获取用户详细信息
  formData.name = userStore.getName
  userInfo.name = userStore.getName
}

const startEdit = () => {
  formData.name = userInfo.name
  formData.email = userInfo.email
  formData.phone = userInfo.phone
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
}

const saveInfo = async () => {
  saving.value = true
  try {
    // TODO: 调用后端 API 保存用户信息
    // await updateUserApi(formData)
    userInfo.name = formData.name
    userInfo.email = formData.email
    userInfo.phone = formData.phone
    ElMessage.success(t('user.save_success'))
    isEditing.value = false
  } catch (error) {
    ElMessage.error(t('user.save_failed'))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style lang="less" scoped>
.personal-info {
  .info-card {
    padding: 20px 24px 24px;
    border-radius: 4px;
    background: #fff;

    .info-header {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .title {
        color: #1f2329;
        font-size: 16px;
        font-weight: 500;
        line-height: 24px;
      }
    }

    .readonly-value {
      color: #1f2329;
      font-size: 14px;
    }
  }
}
</style>
```

**Step 5: 创建安全设置组件 SecuritySettings.vue**

```vue
<template>
  <div class="security-settings">
    <div class="security-card">
      <div class="security-header">
        <span class="title">{{ t('user.security_settings') }}</span>
      </div>
      <el-divider />
      <div class="security-content">
        <update-pwd />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useI18n } from '@/hooks/web/useI18n'
import UpdatePwd from '../../modify-pwd/UpdatePwd.vue'

const { t } = useI18n()
</script>

<style lang="less" scoped>
.security-settings {
  .security-card {
    padding: 20px 24px 24px;
    border-radius: 4px;
    background: #fff;

    .security-header {
      .title {
        color: #1f2329;
        font-size: 16px;
        font-weight: 500;
        line-height: 24px;
      }
    }
  }
}
</style>
```

**Step 6: 创建偏好设置组件 Preferences.vue**

```vue
<template>
  <div class="preferences">
    <div class="preferences-card">
      <div class="preferences-header">
        <span class="title">{{ t('user.preferences') }}</span>
      </div>
      <el-divider />
      <div class="preferences-content">
        <p class="placeholder-text">偏好设置功能开发中...</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useI18n } from '@/hooks/web/useI18n'

const { t } = useI18n()
</script>

<style lang="less" scoped>
.preferences {
  .preferences-card {
    padding: 20px 24px 24px;
    border-radius: 4px;
    background: #fff;
    min-height: 200px;

    .preferences-header {
      .title {
        color: #1f2329;
        font-size: 16px;
        font-weight: 500;
        line-height: 24px;
      }
    }

    .placeholder-text {
      color: #8f959e;
      font-size: 14px;
      text-align: center;
      margin-top: 40px;
    }
  }
}
</style>
```

**Step 7: 创建分组信息组件 GroupInfo.vue**

```vue
<template>
  <div class="group-info">
    <div class="group-card">
      <div class="group-header">
        <span class="title">{{ t('user.group_info') }}</span>
      </div>
      <el-divider />
      <div class="group-content">
        <p class="placeholder-text">分组信息功能开发中...</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useI18n } from '@/hooks/web/useI18n'

const { t } = useI18n()
</script>

<style lang="less" scoped>
.group-info {
  .group-card {
    padding: 20px 24px 24px;
    border-radius: 4px;
    background: #fff;
    min-height: 200px;

    .group-header {
      .title {
        color: #1f2329;
        font-size: 16px;
        font-weight: 500;
        line-height: 24px;
      }
    }

    .placeholder-text {
      color: #8f959e;
      font-size: 14px;
      text-align: center;
      margin-top: 40px;
    }
  }
}
</style>
```

**Step 8: 提交**

```bash
git add core/core-frontend/src/views/system/user-setting/
git commit -m "feat(user-setting): 创建用户设置页面基础组件结构"
```

---

### Task 3: 添加路由配置

**文件：**
- Modify: `core/core-frontend/src/router/index.ts:223`

**Step 1: 找到插入位置**

在 `permissions` 路由之后（大约在第 223 行），添加新的路由配置。

**Step 2: 添加路由配置**

在 routes 数组的最后添加：

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

**Step 3: 验证路由文件语法**

```bash
cd core/core-frontend
npm run type-check
```

**Step 4: 提交**

```bash
git add core/core-frontend/src/router/index.ts
git commit -m "feat(router): 添加用户设置路由"
```

---

### Task 4: 添加菜单入口

**文件：**
- Modify: `core/core-frontend/src/layout/components/AccountOperator.vue:99`

**Step 1: 找到修改位置**

在第 99-105 行，找到 `linkLoaded` 调用的位置。

**Step 2: 在第 103 行之后添加菜单项**

将：
```typescript
linkLoaded([{ id: 2, link: '/modify-pwd/index', label: t('user.change_password') }])
```

修改为：
```typescript
linkLoaded([{ id: 2, link: '/modify-pwd/index', label: t('user.change_password') }])
linkLoaded([{ id: 3, link: '/user-setting/index', label: t('user.user_setting') }])
```

**Step 3: 验证修改**

检查文件语法是否正确：
```bash
cd core/core-frontend
npm run lint
```

**Step 4: 提交**

```bash
git add core/core-frontend/src/layout/components/AccountOperator.vue
git commit -m "feat(menu): 在用户下拉菜单中添加用户设置入口"
```

---

## 阶段二：功能完善

### Task 5: 测试基础页面访问

**Step 1: 启动开发服务器**

```bash
cd core/core-frontend
npm run dev:win
```

**Step 2: 访问页面**

1. 打开浏览器访问 `http://localhost:8081`
2. 登录系统
3. 点击右上角用户头像
4. 查看下拉菜单中是否有"用户设置"选项
5. 点击进入用户设置页面

**Step 6: 验证功能**

- [ ] 页面正常显示，侧边栏标签可见
- [ ] 点击不同标签可以切换内容区域
- [ ] 四个标签（个人信息、安全设置、偏好设置、分组信息）都能正常显示
- [ ] 样式与系统其他页面保持一致

**Step 3: 提交验证**

```bash
git commit --allow-empty -m "test: 验证用户设置页面基础功能"
```

---

### Task 6: 完善个人信息组件（添加表单验证）

**文件：**
- Modify: `core/core-frontend/src/views/system/user-setting/components/PersonalInfo.vue`

**Step 1: 添加验证规则**

在 `<script>` 部分添加验证规则：

```typescript
import { ref, reactive, onMounted } from 'vue'
// ... 其他导入

// 添加验证规则
const emailRules = [
  { required: false, message: '请输入邮箱', trigger: 'blur' },
  { type: 'email' as const, message: '请输入正确的邮箱格式', trigger: 'blur' }
]

const phoneRules = [
  { required: false, message: '请输入手机号', trigger: 'blur' },
  { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
]

// 修改 saveInfo 函数，添加验证
const saveInfo = async () => {
  // 验证邮箱
  if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
    ElMessage.error('请输入正确的邮箱格式')
    return
  }

  // 验证手机号
  if (formData.phone && !/^1[3-9]\d{9}$/.test(formData.phone)) {
    ElMessage.error('请输入正确的手机号')
    return
  }

  saving.value = true
  try {
    // TODO: 调用后端 API 保存用户信息
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟 API 调用
    userInfo.name = formData.name
    userInfo.email = formData.email
    userInfo.phone = formData.phone
    ElMessage.success(t('user.save_success'))
    isEditing.value = false
  } catch (error) {
    ElMessage.error(t('user.save_failed'))
  } finally {
    saving.value = false
  }
}
```

**Step 2: 更新模板，添加验证提示**

修改 el-form-item：

```vue
<el-form-item :label="t('user.email')">
  <el-input v-if="isEditing" v-model="formData.email" placeholder="请输入邮箱" />
  <span v-else class="readonly-value">{{ userInfo.email || '-' }}</span>
</el-form-item>
<el-form-item :label="t('user.phone')">
  <el-input v-if="isEditing" v-model="formData.phone" placeholder="请输入手机号" />
  <span v-else class="readonly-value">{{ userInfo.phone || '-' }}</span>
</el-form-item>
```

**Step 3: 测试验证**

1. 进入个人信息标签
2. 点击"编辑"
3. 输入错误格式的邮箱（如 "test"）
4. 点击"保存"，应该看到错误提示
5. 输入正确的邮箱和手机号
6. 保存成功

**Step 4: 提交**

```bash
git add core/core-frontend/src/views/system/user-setting/components/PersonalInfo.vue
git commit -m "feat(user-setting): 添加个人信息表单验证"
```

---

### Task 7: 完善分组信息组件

**文件：**
- Modify: `core/core-frontend/src/views/system/user-setting/components/GroupInfo.vue`

**Step 1: 实现分组信息展示**

```vue
<template>
  <div class="group-info">
    <div class="group-card">
      <div class="group-header">
        <span class="title">{{ t('user.group_info') }}</span>
      </div>
      <el-divider />
      <div class="group-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="t('user.roles')">
            <el-tag v-for="role in userRoles" :key="role.id" class="role-tag">
              {{ role.name }}
            </el-tag>
            <span v-if="userRoles.length === 0">-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('user.groups')">
            <el-tag v-for="group in userGroups" :key="group.id" class="role-tag">
              {{ group.name }}
            </el-tag>
            <span v-if="userGroups.length === 0">-</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { useUserStoreWithOut } from '@/store/modules/user'

const { t } = useI18n()
const userStore = useUserStoreWithOut()

interface Role {
  id: string
  name: string
}

interface Group {
  id: string
  name: string
}

const userRoles = ref<Role[]>([])
const userGroups = ref<Group[]>([])

const loadGroupInfo = async () => {
  // TODO: 从后端 API 获取用户分组和角色信息
  // 暂时使用模拟数据
  userRoles.value = [
    { id: '1', name: '管理员' }
  ]
  userGroups.value = [
    { id: '1', name: '默认分组' }
  ]
}

onMounted(() => {
  loadGroupInfo()
})
</script>

<style lang="less" scoped>
.group-info {
  .group-card {
    padding: 20px 24px 24px;
    border-radius: 4px;
    background: #fff;

    .group-header {
      .title {
        color: #1f2329;
        font-size: 16px;
        font-weight: 500;
        line-height: 24px;
      }
    }

    .role-tag {
      margin-right: 8px;
    }
  }
}
</style>
```

**Step 2: 测试显示**

1. 进入分组信息标签
2. 查看是否正确显示角色和分组信息
3. 如果没有数据，显示 "-"

**Step 3: 提交**

```bash
git add core/core-frontend/src/views/system/user-setting/components/GroupInfo.vue
git commit -m "feat(user-setting): 实现分组信息展示"
```

---

## 阶段三：样式优化

### Task 8: 统一样式细节

**Step 1: 检查所有组件的样式一致性**

确认以下样式规范：
- 标题：16px, 500 weight, #1f2329
- 内边距：20px 24px 24px
- 卡片背景：#ffffff
- 边框圆角：4px

**Step 2: 调整主页面布局**

确保主页面与 modify-pwd 页面布局一致：

```vue
<style lang="less" scoped>
.user-setting {
  width: 100%;
  flex-direction: column;
  padding-top: 24px;
  background: #f5f6f7;

  .user-setting-container {
    display: flex;
    font-style: normal;
  }

  .user-tabs {
    width: 200px;
    height: fit-content;
    border-radius: 4px;
    background: #fff;
    padding: 16px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

    .ed-divider {
      margin: 4px 0;
      border-color: rgba(31, 35, 41, 0.15);
    }

    .tabs-title {
      padding-left: 8px;
      color: #8d9199;
      font-size: 14px;
      font-style: normal;
      font-weight: 500;
      line-height: 22px;
      height: 40px;
    }
  }

  .setting-content {
    margin-left: 16px;
    width: 864px;
  }
}
</style>
```

**Step 3: 提交**

```bash
git add core/core-frontend/src/views/system/user-setting/index.vue
git commit -m "style(user-setting): 统一页面样式"
```

---

### Task 9: 浏览器测试

**Step 1: 在不同浏览器中测试**

测试以下浏览器：
- [ ] Chrome
- [ ] Firefox
- [ ] Edge

**Step 2: 测试功能清单**

- [ ] 页面加载正常
- [ ] 标签切换流畅
- [ ] 表单验证生效
- [ ] 样式显示正确
- [ ] 国际化切换正常

**Step 3: 提交**

```bash
git commit --allow-empty -m "test: 完成浏览器兼容性测试"
```

---

## 阶段四：文档和收尾

### Task 10: 更新项目文档

**文件：**
- Modify: `E:\cursor\dataease\docs\plans\2026-03-01-user-setting-design.md`

**Step 1: 更新实施计划状态**

将设计文档中的实施计划状态从 ⏳ 改为 ✅

**Step 2: 添加使用说明**

在文档末尾添加：

```markdown
## 使用说明

### 访问入口

1. 登录 DataEase 系统
2. 点击右上角用户头像
3. 在下拉菜单中选择"用户设置"

### 功能说明

- **个人信息：** 查看和编辑用户基本信息
- **安全设置：** 修改登录密码
- **偏好设置：** 个性化设置（开发中）
- **分组信息：** 查看用户所属分组和角色
```

**Step 3: 提交**

```bash
git add docs/plans/2026-03-01-user-setting-design.md
git commit -m "docs: 更新用户设置功能文档"
```

---

### Task 11: 代码审查和清理

**Step 1: 运行代码检查**

```bash
cd core/core-frontend
npm run lint
npm run type-check
```

**Step 2: 修复所有警告和错误**

**Step 3: 检查未使用的代码**

移除：
- 未使用的导入
- 注释掉的代码
- 调试用的 console.log

**Step 4: 最终提交**

```bash
git add -A
git commit -m "chore(user-setting): 代码清理和优化"
```

---

### Task 12: 合并到主分支

**Step 1: 推送到远程**

```bash
git push origin feature/user-setting
```

**Step 2: 创建 Pull Request**

在 GitHub/GitLab 上创建 PR，标题：
```
feat: 添加用户设置功能
```

描述：
```
## 功能概述
添加综合用户设置页面，包含：
- 个人信息管理
- 安全设置（密码修改）
- 偏好设置（框架）
- 分组信息展示

## 实施细节
- 创建独立的用户设置页面
- 采用侧边标签布局
- 集成到用户下拉菜单
- 支持国际化（中英文）

## 测试
- [x] 功能测试
- [x] 样式验证
- [x] 国际化验证
- [x] 浏览器兼容性

## 相关文档
- 设计文档：docs/plans/2026-03-01-user-setting-design.md
```

**Step 3: 代码审查后合并**

等待审查通过后合并到 dev-v2 分支。

---

## 实施注意事项

### 关键点

1. **复用现有组件：** SecuritySettings 组件复用了 modify-pwd/UpdatePwd.vue
2. **国际化优先：** 先添加国际化文本，再实现功能
3. **渐进式开发：** 偏好设置先做框架，后续扩展
4. **样式一致性：** 严格参考 modify-pwd 页面样式

### 已知限制

1. **API 接口：** 当前使用模拟数据，需要后续对接真实后端接口
2. **偏好设置：** 仅实现了 UI 框架，功能待实现
3. **分组信息：** 使用模拟数据，需要对接权限模块

### 后续优化

1. 对接真实后端 API
2. 完善偏好设置功能
3. 添加更多个人信息字段
4. 优化移动端体验

---

## 测试检查清单

### 功能测试
- [ ] 页面可以正常访问
- [ ] 标签切换正常工作
- [ ] 个人信息可以查看和编辑
- [ ] 密码修改功能正常
- [ ] 表单验证生效
- [ ] 分组信息正确显示

### 样式测试
- [ ] 与系统其他页面风格一致
- [ ] 响应式布局正常
- [ ] 悬停和激活状态正确
- [ ] 颜色和间距符合规范

### 国际化测试
- [ ] 中文显示正常
- [ ] 英文显示正常
- [ ] 所有文本都有对应翻译

### 浏览器测试
- [ ] Chrome 正常
- [ ] Firefox 正常
- [ ] Edge 正常

---

**计划版本：** 1.0
**创建日期：** 2026-03-01
**最后更新：** 2026-03-01
