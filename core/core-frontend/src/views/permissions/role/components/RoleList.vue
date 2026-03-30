<template>
  <div class="role-list">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索角色名称/编码"
            clearable
            style="width: 200px"
            @clear="handleSearch"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="全部状态"
            clearable
            style="width: 120px"
            @change="handleSearch"
          >
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作按钮 -->
    <div class="toolbar">
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        创建角色
      </el-button>
    </div>

    <!-- 角色表格 -->
    <el-table
      v-loading="loading"
      :data="roleList"
      border
      stripe
      style="width: 100%"
      :header-cell-style="{ background: '#f5f6f7' }"
    >
      <el-table-column prop="name" label="角色名称" min-width="150" />
      <el-table-column prop="code" label="角色编码" min-width="150" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-space>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus-secondary'
import { getRoleList, deleteRole } from '../api'
import type { Role, RoleListRequest } from '../types'

interface Emits {
  (e: 'create'): void
  (e: 'edit', role: Role): void
  (e: 'delete-success'): void
}

const emit = defineEmits<Emits>()

// 搜索表单
const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined
})

// 加载状态
const loading = ref(false)

// 角色列表
const roleList = ref<Role[]>([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 加载角色列表
const loadRoleList = async () => {
  loading.value = true
  try {
    const params: RoleListRequest = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      status: searchForm.status
    }
    const res = await getRoleList(params)
    roleList.value = res.records
    pagination.total = res.total
  } catch (error) {
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadRoleList()
}

// 重置
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = undefined
  pagination.page = 1
  loadRoleList()
}

// 分页变化
const handlePageChange = (page: number) => {
  pagination.page = page
  loadRoleList()
}

// 每页条数变化
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.page = 1
  loadRoleList()
}

// 创建角色
const handleCreate = () => {
  emit('create')
}

// 编辑角色
const handleEdit = (role: Role) => {
  emit('edit', role)
}

// 删除角色
const handleDelete = async (role: Role) => {
  try {
    await ElMessageBox.confirm(`确定要删除角色 "${role.name}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteRole(role.id)
    ElMessage.success('删除成功')
    emit('delete-success')
    loadRoleList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 暴露刷新方法
const refresh = () => {
  loadRoleList()
}

defineExpose({
  refresh
})

onMounted(() => {
  loadRoleList()
})
</script>

<style scoped>
.role-list {
  .search-bar {
    padding: 16px;
    margin-bottom: 16px;
    background: #fff;
    border-radius: 4px;
  }

  .search-form {
    margin: 0;
  }

  .toolbar {
    margin-bottom: 16px;

    .el-button {
      margin-right: 8px;
    }
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    padding: 16px 0;
  }
}
</style>
