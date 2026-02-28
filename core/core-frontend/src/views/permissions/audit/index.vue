<template>
  <div class="audit-container">
    <div class="audit-header">
      <div class="header-left">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
          style="width: 280px; margin-right: 16px"
        />
        <el-select
          v-model="searchForm.operationType"
          placeholder="操作类型"
          clearable
          style="width: 120px; margin-right: 16px"
          @change="handleSearch"
        >
          <el-option label="全部" value="" />
          <el-option label="授权" value="grant" />
          <el-option label="撤销" value="revoke" />
          <el-option label="更新" value="update" />
        </el-select>
        <el-select
          v-model="searchForm.targetType"
          placeholder="目标类型"
          clearable
          style="width: 120px; margin-right: 16px"
          @change="handleSearch"
        >
          <el-option label="全部" value="" />
          <el-option label="角色" value="role" />
          <el-option label="用户" value="user" />
        </el-select>
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索操作人、目标名称"
          clearable
          style="width: 200px; margin-right: 16px"
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          {{ t('commons.search') }}
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>
          {{ t('commons.reset') }}
        </el-button>
      </div>
      <div class="header-right">
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          {{ t('commons.export') }}
        </el-button>
      </div>
    </div>

    <div class="audit-content">
      <el-table
        :data="auditLogs"
        v-loading="loading"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="operationTime" label="操作时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.operationTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="operationType" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getOperationTypeTag(row.operationType)">
              {{ getOperationTypeLabel(row.operationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="目标类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">
              {{ getTargetTypeLabel(row.targetType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="目标名称" min-width="150" />
        <el-table-column prop="resourceName" label="资源名称" min-width="150" />
        <el-table-column prop="permission" label="权限" width="120">
          <template #default="{ row }">
            <el-tag size="small">
              {{ getPermissionLabel(row.permission) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.result === 'success' ? 'success' : 'danger'" size="small">
              {{ row.result === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">
              {{ t('commons.detail') }}
            </el-button>
            <el-button
              v-if="row.result === 'success'"
              type="warning"
              link
              size="small"
              @click="handleRollback(row)"
            >
              {{ t('permissions.rollback') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalRecords"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" :title="t('permissions.audit_detail')" width="800px">
      <div class="audit-detail" v-if="selectedAudit">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="操作时间">{{
            formatDateTime(selectedAudit.operationTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{
            selectedAudit.operatorName
          }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">
            <el-tag :type="getOperationTypeTag(selectedAudit.operationType)">
              {{ getOperationTypeLabel(selectedAudit.operationType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="目标类型">
            <el-tag size="small" type="info">
              {{ getTargetTypeLabel(selectedAudit.targetType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="目标名称" :span="2">{{
            selectedAudit.targetName
          }}</el-descriptions-item>
          <el-descriptions-item label="资源名称" :span="2">{{
            selectedAudit.resourceName
          }}</el-descriptions-item>
          <el-descriptions-item label="权限">{{
            getPermissionLabel(selectedAudit.permission)
          }}</el-descriptions-item>
          <el-descriptions-item label="操作结果">
            <el-tag :type="selectedAudit.result === 'success' ? 'success' : 'danger'">
              {{ selectedAudit.result === 'success' ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作IP" :span="2">{{
            selectedAudit.operatorIp || '-'
          }}</el-descriptions-item>
        </el-descriptions>

        <div class="change-details" v-if="selectedAudit.oldValue || selectedAudit.newValue">
          <h4>变更详情</h4>
          <div class="change-comparison">
            <div class="change-section" v-if="selectedAudit.oldValue">
              <h5>变更前</h5>
              <pre class="change-content">{{
                JSON.stringify(selectedAudit.oldValue, null, 2)
              }}</pre>
            </div>
            <div class="change-section" v-if="selectedAudit.newValue">
              <h5>变更后</h5>
              <pre class="change-content">{{
                JSON.stringify(selectedAudit.newValue, null, 2)
              }}</pre>
            </div>
          </div>
        </div>

        <div class="error-info" v-if="selectedAudit.result === 'failed' && selectedAudit.errorMsg">
          <h4>错误信息</h4>
          <el-alert :title="selectedAudit.errorMsg" type="error" :closable="false" />
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">{{ t('commons.close') }}</el-button>
      </template>
    </el-dialog>

    <!-- 回滚确认对话框 -->
    <el-dialog
      v-model="rollbackDialogVisible"
      :title="t('permissions.confirm_rollback')"
      width="500px"
    >
      <div class="rollback-confirm">
        <el-alert
          :title="t('permissions.rollback_warning')"
          type="warning"
          :description="t('permissions.rollback_warning_desc')"
          :closable="false"
          show-icon
        />
        <div class="rollback-info">
          <p><strong>操作时间：</strong>{{ formatDateTime(selectedAudit?.operationTime) }}</p>
          <p><strong>操作人：</strong>{{ selectedAudit?.operatorName }}</p>
          <p>
            <strong>操作类型：</strong>{{ getOperationTypeLabel(selectedAudit?.operationType) }}
          </p>
          <p><strong>目标名称：</strong>{{ selectedAudit?.targetName }}</p>
        </div>
      </div>

      <template #footer>
        <el-button @click="rollbackDialogVisible = false">{{ t('commons.cancel') }}</el-button>
        <el-button type="warning" @click="confirmRollback" :loading="rollbacking">
          {{ t('permissions.confirm_rollback') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { ElMessage, ElMessageBox } from 'element-plus-secondary'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import type { ElTable } from 'element-plus-secondary'

const { t } = useI18n()

// 数据定义
interface AuditLog {
  id: string
  operationType: string
  targetType: string
  targetId: string
  targetName: string
  resourceName: string
  permission: string
  oldValue?: any
  newValue?: any
  operatorId: string
  operatorName: string
  operatorIp?: string
  operationTime: string
  result: 'success' | 'failed'
  errorMsg?: string
}

interface SearchForm {
  operationType: string
  targetType: string
  keyword: string
}

// 响应式数据
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const totalRecords = ref(0)
const dateRange = ref<string[]>([])
const searchForm = reactive<SearchForm>({
  operationType: '',
  targetType: '',
  keyword: ''
})

const auditLogs = ref<AuditLog[]>([])
const selectedAudits = ref<AuditLog[]>([])
const selectedAudit = ref<AuditLog | null>(null)
const detailDialogVisible = ref(false)
const rollbackDialogVisible = ref(false)
const rollbacking = ref(false)

// 模拟数据
const mockAuditLogs: AuditLog[] = [
  {
    id: 'audit_001',
    operationType: 'grant',
    targetType: 'role',
    targetId: 'role_001',
    targetName: '超级管理员',
    resourceName: '用户管理菜单',
    permission: 'view',
    operatorId: 'user_001',
    operatorName: '系统管理员',
    operatorIp: '192.168.1.100',
    operationTime: '2024-01-15 14:30:00',
    result: 'success'
  },
  {
    id: 'audit_002',
    operationType: 'revoke',
    targetType: 'role',
    targetId: 'role_002',
    targetName: '普通用户',
    resourceName: '数据源管理',
    permission: 'delete',
    oldValue: { permissions: ['view', 'create', 'update', 'delete'] },
    newValue: { permissions: ['view', 'create', 'update'] },
    operatorId: 'user_001',
    operatorName: '系统管理员',
    operatorIp: '192.168.1.100',
    operationTime: '2024-01-15 14:25:00',
    result: 'success'
  },
  {
    id: 'audit_003',
    operationType: 'update',
    targetType: 'user',
    targetId: 'user_003',
    targetName: '张三',
    resourceName: '销售分析仪表板',
    permission: 'share',
    oldValue: { sharePermission: false },
    newValue: { sharePermission: true },
    operatorId: 'user_002',
    operatorName: '权限管理员',
    operatorIp: '192.168.1.101',
    operationTime: '2024-01-15 14:20:00',
    result: 'success'
  },
  {
    id: 'audit_004',
    operationType: 'grant',
    targetType: 'role',
    targetId: 'role_003',
    targetName: '数据分析师',
    resourceName: '数据集管理',
    permission: 'create',
    operatorId: 'user_001',
    operatorName: '系统管理员',
    operatorIp: '192.168.1.100',
    operationTime: '2024-01-15 14:15:00',
    result: 'failed',
    errorMsg: '权限配置冲突：该角色已存在冲突的权限设置'
  }
]

// 计算属性
const selectedIds = computed(() => {
  return selectedAudits.value.map(audit => audit.id)
})

// 方法定义
const loadAuditLogs = async () => {
  loading.value = true
  try {
    // TODO: 调用API获取审计日志
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      ...searchForm
    }

    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))

    auditLogs.value = mockAuditLogs
    totalRecords.value = mockAuditLogs.length
  } catch (error) {
    ElMessage.error(t('permissions.load_audit_failed'))
  } finally {
    loading.value = false
  }
}

const handleDateChange = () => {
  currentPage.value = 1
  loadAuditLogs()
}

const handleSearch = () => {
  currentPage.value = 1
  loadAuditLogs()
}

const handleReset = () => {
  dateRange.value = []
  searchForm.operationType = ''
  searchForm.targetType = ''
  searchForm.keyword = ''
  currentPage.value = 1
  loadAuditLogs()
}

const handleSelectionChange = (selection: AuditLog[]) => {
  selectedAudits.value = selection
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadAuditLogs()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadAuditLogs()
}

const handleViewDetail = (audit: AuditLog) => {
  selectedAudit.value = audit
  detailDialogVisible.value = true
}

const handleRollback = (audit: AuditLog) => {
  selectedAudit.value = audit
  rollbackDialogVisible.value = true
}

const confirmRollback = async () => {
  if (!selectedAudit.value) return

  rollbacking.value = true
  try {
    // TODO: 调用API回滚权限变更
    await rollbackPermissionChange(selectedAudit.value.id)

    ElMessage.success(t('permissions.rollback_success'))
    rollbackDialogVisible.value = false

    // 刷新数据
    loadAuditLogs()
  } catch (error) {
    ElMessage.error(t('permissions.rollback_failed'))
  } finally {
    rollbacking.value = false
  }
}

const handleExport = () => {
  if (selectedAudits.value.length === 0) {
    ElMessage.warning(t('permissions.please_select_audit_logs'))
    return
  }

  // TODO: 导出审计日志
  const data = JSON.stringify(selectedAudits.value, null, 2)
  const blob = new Blob([data], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `audit_logs_${new Date().toISOString().split('T')[0]}.json`
  link.click()
  URL.revokeObjectURL(url)

  ElMessage.success(t('permissions.export_success'))
}

const rollbackPermissionChange = async (auditId: string) => {
  // TODO: 调用API回滚权限变更
  console.log('回滚权限变更:', auditId)
  return Promise.resolve()
}

const getOperationTypeTag = (type: string) => {
  const tagMap: Record<string, string> = {
    grant: 'success',
    revoke: 'danger',
    update: 'warning'
  }
  return tagMap[type] || 'info'
}

const getOperationTypeLabel = (type: string) => {
  const labelMap: Record<string, string> = {
    grant: '授权',
    revoke: '撤销',
    update: '更新'
  }
  return labelMap[type] || type
}

const getTargetTypeLabel = (type: string) => {
  const labelMap: Record<string, string> = {
    role: '角色',
    user: '用户'
  }
  return labelMap[type] || type
}

const getPermissionLabel = (permission: string) => {
  const labelMap: Record<string, string> = {
    view: '查看',
    create: '创建',
    update: '编辑',
    delete: '删除',
    share: '分享'
  }
  return labelMap[permission] || permission
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

// 初始化
onMounted(() => {
  // 设置默认日期范围为最近30天
  const endDate = new Date()
  const startDate = new Date()
  startDate.setDate(startDate.getDate() - 30)

  dateRange.value = [startDate.toISOString().split('T')[0], endDate.toISOString().split('T')[0]]

  loadAuditLogs()
})
</script>

<style lang="less" scoped>
.audit-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 24px;
}

.audit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
}

.audit-content {
  flex: 1;
  overflow-y: auto;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.audit-detail {
  .change-details {
    margin-top: 24px;
  }

  .change-comparison {
    display: flex;
    gap: 20px;
    margin-top: 16px;
  }

  .change-section {
    flex: 1;

    h5 {
      margin: 0 0 12px 0;
      font-size: 14px;
      color: #303133;
    }
  }

  .change-content {
    background: #f5f7fa;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    padding: 12px;
    font-size: 12px;
    line-height: 1.5;
    max-height: 200px;
    overflow-y: auto;
    white-space: pre-wrap;
    word-break: break-all;
  }

  .error-info {
    margin-top: 24px;
  }
}

.rollback-confirm {
  .rollback-info {
    margin-top: 16px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 4px;

    p {
      margin: 8px 0;
      font-size: 14px;
      color: #606266;
    }
  }
}

:deep(.el-descriptions__label) {
  font-weight: 500;
  width: 100px;
}

:deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}
</style>
