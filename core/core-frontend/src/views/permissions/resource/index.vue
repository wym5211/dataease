<template>
  <div class="resource-auth-container">
    <div class="header-bar">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="角色">
          <el-select
            v-model="selectedRole"
            placeholder="请选择角色"
            clearable
            style="width: 200px"
            @change="handleRoleChange"
          >
            <el-option
              v-for="role in roleList"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select
            v-model="selectedResourceType"
            placeholder="选择资源类型"
            style="width: 150px"
            @change="handleResourceTypeChange"
          >
            <el-option label="仪表板" value="dashboard" />
            <el-option label="数据集" value="dataset" />
            <el-option label="数据源" value="datasource" />
            <el-option label="图表" value="chart" />
            <el-option label="大屏" value="screen" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="searchKey"
            placeholder="搜索资源名称"
            clearable
            style="width: 200px"
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveChanges" :disabled="!hasChanges || !selectedRole">
            保存变更
          </el-button>
          <el-button @click="resetChanges" :disabled="!hasChanges"> 重置 </el-button>
          <el-button
            @click="batchGrant"
            :disabled="!selectedRole || selectedResources.length === 0"
          >
            批量授权
          </el-button>
          <el-button
            @click="batchRevoke"
            :disabled="!selectedRole || selectedResources.length === 0"
          >
            批量取消
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-area">
      <div class="resource-tree-panel">
        <div class="panel-header">
          <span>{{ resourceTypeLabel }}资源树</span>
          <el-button-group size="small">
            <el-button @click="expandAll">全部展开</el-button>
            <el-button @click="collapseAll">全部收起</el-button>
            <el-button @click="selectAll">全选</el-button>
            <el-button @click="clearSelection">清除选择</el-button>
          </el-button-group>
        </div>
        <div class="tree-wrapper">
          <el-tree
            ref="resourceTreeRef"
            :data="resourceTree"
            :props="treeProps"
            :default-expand-all="false"
            :expand-on-click-node="false"
            :check-on-click-node="false"
            :filter-node-method="filterNode"
            node-key="id"
            show-checkbox
            check-strictly
            @check="handleCheckChange"
            @check-change="handleSelectionChange"
          >
            <template #default="{ data }">
              <div class="tree-node" @click.stop="handleNodeClick(data)">
                <el-icon class="node-icon">
                  <Document v-if="data.type === 'file'" />
                  <Folder v-else />
                </el-icon>
                <span class="node-label">{{ data.name }}</span>
                <el-tag
                  v-if="getPermissionStatus(data)"
                  :type="getPermissionStatus(data) === 'granted' ? 'success' : 'info'"
                  size="small"
                  class="permission-tag"
                >
                  {{ getPermissionTagText(data) }}
                </el-tag>
                <el-tooltip v-if="data.description" :content="data.description" placement="top">
                  <el-icon class="info-icon"><InfoFilled /></el-icon>
                </el-tooltip>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <div class="detail-panel">
        <div class="panel-header">资源详情</div>
        <div v-if="selectedNode" class="detail-content">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="资源名称">{{ selectedNode.name }}</el-descriptions-item>
            <el-descriptions-item label="资源类型">{{
              getResourceTypeLabel(selectedNode.type)
            }}</el-descriptions-item>
            <el-descriptions-item label="创建者">{{
              selectedNode.creator || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{
              formatDate(selectedNode.createTime)
            }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{
              formatDate(selectedNode.updateTime)
            }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="selectedNode.status === 'published' ? 'success' : 'warning'">
                {{ selectedNode.status === 'published' ? '已发布' : '未发布' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <div class="permission-actions">
            <h4>权限操作</h4>
            <el-checkbox-group v-model="selectedPermissions" @change="handlePermissionChange">
              <el-checkbox label="view">查看</el-checkbox>
              <el-checkbox label="edit">编辑（管理）</el-checkbox>
              <el-checkbox label="share">分享</el-checkbox>
              <el-checkbox label="export">导出（管理）</el-checkbox>
              <el-checkbox label="delete">删除（管理）</el-checkbox>
            </el-checkbox-group>
            <el-text type="info" size="small">编辑/导出/删除共用同一管理权限位</el-text>
          </div>

          <div v-if="relatedMenus.length > 0" class="related-menus">
            <h4>关联菜单</h4>
            <el-tag
              v-for="menu in relatedMenus"
              :key="menu.id"
              type="info"
              style="margin-right: 8px; margin-bottom: 4px"
            >
              {{ menu.name }}
            </el-tag>
          </div>

          <div class="resource-actions">
            <el-button type="primary" size="small" @click="quickGrant"> 快速授权 </el-button>
            <el-button size="small" @click="quickRevoke"> 取消授权 </el-button>
            <el-button size="small" @click="viewResource"> 查看资源 </el-button>
          </div>
        </div>
        <div v-else class="empty-detail">
          <el-empty description="请选择资源节点" />
        </div>
      </div>
    </div>

    <div v-if="hasChanges" class="change-summary">
      <el-alert title="权限变更预览" type="warning" :closable="false">
        <div class="change-list">
          <div v-for="change in changeSummary" :key="change.id" class="change-item">
            <el-icon :class="change.type">
              <CircleCheck v-if="change.type === 'grant'" />
              <CircleClose v-if="change.type === 'revoke'" />
            </el-icon>
            <span class="change-desc">{{ change.description }}</span>
          </div>
        </div>
      </el-alert>
    </div>

    <!-- 批量操作对话框 -->
    <el-dialog
      v-model="batchDialogVisible"
      :title="batchAction === 'grant' ? '批量授权' : '批量取消授权'"
      width="500px"
    >
      <div class="batch-dialog-content">
        <p>选中了 {{ selectedResources.length }} 个资源</p>
        <el-checkbox-group v-model="batchPermissions">
          <el-checkbox label="view">查看</el-checkbox>
          <el-checkbox label="edit">编辑（管理）</el-checkbox>
          <el-checkbox label="share">分享</el-checkbox>
          <el-checkbox label="export">导出（管理）</el-checkbox>
          <el-checkbox label="delete">删除（管理）</el-checkbox>
        </el-checkbox-group>
        <el-text type="info" size="small">编辑/导出/删除共用同一管理权限位</el-text>
      </div>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchAction">
          确认{{ batchAction === 'grant' ? '授权' : '取消' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus-secondary'
import {
  Search,
  Document,
  Folder,
  InfoFilled,
  CircleCheck,
  CircleClose
} from '@element-plus/icons-vue'
import type { TreeInstance } from 'element-plus-secondary'
import { useRouter } from 'vue-router'
import { usePermissionStore } from '@/stores/permission'
import type { ResourceNode } from '@/stores/permission'

type ResourceItem = {
  readonly id: ResourceNode['id']
  readonly name: ResourceNode['name']
  readonly type: ResourceNode['type']
  readonly hasPermission: ResourceNode['hasPermission']
  readonly permissions: readonly string[]
  readonly children?: readonly ResourceItem[]
  readonly createTime?: ResourceNode['createTime']
  readonly updateTime?: string
  readonly status?: string
  readonly description?: string
  readonly creator?: ResourceNode['creator']
  readonly parentId?: ResourceNode['parentId']
}

const permissionStore = usePermissionStore()
const router = useRouter()

// 直接使用 store 的计算属性（在模板中自动解包）
const roleList = computed(() => permissionStore.roleList as any)
const hasChanges = computed(() => Boolean(permissionStore.hasAnyChanges))
const changeSummary = computed(() => permissionStore.changeSummary)

const selectedRole = ref<string>('')
const selectedResourceType = ref<string>('dashboard')
const searchKey = ref<string>('')
const resourceTreeRef = ref<TreeInstance>()
const selectedNode = ref<ResourceItem | null>(null)
const selectedPermissions = ref<string[]>([])
const selectedResources = ref<string[]>([])
const batchDialogVisible = ref<boolean>(false)
const batchAction = ref<'grant' | 'revoke'>('grant')
const batchPermissions = ref<string[]>(['view'])

// resourceTree 需要动态获取，因为依赖 selectedResourceType
const resourceTree = computed(() => {
  // 直接访问 state 以确保响应式更新
  const treeData = permissionStore.state.resourceTreeData
  if (!treeData) return []
  return treeData[selectedResourceType.value] || []
})

const resourceTypeLabel = computed(() => {
  const labels = {
    dashboard: '仪表板',
    dataset: '数据集',
    datasource: '数据源',
    chart: '图表',
    screen: '大屏'
  }
  return labels[selectedResourceType.value as keyof typeof labels] || '资源'
})

const relatedMenus = computed(() => {
  if (!selectedNode.value) return []
  const getRelatedMenus = (permissionStore as any).getRelatedMenus
  if (typeof getRelatedMenus !== 'function') {
    return []
  }
  return getRelatedMenus(selectedNode.value.id) || []
})

const treeProps = {
  children: 'children',
  label: 'name'
}

const handleRoleChange = async (roleId: string) => {
  if (!roleId) return
  try {
    // 选择角色
    permissionStore.selectRole(roleId)
    // 加载资源树
    await permissionStore.loadResourceTree(selectedResourceType.value)
    await updateTreeChecks()
  } catch (error) {
    ElMessage.error('加载角色权限失败')
  }
}

const handleResourceTypeChange = async (type: string) => {
  selectedResourceType.value = type
  selectedNode.value = null
  selectedResources.value = []
  try {
    await permissionStore.loadResourceTree(type)
    await updateTreeChecks()
  } catch (error) {
    ElMessage.error('加载资源树失败')
  }
}

const handleSearch = () => {
  resourceTreeRef.value?.filter(searchKey.value)
}

const filterNode = (value: string, data: ResourceItem) => {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

const handleCheckChange = (data: ResourceItem, checkedInfo: any) => {
  if (!checkedInfo || !Array.isArray(checkedInfo.checkedKeys)) {
    return
  }
  const checkedKeys = checkedInfo.checkedKeys
  const checked = checkedKeys.includes(data.id)
  const permissions = checked ? ['view'] : []
  permissionStore.updateResourcePermission(selectedResourceType.value, data.id, permissions)
  selectedNode.value = data
  selectedPermissions.value = permissionStore.getResourcePermissions(data.id)
}

const handleNodeClick = (data: ResourceItem) => {
  selectedNode.value = data
  selectedPermissions.value = permissionStore.getResourcePermissions(data.id)
}

const handleSelectionChange = () => {
  const checkedNodes = resourceTreeRef.value?.getCheckedNodes() || []
  selectedResources.value = checkedNodes.map(node => node.id)
}

const normalizePermissionSelection = (permissions: string[]): string[] => {
  const set = new Set(permissions)
  const hasManage = set.has('edit') || set.has('export') || set.has('delete')
  if (hasManage) {
    set.add('edit')
    set.add('export')
    set.add('delete')
  } else {
    set.delete('edit')
    set.delete('export')
    set.delete('delete')
  }
  return ['view', 'edit', 'share', 'export', 'delete'].filter(permission => set.has(permission))
}

const handlePermissionChange = () => {
  if (selectedNode.value) {
    const normalizedPermissions = normalizePermissionSelection(selectedPermissions.value)
    selectedPermissions.value = normalizedPermissions
    permissionStore.updateResourcePermission(
      selectedResourceType.value,
      selectedNode.value.id,
      normalizedPermissions
    )
  }
}

const getPermissionStatus = (data: ResourceItem): string | null => {
  if (!selectedRole.value) return null
  return permissionStore.hasResourcePermission(data.id) ? 'granted' : 'revoked'
}

const getPermissionTagText = (data: ResourceItem): string => {
  if (!selectedRole.value) return ''
  if (!permissionStore.hasResourcePermission(data.id)) {
    return '未授权'
  }
  const permissions = permissionStore.getResourcePermissions(data.id)
  const labels: string[] = []
  if (permissions.includes('view')) {
    labels.push('查看')
  }
  if (
    permissions.includes('edit') ||
    permissions.includes('export') ||
    permissions.includes('delete')
  ) {
    labels.push('管理')
  }
  if (permissions.includes('share')) {
    labels.push('分享')
  }
  return labels.length ? `已授权(${labels.join('、')})` : '已授权'
}

const getResourceTypeLabel = (type: string): string => {
  const labels = {
    dashboard: '仪表板',
    dataset: '数据集',
    datasource: '数据源',
    chart: '图表',
    screen: '大屏',
    folder: '文件夹'
  }
  return labels[type as keyof typeof labels] || type
}

const formatDate = (value?: string | number): string => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN')
}

const setTreeExpandedState = (expanded: boolean) => {
  const treeInstance = resourceTreeRef.value as any
  const setExpandedKeys = treeInstance?.setExpandedKeys
  if (typeof setExpandedKeys === 'function') {
    const expandedKeys = expanded ? getAllNodeKeys(resourceTree.value) : []
    setExpandedKeys.call(treeInstance, expandedKeys)
    return
  }
  const nodesMap = treeInstance?.store?.nodesMap || {}
  Object.values(nodesMap).forEach((node: any) => {
    if (node && node.level > 0) {
      node.expanded = expanded
    }
  })
}

const expandAll = () => {
  setTreeExpandedState(true)
}

const collapseAll = () => {
  setTreeExpandedState(false)
}

const selectAll = () => {
  const allKeys = getAllNodeKeys(resourceTree.value)
  resourceTreeRef.value?.setCheckedKeys(allKeys)
}

const clearSelection = () => {
  resourceTreeRef.value?.setCheckedKeys([])
}

const getAllNodeKeys = (tree: readonly ResourceItem[]): string[] => {
  const keys: string[] = []
  const traverse = (nodes: readonly ResourceItem[]) => {
    nodes.forEach(node => {
      keys.push(node.id)
      if (node.children?.length) {
        traverse(node.children)
      }
    })
  }
  traverse(tree)
  return keys
}

const updateTreeChecks = async () => {
  await nextTick()
  const checkedKeys = permissionStore.getResourceCheckedKeys()
  resourceTreeRef.value?.setCheckedKeys(checkedKeys, false)
}

const batchGrant = () => {
  if (selectedResources.value.length === 0) {
    ElMessage.warning('请先选择资源')
    return
  }
  batchAction.value = 'grant'
  batchPermissions.value = ['view']
  batchDialogVisible.value = true
}

const batchRevoke = () => {
  if (selectedResources.value.length === 0) {
    ElMessage.warning('请先选择资源')
    return
  }
  batchAction.value = 'revoke'
  batchPermissions.value = []
  batchDialogVisible.value = true
}

const confirmBatchAction = () => {
  if (batchAction.value === 'grant') {
    const normalizedBatchPermissions = normalizePermissionSelection(batchPermissions.value)
    batchPermissions.value = normalizedBatchPermissions
    selectedResources.value.forEach(resourceId => {
      permissionStore.updateResourcePermission(
        selectedResourceType.value,
        resourceId,
        normalizedBatchPermissions
      )
    })
    ElMessage.success(`批量授权 ${selectedResources.value.length} 个资源成功`)
  } else {
    selectedResources.value.forEach(resourceId => {
      permissionStore.updateResourcePermission(selectedResourceType.value, resourceId, [])
    })
    ElMessage.success(`批量取消授权 ${selectedResources.value.length} 个资源成功`)
  }
  batchDialogVisible.value = false
}

const quickGrant = async () => {
  if (!selectedNode.value || !selectedRole.value) return
  selectedPermissions.value = ['view']
  permissionStore.updateResourcePermission(selectedResourceType.value, selectedNode.value.id, [
    'view'
  ])
  await updateTreeChecks()
  handleSelectionChange()
  ElMessage.success('快速授权成功')
}

const quickRevoke = async () => {
  if (!selectedNode.value || !selectedRole.value) return
  selectedPermissions.value = []
  permissionStore.updateResourcePermission(selectedResourceType.value, selectedNode.value.id, [])
  await updateTreeChecks()
  handleSelectionChange()
  ElMessage.success('取消授权成功')
}

const viewResource = () => {
  if (!selectedNode.value) return
  const resourceId = selectedNode.value.id
  const resourceType = selectedNode.value.type
  if (resourceType === 'dashboard') {
    const newWindow = window.open(`#/dashboard?resourceId=${resourceId}`, '_blank')
    if (!newWindow) {
      ElMessage.warning('无法打开新窗口，请检查浏览器弹窗设置')
    }
    return
  }
  if (resourceType === 'screen') {
    const newWindow = window.open(`#/dvCanvas?dvId=${resourceId}`, '_blank')
    if (!newWindow) {
      ElMessage.warning('无法打开新窗口，请检查浏览器弹窗设置')
    }
    return
  }
  if (resourceType === 'chart') {
    const newWindow = window.open(`#/chart?id=${resourceId}`, '_blank')
    if (!newWindow) {
      ElMessage.warning('无法打开新窗口，请检查浏览器弹窗设置')
    }
    return
  }
  if (resourceType === 'dataset') {
    router.push({
      path: '/dataset-embedded-form',
      query: {
        id: String(resourceId)
      }
    })
    return
  }
  if (resourceType === 'datasource') {
    router.push({
      path: '/datasource',
      query: {
        id: String(resourceId)
      }
    })
    return
  }
  ElMessage.info(`当前资源类型暂不支持查看: ${selectedNode.value.name}`)
}

const saveChanges = async () => {
  if (!selectedRole.value) return

  try {
    await ElMessageBox.confirm('确定要保存权限变更吗？', '确认保存', {
      type: 'warning'
    })

    await permissionStore.saveResourcePermissionChanges()
    ElMessage.success('权限保存成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('权限保存失败')
    }
  }
}

const resetChanges = () => {
  permissionStore.resetPermissionChanges()
  updateTreeChecks()
  ElMessage.info('已重置为上次保存状态')
}

// 初始化
onMounted(async () => {
  try {
    await permissionStore.loadRoles()
    // 加载默认的资源树
    await permissionStore.loadResourceTree(selectedResourceType.value)
  } catch (error) {
    console.error('初始化失败:', error)
  }
})
</script>

<style lang="less" scoped>
.resource-auth-container {
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 120px);
}

.header-bar {
  margin-bottom: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.filter-form {
  margin: 0;
}

.content-area {
  display: flex;
  gap: 16px;
  height: calc(100vh - 300px);
}

.resource-tree-panel {
  flex: 1;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
}

.detail-panel {
  width: 400px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  font-weight: 500;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tree-wrapper {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.node-icon {
  font-size: 16px;
  color: #909399;
}

.node-label {
  flex: 1;
  font-size: 14px;
}

.permission-tag {
  margin-left: 8px;
}

.info-icon {
  font-size: 14px;
  color: #909399;
  cursor: help;
}

.detail-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.empty-detail {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.permission-actions {
  margin-top: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.permission-actions h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 500;
}

.related-menus {
  margin-top: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.related-menus h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 500;
}

.resource-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}

.change-summary {
  margin-top: 16px;
}

.change-list {
  margin-top: 8px;
}

.change-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 13px;
}

.change-item .el-icon {
  font-size: 14px;
}

.change-item .el-icon.grant {
  color: #67c23a;
}

.change-item .el-icon.revoke {
  color: #f56c6c;
}

.change-desc {
  color: #606266;
}

.batch-dialog-content {
  padding: 16px 0;
}

.batch-dialog-content p {
  margin: 0 0 16px 0;
  color: #606266;
}
</style>
