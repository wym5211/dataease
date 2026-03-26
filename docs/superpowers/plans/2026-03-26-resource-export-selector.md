# 资源导出选择器实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为资源导出功能添加选择器，让用户可以选择具体导出哪些资源（包括目录）

**Architecture:** 创建独立的 ResourceSelectDialog 组件，修改 ExportImportDialog 集成选择器和依赖检测功能

**Tech Stack:** Vue 3, TypeScript, Element Plus, Spring Boot, Java 21

---

## 文件结构

### 新增文件

| 文件 | 职责 |
|------|------|
| `core/core-frontend/src/components/backup/ResourceSelectDialog.vue` | 资源选择弹窗组件 |
| `core/core-frontend/src/api/resourceTree.ts` | 资源树 API 封装 |
| `sdk/common/src/main/java/io/dataease/model/backup/DependencyInfo.java` | 依赖信息 DTO |

### 修改文件

| 文件 | 修改内容 |
|------|----------|
| `core/core-frontend/src/components/backup/ExportImportDialog.vue` | 集成选择器、添加依赖确认对话框 |
| `core/core-frontend/src/api/backup.ts` | 添加依赖检测 API |
| `core/core-frontend/src/locales/zh-CN.ts` | 添加中文文案 |
| `core/core-frontend/src/locales/en.ts` | 添加英文文案 |
| `core/core-frontend/src/locales/tw.ts` | 添加繁体中文文案 |
| `sdk/api/api-base/src/main/java/io/dataease/api/backup/BackupCenterApi.java` | 添加依赖检测接口定义 |
| `core/core-backend/src/main/java/io/dataease/backup/server/BackupCenterServer.java` | 实现依赖检测接口 |
| `core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java` | 添加依赖检测逻辑 |

---

## Task 1: 添加国际化文案

**Files:**
- Modify: `core/core-frontend/src/locales/zh-CN.ts`
- Modify: `core/core-frontend/src/locales/en.ts`
- Modify: `core/core-frontend/src/locales/tw.ts`

- [ ] **Step 1: 在 zh-CN.ts 的 backup 对象中添加新文案**

找到 `backup:` 对象（约第 733 行），在现有内容后添加：

```typescript
select_resource_title: '选择要导出的{type}',
selected_count: '已选择 {count} 个资源',
no_selection: '请至少选择一个资源',
no_resources: '暂无可导出的资源',
dependency_detected: '检测到依赖资源',
dependency_desc: '您选择的{type}依赖以下资源，是否一并导出？',
dependency_check_failed: '依赖检测失败，将仅导出所选资源',
export_selected_only: '仅导出所选',
export_all: '全部导出',
search_resource: '搜索资源',
confirm_export: '确认导出'
```

- [ ] **Step 2: 在 en.ts 的 backup 对象中添加英文文案**

```typescript
select_resource_title: 'Select {type} to export',
selected_count: '{count} resources selected',
no_selection: 'Please select at least one resource',
no_resources: 'No resources available for export',
dependency_detected: 'Dependencies detected',
dependency_desc: 'The {type} you selected depends on the following resources. Export them together?',
dependency_check_failed: 'Dependency check failed. Only selected resources will be exported',
export_selected_only: 'Export selected only',
export_all: 'Export all',
search_resource: 'Search resources',
confirm_export: 'Confirm export'
```

- [ ] **Step 3: 在 tw.ts 的 backup 对象中添加繁体中文文案**

```typescript
select_resource_title: '選擇要匯出的{type}',
selected_count: '已選擇 {count} 個資源',
no_selection: '請至少選擇一個資源',
no_resources: '暫無可匯出的資源',
dependency_detected: '檢測到依賴資源',
dependency_desc: '您選擇的{type}依賴以下資源，是否一併匯出？',
dependency_check_failed: '依賴檢測失敗，將僅匯出所選資源',
export_selected_only: '僅匯出所選',
export_all: '全部匯出',
search_resource: '搜尋資源',
confirm_export: '確認匯出'
```

- [ ] **Step 4: 提交国际化更改**

```bash
git add core/core-frontend/src/locales/zh-CN.ts core/core-frontend/src/locales/en.ts core/core-frontend/src/locales/tw.ts
git commit -m "feat(backup): add i18n texts for resource selector"
```

---

## Task 2: 添加资源树 API

