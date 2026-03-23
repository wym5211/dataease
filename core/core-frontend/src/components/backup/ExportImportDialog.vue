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
        <el-tab-pane label="导出" name="export" />
        <el-tab-pane label="导入" name="import" />
      </el-tabs>

      <!-- 导出面板 -->
      <div v-show="activeTab === 'export'" class="export-panel">
        <el-form :model="exportForm" label-position="top">
          <el-form-item label="导出类型">
            <el-select v-model="exportForm.type" placeholder="请选择导出类型" style="width: 100%">
              <el-option label="数据源" value="datasource" />
              <el-option label="数据集" value="dataset" />
              <el-option label="仪表板" value="dashboard" />
              <el-option label="数据大屏" value="dataview" />
              <el-option label="完整导出（包含所有）" value="combined" />
            </el-select>
          </el-form-item>

          <el-form-item v-if="exportForm.type === 'datasource'" label="选择数据源">
            <el-tree-select
              v-model="exportForm.resourceIds"
              :data="datasourceTree"
              :props="{ label: 'name', children: 'children', value: 'id' }"
              multiple
              check-strictly
              placeholder="请选择要导出的数据源"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item v-if="exportForm.type === 'dataset'" label="选择数据集">
            <el-tree-select
              v-model="exportForm.resourceIds"
              :data="datasetTree"
              :props="{ label: 'name', children: 'children', value: 'id' }"
              multiple
              check-strictly
              placeholder="请选择要导出的数据集"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="导出选项">
            <el-checkbox v-model="exportForm.options.compress">压缩文件</el-checkbox>
          </el-form-item>
        </el-form>
      </div>

      <!-- 导入面板 -->
      <div v-show="activeTab === 'import'" class="import-panel">
        <el-form :model="importForm" label-position="top">
          <el-form-item label="选择导入文件">
            <el-upload
              ref="uploadRef"
              class="backup-upload"
              :auto-upload="false"
              :limit="1"
              accept=".debk,.json,.zip"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
            >
              <template #trigger>
                <el-button icon="Upload">选择文件</el-button>
              </template>
              <template #tip>
                <div class="el-upload__tip">支持 .debk、.json、.zip 格式</div>
              </template>
            </el-upload>
          </el-form-item>

          <!-- 文件预览 -->
          <div v-if="previewData" class="preview-info">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="版本">{{ previewData.version }}</el-descriptions-item>
              <el-descriptions-item label="类型">{{
                getTypeName(previewData.type)
              }}</el-descriptions-item>
              <el-descriptions-item label="导出时间">{{
                formatTime(previewData.exportTime)
              }}</el-descriptions-item>
              <el-descriptions-item label="包含数量">
                <span v-if="previewData.datasources?.length"
                  >数据源: {{ previewData.datasources.length }}</span
                >
                <span v-if="previewData.datasets?.length">
                  数据集: {{ previewData.datasets.length }}</span
                >
                <span v-if="previewData.dashboards?.length">
                  仪表板: {{ previewData.dashboards.length }}</span
                >
                <span v-if="previewData.dataviews?.length">
                  大屏: {{ previewData.dataviews.length }}</span
                >
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <el-form-item label="导入模式">
            <el-radio-group v-model="importForm.overwrite">
              <el-radio :label="false">创建新资源（重名自动重命名）</el-radio>
              <el-radio :label="true">覆盖同名资源</el-radio>
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
</template>

<script lang="ts" setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus-secondary'
import { useI18n } from '@/hooks/web/useI18n'
import { exportData, importData, previewBackup, uploadBackup, downloadBackup } from '@/api/backup'
import { listDatasources } from '@/api/datasource'

const { t } = useI18n()

const visible = ref(false)
const loading = ref(false)
const activeTab = ref('export')
const uploadRef = ref(null)
const previewData = ref(null)

const exportForm = ref({
  type: 'datasource',
  resourceIds: [] as string[],
  options: {
    compress: true
  }
})

const importForm = ref({
  overwrite: false,
  file: null as File | null
})

const datasourceTree = ref([])
const datasetTree = ref([])

const dialogTitle = computed(() => {
  return activeTab.value === 'export' ? '导出资源' : '导入资源'
})

watch(activeTab, () => {
  previewData.value = null
})

const loadDatasourceTree = async () => {
  try {
    const data = await listDatasources({})
    datasourceTree.value = data || []
  } catch (e) {
    console.error('Failed to load datasource tree', e)
  }
}

const handleExport = async () => {
  try {
    loading.value = true
    const result = await exportData({
      type: exportForm.value.type,
      resourceIds: exportForm.value.resourceIds,
      options: exportForm.value.options
    })

    if (result.code === 0 && result.data) {
      ElMessage.success('导出成功')
      downloadBackup(result.data.id)
      handleClose()
    } else {
      ElMessage.error(result.msg || '导出失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  } finally {
    loading.value = false
  }
}

const handleImport = async () => {
  if (!importForm.value.file) {
    ElMessage.warning('请选择要导入的文件')
    return
  }

  try {
    loading.value = true

    // 先上传文件
    const uploadResult = await uploadBackup(importForm.value.file)
    if (uploadResult.code !== 0) {
      ElMessage.error(uploadResult.msg || '文件上传失败')
      return
    }

    // 执行导入
    const importId = uploadResult.data?.id
    if (!importId) {
      ElMessage.error('文件上传失败，未获取到文件ID')
      return
    }

    const result = await importData({
      id: importId,
      type: previewData.value?.type || 'combined',
      overwrite: importForm.value.overwrite
    })

    if (result.code === 0) {
      ElMessage.success('导入成功')
      handleClose()
    } else {
      ElMessage.error(result.msg || '导入失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '导入失败')
  } finally {
    loading.value = false
  }
}

const handleFileChange = async (file: any) => {
  importForm.value.file = file.raw

  try {
    const result = await previewBackup(file.raw)
    if (result.code === 0) {
      previewData.value = result.data
    } else {
      ElMessage.error(result.msg || '文件预览失败')
    }
  } catch (e: any) {
    ElMessage.error('文件预览失败: ' + e.message)
  }
}

const handleFileRemove = () => {
  importForm.value.file = null
  previewData.value = null
}

const getTypeName = (type: string) => {
  const typeMap: Record<string, string> = {
    datasource: '数据源',
    dataset: '数据集',
    dashboard: '仪表板',
    dataview: '数据大屏',
    combined: '完整导出'
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
}

const open = () => {
  visible.value = true
  loadDatasourceTree()
}

defineExpose({
  open
})
</script>

<style lang="scss" scoped>
.backup-dialog-content {
  min-height: 300px;
}

.backup-tabs {
  margin-bottom: 20px;
}

.export-panel,
.import-panel {
  padding: 0 10px;
}

.preview-info {
  margin: 16px 0;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.backup-upload {
  width: 100%;
}
</style>
