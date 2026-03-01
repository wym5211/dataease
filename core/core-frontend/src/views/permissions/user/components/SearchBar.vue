<template>
  <div class="search-bar">
    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="关键词">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索用户名/姓名/邮箱"
          clearable
          style="width: 200px"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="状态">
        <el-select
          v-model="searchForm.status"
          placeholder="用户状态"
          clearable
          style="width: 120px"
          @change="handleSearch"
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>

      <el-form-item label="角色">
        <el-select
          v-model="searchForm.roleId"
          placeholder="选择角色"
          clearable
          style="width: 150px"
          @change="handleSearch"
        >
          <el-option
            v-for="role in roles"
            :key="role.roleId"
            :label="role.roleName"
            :value="role.roleId"
          />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getUserOptions } from '../api'
import type { Role } from '../types'

interface SearchForm {
  keyword?: string
  status?: number
  roleId?: string
}

const emit = defineEmits<{
  search: [form: SearchForm]
}>()

const searchForm = ref<SearchForm>({
  keyword: undefined,
  status: undefined,
  roleId: undefined
})

const roles = ref<Role[]>([])

const loadRoles = async () => {
  try {
    const data = await getUserOptions()
    roles.value = data.roles
  } catch (error) {
    console.error('加载角色列表失败:', error)
  }
}

const handleSearch = () => {
  emit('search', { ...searchForm.value })
}

const handleReset = () => {
  searchForm.value = {
    keyword: undefined,
    status: undefined,
    roleId: undefined
  }
  emit('search', { ...searchForm.value })
}

onMounted(() => {
  loadRoles()
})

defineExpose({
  handleReset
})
</script>

<style scoped>
.search-bar {
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  margin-bottom: 16px;
}

.search-form {
  margin: 0;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}
</style>