**Files:**
- Create: `core/core-frontend/src/api/resourceTree.ts`
- Modify: `core/core-frontend/src/api/backup.ts`

- [ ] **Step 1: 创建 resourceTree.ts API 文件**

```typescript
import request from '@/config/axios'

export interface TreeNode {
  id: string
  name: string
  leaf?: boolean
  children?: TreeNode[]
}

// 获取数据源列表（平铺，无树结构）
export const getDatasourceList = (): Promise<IResponse> => {
  return request.post({ url: '/datasource/list', data: {} })
}

// 获取数据集树
export const getDatasetTree = (): Promise<IResponse> => {
  return request.post({ url: '/dataset/tree', data: {} })
}

// 获取仪表板树
export const getDashboardTree = (): Promise<IResponse> => {
  return request.post({ url: '/dataVisualization/tree', data: { busiFlag: 'dashboard' } })
}

// 获取数据大屏树
export const getDataviewTree = (): Promise<IResponse> => {
  return request.post({ url: '/dataVisualization/tree', data: { busiFlag: 'dataV' } })
}
```

- [ ] **Step 2: 在 backup.ts 中添加依赖检测 API**

在文件末尾添加：

```typescript
// 依赖信息接口
export interface DependencyInfo {
  hasDependencies: boolean
  dependencies: {
    datasources: { id: string; name: string }[]
    datasets: { id: string; name: string }[]
  }
}

// 检测资源依赖
export const checkDependencies = (
  type: 'dataset' | 'dashboard' | 'dataview',
  resourceIds: string[]
): Promise<IResponse<DependencyInfo>> => {
  return request.post({
    url: '/backupCenter/checkDependencies',
    data: { type, resourceIds }
  })
}
```

- [ ] **Step 3: 提交 API 更改**

```bash
git add core/core-frontend/src/api/resourceTree.ts core/core-frontend/src/api/backup.ts
git commit -m "feat(backup): add resource tree and dependency check APIs"
```

---

## Task 3: 创建 ResourceSelectDialog 组件

**Files:**
- Create: `core/core-frontend/src/components/backup/ResourceSelectDialog.vue`

- [ ] **Step 1: 创建 ResourceSelectDialog.vue 组件**

```vue
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
        // 数据源是平铺列表，转换为树结构
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

// 从返回的根节点中提取 children
const extractTreeChildren = (data: any): TreeNode[] => {
  if (!data) return []
  // 如果返回的是包含 root 节点的结构，提取其 children
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
```

- [ ] **Step 2: 提交组件**

```bash
git add core/core-frontend/src/components/backup/ResourceSelectDialog.vue
git commit -m "feat(backup): add ResourceSelectDialog component"
```

---

## Task 4: 修改 ExportImportDialog 集成选择器

**Files:**
- Modify: `core/core-frontend/src/components/backup/ExportImportDialog.vue`

- [ ] **Step 1: 修改 template，移除内联选择器，添加资源选择器和依赖确认对话框**

替换整个 template 部分：

