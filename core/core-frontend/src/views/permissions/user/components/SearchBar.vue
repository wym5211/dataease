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
            v-for="role in roleOptions"
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
import { ref, onMounted, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getUserOptions } from '../api'
import type { Role } from '../types'

interface SearchForm {
  keyword?: string
  status?: number
  roleId?: string
}

interface Props {
  keyword?: string
  status?: number
  roleId?: string
  roleOptions: Role[]
}

interface Emits {
  (e: 'update:keyword', value: string): void
  (e: 'update:status', value: number | undefined): void
  (e: 'update:roleId', value: string | undefined): void
  (e: 'search'): void
  (e: 'reset'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const searchForm = ref<SearchForm>({
  keyword: props.keyword,
  status: props.status,
  roleId: props.roleId
})

// 监听props变化，同步到searchForm
watch(
  () => [props.keyword, props.status, props.roleId],
  ([keyword, status, roleId]) => {
    searchForm.value = {
      keyword,
      status,
      roleId
    }
  }
)

const handleSearch = () => {
  // 更新父组件的v-model值
  if (searchForm.value.keyword !== undefined) {
    emit('update:keyword', searchForm.value.keyword)
  }
  if (searchForm.value.status !== undefined) {
    emit('update:status', searchForm.value.status)
  }
  if (searchForm.value.roleId !== undefined) {
    emit('update:roleId', searchForm.value.roleId)
  }
  emit('search')
}

const handleReset = () => {
  searchForm.value = {
    keyword: undefined,
    status: undefined,
    roleId: undefined
  }
  emit('update:keyword', '')
  emit('update:status', undefined)
  emit('update:roleId', undefined)
  emit('reset')
}

defineExpose({
  handleReset
})
</script>

<style scoped>
.search-bar {
  padding: 16px;
  margin-bottom: 16px;
  background: #fff;
  border-radius: 4px;
}

.search-form {
  margin: 0;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}
</style>
