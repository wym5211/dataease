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

          <el-form-item v-if="exportForm.type === 'datasource'" :label="t('backup.select_datasource')">
            <el-tree-select
              v-model="exportForm.resourceIds"
              :data="datasourceTree"
              :props="{ label: 'name', children: 'children', value: 'id' }"
              multiple
              check-strictly
              :placeholder="t('backup.select_export_datasource')"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item v-if="exportForm.type === 'dataset'" :label="t('backup.select_dataset')">
            <el-tree-select
              v-model="exportForm.resourceIds"
              :data="datasetTree"
              :props="{ label: 'name', children: 'children', value: 'id' }"
              multiple
              check-strictly
              :placeholder="t('backup.select_export_dataset')"
              style="width: 100%"
            />
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
const fileList = ref([])

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
  return activeTab.value === 'export' ? t('backup.export_resource') : t('backup.import_resource')
})

watch(activeTab, () => {
  previewData.value = null
  fileList.value = []
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

    // 先上传文件
    const uploadResult = await uploadBackup(importForm.value.file)
    if (uploadResult.status !== 'success') {
      ElMessage.error(uploadResult.message || t('backup.upload_failed'))
      return
    }

    // 执行导入
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
  // 只保留最新选择的文件
  if (uploadFiles && uploadFiles.length > 1) {
    // 更新 v-model 绑定的 fileList 来替换旧文件
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
