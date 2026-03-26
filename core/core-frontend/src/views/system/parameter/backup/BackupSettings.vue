<template>
  <div class="backup-settings">
    <el-alert
      v-if="!isAdmin"
      :title="t('backup.permission_title')"
      type="warning"
      :description="t('backup.permission_desc')"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />
    <div v-else class="backup-content">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card class="backup-card">
            <template #header>
              <div class="card-header">
                <span>{{ t('backup.export_resource') }}</span>
              </div>
            </template>
            <div class="card-body">
              <p class="description">{{ t('backup.export_desc') }}</p>
              <el-form :model="exportForm" label-position="top">
                <el-form-item :label="t('backup.export_type')">
                  <el-select
                    v-model="exportForm.type"
                    :placeholder="t('backup.select_export_type')"
                    style="width: 100%"
                  >
                    <el-option :label="t('backup.datasource')" value="datasource" />
                    <el-option :label="t('backup.dataset')" value="dataset" />
                    <el-option :label="t('backup.dashboard')" value="dashboard" />
                    <el-option :label="t('backup.dataview')" value="dataview" />
                    <el-option :label="t('backup.combined')" value="combined" />
                  </el-select>
                </el-form-item>
                <el-form-item :label="t('backup.export_options')">
                  <el-checkbox v-model="exportForm.options.compress">
                    {{ t('backup.compress_file') }}
                  </el-checkbox>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="exporting" @click="handleExport">
                    {{ t('commons.export') }}
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card class="backup-card">
            <template #header>
              <div class="card-header">
                <span>{{ t('backup.import_resource') }}</span>
              </div>
            </template>
            <div class="card-body">
              <p class="description">{{ t('backup.import_desc') }}</p>
              <el-form :model="importForm" label-position="top">
                <el-form-item :label="t('backup.select_import_file')">
                  <el-upload
                    ref="uploadRef"
                    class="backup-upload"
                    :auto-upload="false"
                    :limit="1"
                    accept=".debk,.json,.zip"
                    v-model:file-list="fileList"
                    :on-change="handleFileChange"
                    :on-remove="handleFileRemove"
                    :on-exceed="handleExceed"
                  >
                    <template #trigger>
                      <el-button icon="Upload">{{ t('backup.select_file') }}</el-button>
                    </template>
                  </el-upload>
                </el-form-item>

                <div v-if="previewData" class="preview-info">
                  <el-descriptions :column="2" border size="small">
                    <el-descriptions-item :label="t('backup.version')">{{
                      previewData.version
                    }}</el-descriptions-item>
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
                        | {{ t('backup.dataset') }}: {{ previewData.datasets.length }}</span
                      >
                      <span v-if="previewData.dashboards?.length">
                        | {{ t('backup.dashboard') }}: {{ previewData.dashboards.length }}</span
                      >
                      <span v-if="previewData.dataviews?.length">
                        | {{ t('backup.dataview') }}: {{ previewData.dataviews.length }}</span
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
                <el-form-item>
                  <el-button
                    type="primary"
                    :loading="importing"
                    :disabled="!importForm.file"
                    @click="handleImport"
                  >
                    {{ t('commons.import') }}
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row style="margin-top: 20px">
        <el-col :span="24">
          <el-card class="backup-card">
            <template #header>
              <div class="card-header">
                <span>{{ t('backup.export_history') }}</span>
                <el-button text @click="loadHistory">{{ t('commons.refresh') }}</el-button>
              </div>
            </template>
            <el-table :data="historyList" style="width: 100%">
              <el-table-column prop="fileName" :label="t('backup.file_name')" />
              <el-table-column prop="itemCount" :label="t('backup.resource_count')" width="100" />
              <el-table-column prop="exportTime" :label="t('backup.export_time')" width="180">
                <template #default="{ row }">
                  {{ formatTime(row.exportTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="message" :label="t('backup.status')" />
              <el-table-column :label="t('backup.operation')" width="100">
                <template #default="{ row }">
                  <el-button link type="primary" @click="handleDownload(row)">
                    {{ t('commons.download') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>

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
        <div
          v-for="ds in dependencyInfo?.dependencies?.datasources"
          :key="ds.id"
          style="margin-bottom: 8px"
        >
          <el-checkbox :label="ds.id">{{ ds.name }} ({{ t('backup.datasource') }})</el-checkbox>
        </div>
        <div
          v-for="dt in dependencyInfo?.dependencies?.datasets"
          :key="dt.id"
          style="margin-bottom: 8px"
        >
          <el-checkbox :label="dt.id">{{ dt.name }} ({{ t('backup.dataset') }})</el-checkbox>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="onDependencyConfirm(false)">{{
          t('backup.export_selected_only')
        }}</el-button>
        <el-button type="primary" @click="onDependencyConfirm(true)">{{
          t('backup.export_all')
        }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus-secondary'
import { useI18n } from '@/hooks/web/useI18n'
import { useUserStoreWithOut } from '@/store/modules/user'
import {
  exportData,
  importData,
  previewBackup,
  uploadBackup,
  downloadBackup,
  getBackupHistory,
  checkDependencies
} from '@/api/backup'
import type { BackupRequest, ExportPackage, DependencyInfo } from '@/api/backup'
import ResourceSelectDialog from '@/components/backup/ResourceSelectDialog.vue'

interface BackupHistoryItem {
  id: string
  fileName: string
  itemCount: number
  exportTime: number
  message?: string
}

interface ExportDataResponse {
  id: string
  fileName: string
}

interface UploadDataResponse {
  id: string
}

interface FileUploadRaw {
  raw: File
}

const { t } = useI18n()
const userStore = useUserStoreWithOut()

const isAdmin = computed(() => String(userStore.getUid) === '1')

const exportForm = ref<{ type: BackupRequest['type']; options: { compress: boolean } }>({
  type: 'datasource',
  options: {
    compress: true
  }
})

const importForm = ref({
  overwrite: false,
  file: null as File | null
})

const previewData = ref<ExportPackage | null>(null)
const historyList = ref<BackupHistoryItem[]>([])
const exporting = ref(false)
const importing = ref(false)
const uploadRef = ref()
const fileList = ref([])
const resourceSelectDialog = ref()
const dependencyDialogVisible = ref(false)
const dependencyInfo = ref<DependencyInfo | null>(null)
const pendingSelectedIds = ref<string[]>([])
const selectedDependencyIds = ref<string[]>([])

onMounted(() => {
  if (isAdmin.value) {
    loadHistory()
  }
})

const loadHistory = async () => {
  try {
    const res = await getBackupHistory()
    if (res.code === 0) {
      historyList.value = (res.data as BackupHistoryItem[]) || []
    }
  } catch (e) {
    console.error('Failed to load history', e)
  }
}

const handleExport = async () => {
  if (exportForm.value.type === 'combined') {
    doExport([])
  } else {
    resourceSelectDialog.value?.open(
      exportForm.value.type as 'datasource' | 'dataset' | 'dashboard' | 'dataview'
    )
  }
}

const onResourceSelected = async (selectedIds: string[]) => {
  if (exportForm.value.type === 'datasource') {
    doExport(selectedIds)
  } else {
    try {
      exporting.value = true
      const result = await checkDependencies(
        exportForm.value.type as 'dataset' | 'dashboard' | 'dataview',
        selectedIds
      )
      if (result.data?.hasDependencies) {
        pendingSelectedIds.value = selectedIds
        dependencyInfo.value = result.data
        selectedDependencyIds.value = [
          ...result.data.dependencies.datasources.map(d => d.id),
          ...result.data.dependencies.datasets.map(d => d.id)
        ]
        dependencyDialogVisible.value = true
      } else {
        doExport(selectedIds)
      }
    } catch (e) {
      console.error('Dependency check failed:', e)
      ElMessage.warning(t('backup.dependency_check_failed'))
      doExport(selectedIds)
    } finally {
      exporting.value = false
    }
  }
}

const onDependencyConfirm = (includeDeps: boolean) => {
  dependencyDialogVisible.value = false
  if (includeDeps && dependencyInfo.value) {
    const allIds = [...pendingSelectedIds.value, ...selectedDependencyIds.value]
    doExport(allIds)
  } else {
    doExport(pendingSelectedIds.value)
  }
  dependencyInfo.value = null
  pendingSelectedIds.value = []
  selectedDependencyIds.value = []
}

const doExport = async (resourceIds: string[]) => {
  try {
    exporting.value = true
    const result = await exportData({
      type: exportForm.value.type,
      resourceIds,
      options: exportForm.value.options
    })

    if (result.code === 0 && result.data) {
      ElMessage.success(t('backup.export_success'))
      const data = result.data as ExportDataResponse
      downloadBackup(data.id, data.fileName)
      loadHistory()
    } else {
      ElMessage.error(result.msg || t('backup.export_failed'))
    }
  } catch (err: unknown) {
    const e = err as Error
    ElMessage.error(e.message || t('backup.export_failed'))
  } finally {
    exporting.value = false
  }
}

const handleImport = async () => {
  if (!importForm.value.file) {
    ElMessage.warning(t('backup.select_import_file_warning'))
    return
  }

  try {
    importing.value = true

    const uploadResult = await uploadBackup(importForm.value.file)
    if (uploadResult.code !== 0) {
      ElMessage.error(uploadResult.msg || t('backup.upload_failed'))
      return
    }

    const importId = (uploadResult.data as UploadDataResponse)?.id
    if (!importId) {
      ElMessage.error(t('backup.upload_failed_no_id'))
      return
    }

    const result = await importData({
      id: importId,
      type: (previewData.value?.type as BackupRequest['type']) || 'combined',
      overwrite: importForm.value.overwrite
    })

    if (result.code === 0) {
      ElMessage.success(t('backup.import_success'))
      importForm.value.file = null
      previewData.value = null
      uploadRef.value?.clearFiles()
      loadHistory()
    } else {
      ElMessage.error(result.msg || t('backup.import_failed'))
    }
  } catch (err: unknown) {
    const e = err as Error
    ElMessage.error(e.message || t('backup.import_failed'))
  } finally {
    importing.value = false
  }
}

const handleFileChange = async (file: FileUploadRaw) => {
  importForm.value.file = file.raw

  try {
    const result = await previewBackup(file.raw)
    if (result.code === 0) {
      previewData.value = result.data as ExportPackage
    } else {
      ElMessage.error(result.msg || t('backup.preview_failed'))
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

const handleExceed = (files: File[]) => {
  // 清除旧文件，选择新文件
  fileList.value = []
  if (files[0]) {
    fileList.value = [{ name: files[0].name, raw: files[0] }]
    handleFileChange({ raw: files[0] } as FileUploadRaw)
  }
}

const handleDownload = (row: BackupHistoryItem) => {
  downloadBackup(row.id, row.fileName)
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
</script>

<style lang="scss" scoped>
.backup-settings {
  padding: 0 20px;
}

.backup-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .card-body {
    .description {
      margin-bottom: 16px;
      font-size: 14px;
      color: #909399;
    }
  }
}

.preview-info {
  padding: 12px;
  margin: 16px 0;
  background: #f5f7fa;
  border-radius: 4px;
}

.backup-upload {
  width: 100%;
}
</style>
