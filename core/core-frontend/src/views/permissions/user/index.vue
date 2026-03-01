<template>
  <div class="user-management">
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <search-bar
        v-model:keyword="searchKeyword"
        v-model:status="searchStatus"
        v-model:roleId="searchRoleId"
        :role-options="roleOptions"
        @search="handleSearch"
        @reset="handleReset"
      />

      <!-- 操作按钮 -->
      <div class="toolbar">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          创建用户
        </el-button>
      </div>

      <!-- 用户表格 -->
      <user-table
        :loading="loading"
        :users="userList"
        :pagination="pagination"
        @page-change="handlePageChange"
        @edit="handleEdit"
        @delete="handleDelete"
        @reset-password="handleResetPassword"
      />
    </el-card>

    <!-- 创建/编辑用户对话框 -->
    <user-dialog
      v-model="userDialogVisible"
      :user="currentUser"
      :role-options="roleOptions"
      :group-options="groupOptions"
      @success="handleUserSuccess"
    />

    <!-- 重置密码对话框 -->
    <reset-password-dialog
      v-model="resetPasswordDialogVisible"
      :user="currentUser"
      @success="handleResetPasswordSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getUserList, getUserOptions, deleteUser } from './api'
import { roleList } from '@/api/permissions/role'
import type { User, Role, Group } from './types'
import SearchBar from './components/SearchBar.vue'
import UserTable from './components/UserTable.vue'
import UserDialog from './components/UserDialog.vue'
import ResetPasswordDialog from './components/ResetPasswordDialog.vue'

const loading = ref(false)
const userList = ref<User[]>([])
const roleOptions = ref<Role[]>([])
const groupOptions = ref<Group[]>([])

const searchKeyword = ref('')
const searchStatus = ref<number | undefined>(undefined)
const searchRoleId = ref<string | undefined>(undefined)

const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

const userDialogVisible = ref(false)
const resetPasswordDialogVisible = ref(false)
const currentUser = ref<User | undefined>(undefined)

// 加载用户列表
const loadUserList = async () => {
  loading.value = true
  try {
    const res = await getUserList({
      page: pagination.value.page,
      pageSize: pagination.value.pageSize,
      keyword: searchKeyword.value || undefined,
      status: searchStatus.value,
      roleId: searchRoleId.value
    })
    userList.value = res.records
    pagination.value.total = res.total
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 加载选项数据
const loadOptions = async () => {
  try {
    const [userOptionsRes, roleListRes] = await Promise.all([
      getUserOptions(),
      roleList({ page: 1, pageSize: 1000 })
    ])
    roleOptions.value = userOptionsRes.roles
    groupOptions.value = userOptionsRes.groups
    // 如果从 getUserOptions 获取的角色为空，则使用角色列表
    if (roleOptions.value.length === 0) {
      roleOptions.value = roleListRes.records
    }
  } catch (error) {
    ElMessage.error('获取选项数据失败')
  }
}

// 搜索
const handleSearch = () => {
  pagination.value.page = 1
  loadUserList()
}

// 重置搜索
const handleReset = () => {
  searchKeyword.value = ''
  searchStatus.value = undefined
  searchRoleId.value = undefined
  pagination.value.page = 1
  loadUserList()
}

// 分页变化
const handlePageChange = (page: number, pageSize: number) => {
  pagination.value.page = page
  pagination.value.pageSize = pageSize
  loadUserList()
}

// 创建用户
const handleCreate = () => {
  currentUser.value = undefined
  userDialogVisible.value = true
}

// 编辑用户
const handleEdit = (user: User) => {
  currentUser.value = user
  userDialogVisible.value = true
}

// 删除用户
const handleDelete = async (user: User) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户 "${user.nickName}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteUser(user.userId)
    ElMessage.success('删除成功')
    loadUserList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 重置密码
const handleResetPassword = (user: User) => {
  currentUser.value = user
  resetPasswordDialogVisible.value = true
}

// 用户操作成功
const handleUserSuccess = () => {
  loadUserList()
}

// 重置密码成功
const handleResetPasswordSuccess = () => {
  ElMessage.success('密码重置成功')
}

onMounted(() => {
  loadUserList()
  loadOptions()
})
</script>

<style scoped>
.user-management {
  padding: 24px;

  .el-card {
    min-height: calc(100vh - 120px);

    :deep(.el-card__body) {
      padding: 20px;
    }
  }

  .toolbar {
    margin: 16px 0;

    .el-button {
      margin-right: 8px;
    }
  }
}
</style>