```vue
<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="backup-dialog-content">
      <!-- 模式切换 -->
      <el-tabs v-model="activeTab" class="backup-tabs">
        <el-tab-pane :label="t('commons.export')" name="export" />
        <el-tab-pane :label="t('commons.import')" name="import" />
      </el-tabs>

      <!-- 导出面板 -->
      <div v-show="activeTab === 'export'" class="export-panel">
        <el-form :model="exportForm" label-position="top">
          <el-form-item :label="t('backup.export_type')">
            <el-select v-model="exportForm.type" :placeholder="t('backup.select_export_type')" style="width: 100%">
              <el-option :label="t('backup.datasource')" value="datasource" />
              <el-option :label="t('backup.dataset')" value="dataset" />
              <el-option :label="t('backup.dashboard')" value="dashboard" />
              <el-option :label="t('backup.dataview')" value="dataview" />
              <el-option :label="t('backup.combined')" value="combined" />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('backup.export_options')">
            <el-checkbox v-model="exportForm.options.compress">{{ t('backup.compress_file') }}</el-checkbox>
          </el-form-item>
        </el-form>
      </div>

      <!-- 导入面板 -->
      <div v-show="activeTab === 'import'" class="import-panel">
        <el-form :model="importForm" label-position="top">
          <el-form-item :label="t('backup.select_import_file')">
            <el-upload
              ref="uploadRef"
              class="backup-upload"
              :auto-upload="false"
              accept=".debk,.json,.zip"
              v-model:file-list="fileList"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
            >
              <template #trigger>
                <el-button icon="Upload">{{ t('backup.select_file') }}</el-button>
              </template>
              <template #tip>
                <div class="el-upload__tip">{{ t('backup.file_format_tip') }}</div>
              </template>
            </el-upload>
          </el-form-item>

          <!-- 文件预览 -->
          <div v-if="previewData" class="preview-info">
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="t('backup.version')">{{ previewData.version }}</el-descriptions-item>
              <el-descriptions-item :label="t('backup.type')">{{
                getTypeName(previewData.type)
              }}</el-descriptions-item>
              <el-descriptions-item :label="t('backup.export_time')">{{
                formatTime(previewData.exportTime)
              }}</el-descriptions-item>
              <el-descriptions-item :label="t('backup.include_count')">
                <span v-if="previewData.datasources?.length"
                  >{{ t('backup.datasource') }}: {{ previewData.datasources.length }}</span
                >
                <span v-if="previewData.datasets?.length">
                  {{ t('backup.dataset') }}: {{ previewData.datasets.length }}</span
                >
                <span v-if="previewData.dashboards?.length">
                  {{ t('backup.dashboard') }}: {{ previewData.dashboards.length }}</span
                >
                <span v-if="previewData.dataviews?.length">
                  {{ t('backup.dataview') }}: {{ previewData.dataviews.length }}</span
                >
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <el-form-item :label="t('backup.import_mode')">
            <el-radio-group v-model="importForm.overwrite">
              <el-radio :label="false">{{ t('backup.create_new') }}</el-radio>
              <el-radio :label="true">{{ t('backup.overwrite_same') }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">{{ t('commons.cancel') }}</el-button>
      <el-button
        v-show="activeTab === 'export'"
        type="primary"
        @click="handleExport"
        :loading="loading"
      >
        {{ t('commons.export') }}
      </el-button>
      <el-button
        v-show="activeTab === 'import'"
        type="primary"
        @click="handleImport"
        :loading="loading"
      >
        {{ t('commons.import') }}
      </el-button>
    </template>
  </el-dialog>

  <!-- 资源选择弹窗 -->
  <ResourceSelectDialog ref="resourceSelectDialog" @confirm="onResourceSelected" />

  <!-- 依赖确认弹窗 -->
  <el-dialog
    v-model="dependencyDialogVisible"
    :title="t('backup.dependency_detected')"
    width="500px"
    :close-on-click-modal="false"
  >
    <p>{{ t('backup.dependency_desc', { type: getTypeName(exportForm.type) }) }}</p>
    <el-checkbox-group v-model="selectedDependencyIds" style="margin-top: 16px">
      <div v-for="ds in dependencyInfo?.dependencies?.datasources" :key="ds.id" style="margin-bottom: 8px">
        <el-checkbox :label="ds.id">{{ ds.name }} ({{ t('backup.datasource') }})</el-checkbox>
      </div>
      <div v-for="dt in dependencyInfo?.dependencies?.datasets" :key="dt.id" style="margin-bottom: 8px">
        <el-checkbox :label="dt.id">{{ dt.name }} ({{ t('backup.dataset') }})</el-checkbox>
      </div>
    </el-checkbox-group>
    <template #footer>
      <el-button @click="dependencyDialogVisible = false">{{ t('commons.cancel') }}</el-button>
      <el-button @click="onDependencyConfirm(false)">{{ t('backup.export_selected_only') }}</el-button>
      <el-button type="primary" @click="onDependencyConfirm(true)">{{ t('backup.export_all') }}</el-button>
    </template>
  </el-dialog>
</template>
```

- [ ] **Step 2: 修改 script 部分**

