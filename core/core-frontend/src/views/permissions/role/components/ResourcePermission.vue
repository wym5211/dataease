<template>
  <div class="resource-permission">
    <div class="toolbar">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="资源类型">
          <el-select v-model="resourceType" placeholder="全部类型" clearable style="width: 150px">
            <el-option label="仪表板" value="dashboard" />
            <el-option label="数据集" value="dataset" />
            <el-option label="数据源" value="datasource" />
            <el-option label="大屏" value="screen" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchKey" placeholder="搜索资源名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="selectedRole"
        :title="`当前角色：${selectedRole.name}`"
        type="info"
        :closable="false"
        show-icon
      />
    </div>

    <el-table
      v-loading="loading"
      :data="filteredResourceList"
      style="width: 100%"
      row-key="id"
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="name" label="资源名称" min-width="200" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.type)">
            {{ getTypeLabel(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路径" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作权限" width="300">
        <template #default="{ row }">
          <el-checkbox-group
            v-model="row.permissions"
            size="small"
            @change="handlePermissionChange"
          >
            <el-checkbox label="view">查看</el-checkbox>
            <el-checkbox label="edit">编辑</el-checkbox>
            <el-checkbox label="delete">删除</el-checkbox>
            <el-checkbox label="share">分享</el-checkbox>
          </el-checkbox-group>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import type { ResourcePermission, Role } from '../types'

interface Props {
  role?: Role | null
}

interface Emits {
  (e: 'change', data: ResourcePermission[]): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const loading = ref(false)
const resourceType = ref('')
const searchKey = ref('')
const selectedResources = ref<ResourcePermission[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 模拟资源数据
const resourceList = ref<ResourcePermission[]>([
  {
    id: '1',
    name: '销售分析仪表板',
    type: 'dashboard',
    path: '/dashboard/sales',
    permissions: ['view']
  },
  {
    id: '2',
    name: '用户增长报表',
    type: 'dashboard',
    path: '/dashboard/growth',
    permissions: ['view', 'edit']
  },
  { id: '3', name: '订单数据集', type: 'dataset', path: '/dataset/orders', permissions: ['view'] },
  {
    id: '4',
    name: 'MySQL数据源',
    type: 'datasource',
    path: '/datasource/mysql',
    permissions: ['view', 'edit', 'delete']
  },
  {
    id: '5',
    name: '年度大屏',
    type: 'screen',
    path: '/screen/yearly',
    permissions: ['view', 'share']
  }
])

// 过滤后的列表（无副作用）
const filteredList = computed(() => {
  let list = resourceList.value

  if (resourceType.value) {
    list = list.filter(item => item.type === resourceType.value)
  }

  if (searchKey.value) {
    const keyword = searchKey.value.toLowerCase()
    list = list.filter(item => item.name.toLowerCase().includes(keyword))
  }

  return list
})

// 分页后的列表
const filteredResourceList = computed(() => {
  const list = filteredList.value
  const start = (pagination.page - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return list.slice(start, end)
})

// 使用watch更新总数
watch(
  filteredList,
  list => {
    pagination.total = list.length
  },
  { immediate: true }
)

const selectedRole = computed(() => props.role)

// 类型标签
const getTypeTag = (type: string) => {
  const map: Record<string, string> = {
    dashboard: 'primary',
    dataset: 'success',
    datasource: 'warning',
    screen: 'danger'
  }
  return map[type] || 'info'
}

// 类型标签
const getTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    dashboard: '仪表板',
    dataset: '数据集',
    datasource: '数据源',
    screen: '大屏'
  }
  return map[type] || type
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
}

// 重置
const handleReset = () => {
  resourceType.value = ''
  searchKey.value = ''
  pagination.page = 1
}

// 分页
const handlePageChange = (page: number) => {
  pagination.page = page
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.page = 1
}

// 选择变化
const handleSelectionChange = (selection: ResourcePermission[]) => {
  selectedResources.value = selection
}

// 权限变化
const handlePermissionChange = () => {
  emit(
    'change',
    resourceList.value.filter(item => item.permissions.length > 0)
  )
}

// 暴露方法
defineExpose({
  getPermissions: () => resourceList.value.filter(item => item.permissions.length > 0),
  setPermissions: (ids: string[]) => {
    resourceList.value.forEach(item => {
      if (ids.includes(item.id)) {
        item.permissions = ['view']
      }
    })
  }
})

// 监听role变化
watch(
  () => props.role,
  () => {
    // 加载角色已授权的权限
    if (props.role) {
      // TODO: 调用API加载权限
      loading.value = true
      setTimeout(() => {
        loading.value = false
      }, 500)
    }
  },
  { immediate: true }
)
</script>

<style scoped lang="less">
.resource-permission {
  padding: 16px;

  .toolbar {
    margin-bottom: 16px;

    .filter-form {
      margin-bottom: 16px;
    }

    .el-alert {
      margin-bottom: 16px;
    }
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
