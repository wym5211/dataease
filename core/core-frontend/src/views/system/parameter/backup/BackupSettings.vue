<template>
  <div class="backup-settings">
    <el-alert
      v-if="!isAdmin"
      title="权限提示"
      type="warning"
      description="只有管理员用户才能访问备份设置"
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
                <span>导出资源</span>
              </div>
            </template>
            <div class="card-body">
              <p class="description">将数据源、数据集、仪表板或数据大屏导出为备份文件</p>
              <el-form :model="exportForm" label-position="top">
                <el-form-item label="导出类型">
                  <el-select
                    v-model="exportForm.type"
                    placeholder="请选择导出类型"
                    style="width: 100%"
                  >
                    <el-option label="数据源" value="datasource" />
                    <el-option label="数据集" value="dataset" />
                    <el-option label="仪表板" value="dashboard" />
                    <el-option label="数据大屏" value="dataview" />
                    <el-option label="完整导出（包含所有）" value="combined" />
                  </el-select>
                </el-form-item>
                <el-form-item label="导出选项">
                  <el-checkbox v-model="exportForm.options.compress">压缩文件</el-checkbox>
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
                <span>导入资源</span>
              </div>
            </template>
            <div class="card-body">
              <p class="description">从备份文件导入数据源、数据集、仪表板或数据大屏</p>
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
                  </el-upload>
                </el-form-item>

                <div v-if="previewData" class="preview-info">
                  <el-descriptions :column="2" border size="small">
                    <el-descriptions-item label="版本">{{
                      previewData.version
                    }}</el-descriptions-item>
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
                        | 数据集: {{ previewData.datasets.length }}</span
                      >
                      <span v-if="previewData.dashboards?.length">
                        | 仪表板: {{ previewData.dashboards.length }}</span
                      >
                      <span v-if="previewData.dataviews?.length">
                        | 大屏: {{ previewData.dataviews.length }}</span
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
                <span>导出历史</span>
                <el-button text @click="loadHistory">刷新</el-button>
              </div>
            </template>
            <el-table :data="historyList" style="width: 100%">
              <el-table-column prop="fileName" label="文件名" />
              <el-table-column prop="itemCount" label="资源数量" width="100" />
              <el-table-column prop="exportTime" label="导出时间" width="180">
                <template #default="{ row }">
                  {{ formatTime(row.exportTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="message" label="状态" />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button link type="primary" @click="handleDownload(row)">下载</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>
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
  getBackupHistory
} from '@/api/backup'

const { t } = useI18n()
const userStore = useUserStoreWithOut()

const isAdmin = computed(() => userStore.getUid === '1' || userStore.getUid === 1)

const exportForm = ref({
  type: 'datasource',
  options: {
    compress: true
  }
})

const importForm = ref({
  overwrite: false,
  file: null as File | null
})

const previewData = ref(null)
const historyList = ref([])
const exporting = ref(false)
const importing = ref(false)
const uploadRef = ref()

onMounted(() => {
  if (isAdmin.value) {
    loadHistory()
  }
})

const loadHistory = async () => {
  try {
    const res = await getBackupHistory()
    if (res.code === 0) {
      historyList.value = res.data || []
    }
  } catch (e) {
    console.error('Failed to load history', e)
  }
}

const handleExport = async () => {
  try {
    exporting.value = true
    const result = await exportData({
      type: exportForm.value.type,
      resourceIds: [],
      options: exportForm.value.options
    })

    if (result.code === 0 && result.data) {
      ElMessage.success('导出成功')
      downloadBackup(result.data.id, result.data.fileName)
      loadHistory()
    } else {
      ElMessage.error(result.msg || '导出失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

const handleImport = async () => {
  if (!importForm.value.file) {
    ElMessage.warning('请选择要导入的文件')
    return
  }

  try {
    importing.value = true

    const uploadResult = await uploadBackup(importForm.value.file)
    if (uploadResult.code !== 0) {
      ElMessage.error(uploadResult.msg || '文件上传失败')
      return
    }

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
      importForm.value.file = null
      previewData.value = null
      uploadRef.value?.clearFiles()
      loadHistory()
    } else {
      ElMessage.error(result.msg || '导入失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '导入失败')
  } finally {
    importing.value = false
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

const handleDownload = (row: any) => {
  downloadBackup(row.id, row.fileName)
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
      color: #909399;
      font-size: 14px;
      margin-bottom: 16px;
    }
  }
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