```vue
<script lang="ts" setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus-secondary'
import { useI18n } from '@/hooks/web/useI18n'
import { exportData, importData, previewBackup, uploadBackup, downloadBackup, checkDependencies, type DependencyInfo } from '@/api/backup'
import ResourceSelectDialog from './ResourceSelectDialog.vue'

const { t } = useI18n()

const visible = ref(false)
const loading = ref(false)
const activeTab = ref('export')
const uploadRef = ref(null)
const previewData = ref(null)
const fileList = ref([])
const resourceSelectDialog = ref()

// 依赖确认相关
const dependencyDialogVisible = ref(false)
const pendingSelectedIds = ref<string[]>([])
const dependencyInfo = ref<DependencyInfo | null>(null)
const selectedDependencyIds = ref<string[]>([])

const exportForm = ref({
  type: 'datasource' as 'datasource' | 'dataset' | 'dashboard' | 'dataview' | 'combined',
  resourceIds: [] as string[],
  options: {
    compress: true
  }
})

const importForm = ref({
  overwrite: false,
  file: null as File | null
})

const dialogTitle = computed(() => {
  return activeTab.value === 'export' ? t('backup.export_resource') : t('backup.import_resource')
})

watch(activeTab, () => {
  previewData.value = null
  fileList.value = []
})

const handleExport = async () => {
  if (exportForm.value.type === 'combined') {
    doExport([])
  } else {
    resourceSelectDialog.value?.open(exportForm.value.type)
  }
}

const onResourceSelected = async (selectedIds: string[]) => {
  if (exportForm.value.type === 'datasource') {
    doExport(selectedIds)
  } else {
    try {
      loading.value = true
      const result = await checkDependencies(
        exportForm.value.type as 'dataset' | 'dashboard' | 'dataview',
        selectedIds
      )
      if (result.data?.hasDependencies) {
        pendingSelectedIds.value = selectedIds
        dependencyInfo.value = result.data
        // 默认全选依赖项
        selectedDependencyIds.value = [
          ...result.data.dependencies.datasources.map(d => d.id),
          ...result.data.dependencies.datasets.map(d => d.id)
        ]
        dependencyDialogVisible.value = true
      } else {
        doExport(selectedIds)
      }
    } catch (e) {
      console.error('Failed to check dependencies:', e)
      // 检测失败时直接导出所选资源
      ElMessage.warning(t('backup.dependency_check_failed'))
      doExport(selectedIds)
    } finally {
      loading.value = false
    }
  }
}

const onDependencyConfirm = (includeDeps: boolean) => {
  dependencyDialogVisible.value = false
  if (includeDeps && dependencyInfo.value) {
    const allIds = [
      ...pendingSelectedIds.value,
      ...selectedDependencyIds.value
    ]
    doExport(allIds)
  } else {
    doExport(pendingSelectedIds.value)
  }
}

const doExport = async (resourceIds: string[]) => {
  try {
    loading.value = true
    const result = await exportData({
      type: exportForm.value.type,
      resourceIds,
      options: exportForm.value.options
    })

    if (result.code === 0 && result.data) {
      ElMessage.success(t('backup.export_success'))
      downloadBackup(result.data.id)
      handleClose()
    } else {
      ElMessage.error(result.msg || t('backup.export_failed'))
    }
  } catch (err: unknown) {
    const e = err as Error
    ElMessage.error(e.message || t('backup.export_failed'))
  } finally {
    loading.value = false
  }
}

const handleImport = async () => {
  if (!importForm.value.file) {
    ElMessage.warning(t('backup.select_import_file_warning'))
    return
  }

  try {
    loading.value = true

    const uploadResult = await uploadBackup(importForm.value.file)
    if (uploadResult.status !== 'success') {
      ElMessage.error(uploadResult.message || t('backup.upload_failed'))
      return
    }

    const importId = uploadResult.id
    if (!importId) {
      ElMessage.error(t('backup.upload_failed_no_id'))
      return
    }

    const result = await importData({
      id: importId,
      type: previewData.value?.type || 'combined',
      overwrite: importForm.value.overwrite
    })

    if (result.status === 'success') {
      ElMessage.success(t('backup.import_success'))
      handleClose()
    } else {
      ElMessage.error(result.message || t('backup.import_failed'))
    }
  } catch (err: unknown) {
    const e = err as Error
    ElMessage.error(e.message || t('backup.import_failed'))
  } finally {
    loading.value = false
  }
}

const handleFileChange = async (uploadFile, uploadFiles) => {
  if (uploadFiles && uploadFiles.length > 1) {
    fileList.value = [uploadFile]
  }

  const file = uploadFile.raw
  importForm.value.file = file

  try {
    const result = await previewBackup(file)
    if (result && result.version) {
      previewData.value = result
    } else {
      ElMessage.error(t('backup.preview_failed'))
    }
  } catch (err: unknown) {
    const e = err as Error
    ElMessage.error(t('backup.preview_failed') + ': ' + e.message)
  }
}

const handleFileRemove = () => {
  importForm.value.file = null
  previewData.value = null
  fileList.value = []
}

const getTypeName = (type: string) => {
  const typeMap: Record<string, string> = {
    datasource: t('backup.datasource'),
    dataset: t('backup.dataset'),
    dashboard: t('backup.dashboard'),
    dataview: t('backup.dataview'),
    combined: t('backup.combined')
  }
  return typeMap[type] || type
}

const formatTime = (timestamp: number) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString()
}

const handleClose = () => {
  visible.value = false
  activeTab.value = 'export'
  exportForm.value = {
    type: 'datasource',
    resourceIds: [],
    options: { compress: true }
  }
  importForm.value = {
    overwrite: false,
    file: null
  }
  previewData.value = null
  fileList.value = []
  pendingSelectedIds.value = []
  dependencyInfo.value = null
  selectedDependencyIds.value = []
}

const open = () => {
  visible.value = true
}

defineExpose({
  open
})
</script>
```

