<template>
  <div class="role-management">
    <!-- 角色列表 -->
    <role-list
      ref="roleListRef"
      @create="handleCreate"
      @edit="handleEdit"
      @delete-success="handleDeleteSuccess"
    />

    <!-- 创建/编辑角色对话框 -->
    <role-dialog v-model="roleDialogVisible" :role="currentRole" @success="handleRoleSuccess" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import RoleList from './components/RoleList.vue'
import RoleDialog from './components/RoleDialog.vue'
import type { Role } from './types'

const roleListRef = ref<InstanceType<typeof RoleList>>()
const roleDialogVisible = ref(false)
const currentRole = ref<Role | undefined>(undefined)

// 创建角色
const handleCreate = () => {
  currentRole.value = undefined
  roleDialogVisible.value = true
}

// 编辑角色
const handleEdit = (role: Role) => {
  currentRole.value = role
  roleDialogVisible.value = true
}

// 角色操作成功
const handleRoleSuccess = () => {
  roleListRef.value?.refresh()
}

// 删除成功
const handleDeleteSuccess = () => {
  // 删除成功后刷新列表
  roleListRef.value?.refresh()
}
</script>

<style scoped>
.role-management {
  padding: 24px;
}
</style>
