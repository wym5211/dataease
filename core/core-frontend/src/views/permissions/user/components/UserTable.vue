<template>
  <div class="user-table-container">
    <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="nickName" label="姓名" width="120" />
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column prop="phone" label="手机号" width="140">
        <template #default="{ row }">
          {{ row.phone || '-' }}
        </template>
      </el-table-column>

      <el-table-column label="角色" width="180">
        <template #default="{ row }">
          <el-tag
            v-for="role in row.roles"
            :key="role.roleId"
            type="primary"
            size="small"
            style="margin-right: 4px"
          >
            {{ role.roleName }}
          </el-tag>
          <span v-if="!row.roles || row.roles.length === 0">-</span>
        </template>
      </el-table-column>

      <el-table-column label="用户组" width="150">
        <template #default="{ row }">
          <el-tag
            v-for="group in row.groups"
            :key="group.groupId"
            type="info"
            size="small"
            style="margin-right: 4px"
          >
            {{ group.groupName }}
          </el-tag>
          <span v-if="!row.groups || row.groups.length === 0">-</span>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="创建时间" width="180" />

      <el-table-column label="操作" min-width="320" width="320" fixed="right">
        <template #default="{ row }">
          <div class="operation-buttons">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="handleResetPassword(row)">
              重置密码
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { User } from '../types'

interface Props {
  users: User[]
  pagination: {
    page: number
    pageSize: number
    total: number
  }
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  'page-change': [page: number, pageSize: number]
  edit: [user: User]
  'reset-password': [user: User]
  delete: [user: User]
}>()

const currentPage = ref(1)
const pageSize = ref(10)

const tableData = computed(() => props.users)

// 同步分页状态
const syncPagination = () => {
  currentPage.value = props.pagination.page
  pageSize.value = props.pagination.pageSize
}

// 监听pagination变化
import { watch } from 'vue'
watch(() => props.pagination, syncPagination, { deep: true, immediate: true })

const handleSizeChange = (size: number) => {
  pageSize.value = size
  emit('page-change', currentPage.value, size)
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  emit('page-change', page, pageSize.value)
}

const handleEdit = (user: User) => {
  emit('edit', user)
}

const handleResetPassword = (user: User) => {
  emit('reset-password', user)
}

const handleDelete = (user: User) => {
  emit('delete', user)
}

defineExpose({
  resetPage: () => {
    currentPage.value = 1
  }
})
</script>

<style scoped>
.user-table-container {
  padding: 16px;
  background: #fff;
  border-radius: 4px;
}

.pagination-container {
  display: flex;
  margin-top: 16px;
  justify-content: flex-end;
}

.operation-buttons {
  display: flex;
  align-items: center;
  gap: 0;
  width: 100%;
  justify-content: flex-start;
  overflow: visible;
  white-space: nowrap;
}

.operation-buttons .el-button {
  padding: 0 2px !important;
  margin: 0 1px !important;
  flex-shrink: 0;
}
</style>