- [ ] **Step 3: 保持 style 部分不变**

- [ ] **Step 4: 提交更改**

```bash
git add core/core-frontend/src/components/backup/ExportImportDialog.vue
git commit -m "feat(backup): integrate ResourceSelectDialog and dependency check"
```

---

## Task 5: 后端添加依赖检测接口

**Files:**
- Create: `sdk/common/src/main/java/io/dataease/model/backup/DependencyInfo.java`
- Modify: `sdk/api/api-base/src/main/java/io/dataease/api/backup/BackupCenterApi.java`
- Modify: `core/core-backend/src/main/java/io/dataease/backup/server/BackupCenterServer.java`
- Modify: `core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java`

- [ ] **Step 1: 创建 DependencyInfo DTO 类**

文件路径: `sdk/common/src/main/java/io/dataease/model/backup/DependencyInfo.java`

```java
package io.dataease.model.backup;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class DependencyInfo {
    private boolean hasDependencies;
    private Dependencies dependencies;

    public DependencyInfo() {
        this.dependencies = new Dependencies();
    }

    @Data
    public static class Dependencies {
        private List<ResourceItem> datasources = new ArrayList<>();
        private List<ResourceItem> datasets = new ArrayList<>();
    }

    @Data
    public static class ResourceItem {
        private String id;
        private String name;

        public ResourceItem() {}

        public ResourceItem(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResourceItem that = (ResourceItem) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}
```

- [ ] **Step 2: 在 BackupCenterApi.java 接口中添加方法**

在 `BackupCenterApi.java` 文件末尾（`}` 之前）添加：

```java
@Operation(summary = "检测资源依赖")
@PostMapping("/checkDependencies")
DependencyInfo checkDependencies(@RequestBody Map<String, Object> request);
```

需要添加 import:
```java
import io.dataease.model.backup.DependencyInfo;
```

- [ ] **Step 3: 在 BackupCenterServer.java 中实现接口**

在类中添加：

```java
@Override
@Operation(summary = "检测资源依赖")
@PostMapping("/checkDependencies")
public DependencyInfo checkDependencies(@RequestBody Map<String, Object> request) {
    String type = (String) request.get("type");
    @SuppressWarnings("unchecked")
    List<String> resourceIds = (List<String>) request.get("resourceIds");
    return backupCenterManage.checkDependencies(type, resourceIds);
}
```

需要添加 imports:
```java
import io.dataease.model.backup.DependencyInfo;
import java.util.Map;
```

- [ ] **Step 4: 在 BackupCenterManage.java 中添加检测逻辑**

添加以下内容：

1. 添加 import:
```java
import io.dataease.model.backup.DependencyInfo;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.HashSet;
```

2. 添加依赖注入:
```java
@Autowired
private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
```

