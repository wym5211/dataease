<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="700px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <!-- 搜索框 -->
    <el-input
      v-model="filterText"
      :placeholder="t('backup.search_resource')"
      clearable
      style="margin-bottom: 16px"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>

    <!-- 资源树 -->
    <div class="tree-container" v-loading="loading">
      <el-scrollbar height="400px">
        <el-tree
          v-if="treeData.length > 0"
          ref="treeRef"
          :data="treeData"
          :props="treeProps"
          show-checkbox
          node-key="id"
          :filter-node-method="filterNode"
          :default-expand-all="false"
          @check="handleCheck"
        >
          <template #default="{ node, data }">
            <span class="custom-tree-node">
              <el-icon v-if="!data.leaf" style="margin-right: 6px">
                <Folder />
              </el-icon>
              <span>{{ node.label }}</span>
            </span>
          </template>
        </el-tree>
        <el-empty v-else :description="t('backup.no_resources')" />
      </el-scrollbar>
    </div>

    <!-- 底部状态栏 -->
    <div class="selection-status">
      {{ t('backup.selected_count', { count: selectedCount }) }}
    </div>

    <template #footer>
      <el-button @click="handleClose">{{ t('commons.cancel') }}</el-button>
      <el-button
        type="primary"
        :disabled="selectedCount === 0"
        @click="handleConfirm"
      >
        {{ t('backup.confirm_export') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, computed, watch } from 'vue'
import { Search, Folder } from '@element-plus/icons-vue'
import { useI18n } from '@/hooks/web/useI18n'
import { getDatasourceList, getDatasetTree, getDashboardTree, getDataviewTree } from '@/api/resourceTree'
import type { TreeNode } from '@/api/resourceTree'

const { t } = useI18n()

type ResourceType = 'datasource' | 'dataset' | 'dashboard' | 'dataview'

const visible = ref(false)
const loading = ref(false)
const filterText = ref('')
const treeRef = ref()
const treeData = ref<TreeNode[]>([])
const currentType = ref<ResourceType>('datasource')

const treeProps = {
  children: 'children',
  label: 'name'
}

const dialogTitle = computed(() => {
  const typeNames: Record<ResourceType, string> = {
    datasource: t('backup.datasource'),
    dataset: t('backup.dataset'),
    dashboard: t('backup.dashboard'),
    dataview: t('backup.dataview')
  }
  return t('backup.select_resource_title', { type: typeNames[currentType.value] })
})

const selectedCount = computed(() => {
  if (!treeRef.value) return 0
  const checkedNodes = treeRef.value.getCheckedNodes(true)
  return checkedNodes.length
})

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

const filterNode = (value: string, data: TreeNode) => {
  if (!value) return true
  return data.name?.toLowerCase().includes(value.toLowerCase())
}

const handleCheck = () => {
  // 触发 selectedCount 重新计算
}

const loadTreeData = async (type: ResourceType) => {
  loading.value = true
  try {
    let response: IResponse | null = null
    switch (type) {
      case 'datasource':
        response = await getDatasourceList()
        const list = (response?.data as any[]) || []
        treeData.value = list.map((item: any) => ({
          ...item,
          leaf: true
        }))
        break
      case 'dataset':
        response = await getDatasetTree()
        treeData.value = ((response?.data as any[]) || [])
        break
      case 'dashboard':
        response = await getDashboardTree()
        treeData.value = extractTreeChildren(response?.data)
        break
      case 'dataview':
        response = await getDataviewTree()
        treeData.value = extractTreeChildren(response?.data)
        break
    }
  } catch (e) {
    console.error('Failed to load tree data:', e)
    treeData.value = []
  } finally {
    loading.value = false
  }
}

const extractTreeChildren = (data: any): TreeNode[] => {
  if (!data) return []
  if (data.id === '0' && data.children) {
    return data.children
  }
  if (Array.isArray(data)) {
    return data
  }
  return []
}

const open = async (type: ResourceType) => {
  currentType.value = type
  visible.value = true
  filterText.value = ''
  treeData.value = []
  await loadTreeData(type)
}

const handleClose = () => {
  visible.value = false
  treeData.value = []
  filterText.value = ''
}

const emit = defineEmits<{
  (e: 'confirm', selectedIds: string[]): void
}>()

const handleConfirm = () => {
  if (!treeRef.value) return
  const checkedNodes = treeRef.value.getCheckedNodes(true) as TreeNode[]
  const selectedIds = checkedNodes.map((node: TreeNode) => node.id)
  emit('confirm', selectedIds)
  handleClose()
}

defineExpose({
  open
})
</script>

<style lang="scss" scoped>
.tree-container {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 12px;
}

.selection-status {
  margin-top: 12px;
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.custom-tree-node {
  display: flex;
  align-items: center;
}
</style>