3. 添加检测方法:
```java
public DependencyInfo checkDependencies(String type, List<String> resourceIds) {
    DependencyInfo result = new DependencyInfo();
    DependencyInfo.Dependencies dependencies = result.getDependencies();

    if (resourceIds == null || resourceIds.isEmpty()) {
        result.setHasDependencies(false);
        return result;
    }

    switch (type) {
        case "dataset":
            // 查询数据集关联的数据源
            List<DependencyInfo.ResourceItem> datasources = queryDatasetDatasources(resourceIds);
            dependencies.getDatasources().addAll(datasources);
            break;
        case "dashboard":
        case "dataview":
            // 查询仪表板/大屏关联的数据集
            List<DependencyInfo.ResourceItem> datasets = queryDashboardDatasets(resourceIds);
            dependencies.getDatasets().addAll(datasets);
            // 递归查询数据集关联的数据源
            if (!datasets.isEmpty()) {
                List<String> datasetIds = datasets.stream()
                    .map(DependencyInfo.ResourceItem::getId)
                    .distinct()
                    .collect(Collectors.toList());
                List<DependencyInfo.ResourceItem> ds = queryDatasetDatasources(datasetIds);
                dependencies.getDatasources().addAll(ds);
            }
            break;
        default:
            break;
    }

    // 去重
    dependencies.setDatasources(new ArrayList<>(new HashSet<>(dependencies.getDatasources())));
    dependencies.setDatasets(new ArrayList<>(new HashSet<>(dependencies.getDatasets())));

    result.setHasDependencies(
        !dependencies.getDatasources().isEmpty() || !dependencies.getDatasets().isEmpty()
    );
    return result;
}

private List<DependencyInfo.ResourceItem> queryDatasetDatasources(List<String> datasetIds) {
    if (datasetIds == null || datasetIds.isEmpty()) {
        return Collections.emptyList();
    }
    try {
        String sql = "SELECT DISTINCT d.id, d.name FROM core_datasource d " +
                     "INNER JOIN core_dataset_table dt ON d.id = dt.datasource_id " +
                     "WHERE dt.id IN (:ids)";
        return namedParameterJdbcTemplate.query(
            sql,
            Map.of("ids", datasetIds),
            (rs, rowNum) -> new DependencyInfo.ResourceItem(rs.getString("id"), rs.getString("name"))
        );
    } catch (Exception e) {
        return Collections.emptyList();
    }
}

private List<DependencyInfo.ResourceItem> queryDashboardDatasets(List<String> dashboardIds) {
    if (dashboardIds == null || dashboardIds.isEmpty()) {
        return Collections.emptyList();
    }
    try {
        // 根据实际的图表-数据集关联表查询
        String sql = "SELECT DISTINCT dt.id, dt.name FROM core_dataset_table dt " +
                     "INNER JOIN core_chart_view cv ON dt.id = cv.table_id " +
                     "WHERE cv.scene_id IN (:ids)";
        return namedParameterJdbcTemplate.query(
            sql,
            Map.of("ids", dashboardIds),
            (rs, rowNum) -> new DependencyInfo.ResourceItem(rs.getString("id"), rs.getString("name"))
        );
    } catch (Exception e) {
        return Collections.emptyList();
    }
}
```

- [ ] **Step 5: 提交后端更改**

```bash
git add sdk/common/src/main/java/io/dataease/model/backup/DependencyInfo.java
git add sdk/api/api-base/src/main/java/io/dataease/api/backup/BackupCenterApi.java
git add core/core-backend/src/main/java/io/dataease/backup/server/BackupCenterServer.java
git add core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java
git commit -m "feat(backup): add dependency check API"
```

---

## Task 6: 最终集成测试

**Files:**
- 无新文件

- [ ] **Step 1: 启动前端开发服务器**

```bash
cd core/core-frontend
npm run dev:win
```

- [ ] **Step 2: 启动后端服务**

```bash
cd core/core-backend
mvn clean package -DskipTests
java -jar target/CoreApplication.jar --spring.profiles.active=standalone
```

- [ ] **Step 3: 测试各类型资源导出**

1. 测试数据源导出：选择数据源类型 → 点击导出 → 选择资源 → 确认导出
2. 测试数据集导出：选择数据集类型 → 点击导出 → 选择资源 → 检测依赖 → 确认导出
3. 测试仪表板导出：选择仪表板类型 → 点击导出 → 选择资源 → 检测依赖 → 确认导出
4. 测试组合导出：选择组合导出类型 → 点击导出 → 直接导出

- [ ] **Step 4: 提交最终更改（如有）**

```bash
git status
git add -A
git commit -m "fix(backup): fix issues found in testing"
```

---

## 执行顺序总结

1. Task 1: 国际化文案
2. Task 2: 前端 API
3. Task 3: ResourceSelectDialog 组件
4. Task 4: 修改 ExportImportDialog
5. Task 5: 后端依赖检测接口
6. Task 6: 集成测试

每个 Task 应该独立提交，便于回滚和审查。
