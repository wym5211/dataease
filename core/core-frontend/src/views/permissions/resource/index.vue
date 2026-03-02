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
            :filter-node-method="filterNode"
            node-key="id"
            show-checkbox
            check-strictly
            @check="handleCheckChange"
            @node-click="handleNodeClick"
            @check-change="handleSelectionChange"
          >
            <template #default="{ data }">
              <div class="tree-node">
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
                  {{ getPermissionStatus(data) === 'granted' ? '已授权' : '未授权' }}
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
              <el-checkbox label="edit">编辑</el-checkbox>
              <el-checkbox label="share">分享</el-checkbox>
              <el-checkbox label="export">导出</el-checkbox>
              <el-checkbox label="delete">删除</el-checkbox>
            </el-checkbox-group>
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
          <el-checkbox label="edit">编辑</el-checkbox>
          <el-checkbox label="share">分享</el-checkbox>
          <el-checkbox label="export">导出</el-checkbox>
          <el-checkbox label="delete">删除</el-checkbox>
        </el-checkbox-group>
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
import { usePermissionStore } from '@/stores/permission'
import type { ResourceItem } from '@/stores/permission'

const permissionStore = usePermissionStore()

// 直接使用 store 的计算属性（在模板中自动解包）
const roleList = computed(() => permissionStore.roleList as any)
const hasChanges = computed(() => permissionStore.hasChanges)
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
  const treeData = permissionStore.resourceTree
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
  return permissionStore.getRelatedMenus(selectedNode.value.id)
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
    updateTreeChecks()
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
    updateTreeChecks()
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
  const checked = checkedInfo.checkedKeys.includes(data.id)
  permissionStore.updateResourcePermission(data.id, checked)
}

const handleNodeClick = (data: ResourceItem) => {
  selectedNode.value = data
  selectedPermissions.value = permissionStore.getResourcePermissions(data.id)
}

const handleSelectionChange = () => {
  const checkedNodes = resourceTreeRef.value?.getCheckedNodes() || []
  selectedResources.value = checkedNodes.map(node => node.id)
}

const handlePermissionChange = () => {
  if (selectedNode.value) {
    permissionStore.updateResourceNodePermissions(selectedNode.value.id, selectedPermissions.value)
  }
}

const getPermissionStatus = (data: ResourceItem): string | null => {
  if (!selectedRole.value) return null
  return permissionStore.hasResourcePermission(data.id) ? 'granted' : 'revoked'
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

const formatDate = (timestamp: number): string => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString('zh-CN')
}

const expandAll = () => {
  const allKeys = getAllNodeKeys(resourceTree.value)
  resourceTreeRef.value?.setExpandedKeys(allKeys)
}

const collapseAll = () => {
  resourceTreeRef.value?.setExpandedKeys([])
}

const selectAll = () => {
  const allKeys = getAllNodeKeys(resourceTree.value)
  resourceTreeRef.value?.setCheckedKeys(allKeys)
}

const clearSelection = () => {
  resourceTreeRef.value?.setCheckedKeys([])
}

const getAllNodeKeys = (tree: ResourceItem[]): string[] => {
  const keys: string[] = []
  const traverse = (nodes: ResourceItem[]) => {
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

const updateTreeChecks = () => {
  nextTick(() => {
    const checkedKeys = permissionStore.getResourceCheckedKeys()
    resourceTreeRef.value?.setCheckedKeys(checkedKeys)
  })
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
    selectedResources.value.forEach(resourceId => {
      permissionStore.updateResourcePermissions(resourceId, batchPermissions.value)
    })
    ElMessage.success(`批量授权 ${selectedResources.value.length} 个资源成功`)
  } else {
    selectedResources.value.forEach(resourceId => {
      permissionStore.revokeResourcePermissions(resourceId)
    })
    ElMessage.success(`批量取消授权 ${selectedResources.value.length} 个资源成功`)
  }
  batchDialogVisible.value = false
}

const quickGrant = () => {
  if (!selectedNode.value || !selectedRole.value) return
  permissionStore.updateResourcePermission(selectedNode.value.id, true)
  ElMessage.success('快速授权成功')
}

const quickRevoke = () => {
  if (!selectedNode.value || !selectedRole.value) return
  permissionStore.updateResourcePermission(selectedNode.value.id, false)
  ElMessage.success('取消授权成功')
}

const viewResource = () => {
  if (!selectedNode.value) return
  // 打开资源预览或编辑页面
  ElMessage.info(`查看资源: ${selectedNode.value.name}`)
}

const saveChanges = async () => {
  if (!selectedRole.value) return

  try {
    await ElMessageBox.confirm('确定要保存权限变更吗？', '确认保存', {
      type: 'warning'
    })

    await permissionStore.saveResourcePermissionChanges(
      selectedRole.value,
      selectedResourceType.value
    )
    ElMessage.success('权限保存成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('权限保存失败')
    }
  }
}

const resetChanges = () => {
  permissionStore.resetResourceChanges()
  updateTreeChecks()
  ElMessage.info('已重置为上次保存状态')
}

// 初始化
onMounted(async () => {
  try {
    console.log('[资源授权页面] 开始加载角色列表...')
    await permissionStore.loadRoles()
    console.log('[资源授权页面] loadRoles 完成')
    console.log('[资源授权页面] permissionStore.state.roles:', permissionStore.state.roles)
    console.log('[资源授权页面] roleList 长度:', roleList.value?.length || 0)
    console.log('[资源授权页面] roleList 值:', roleList.value)
    console.log('[资源授权页面] permissionStore.roleList:', permissionStore.roleList)

    // 加载默认的资源树
    console.log('[资源授权页面] 加载默认资源树:', selectedResourceType.value)
    await permissionStore.loadResourceTree(selectedResourceType.value)
    console.log('[资源授权页面] 资源树加载完成')
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
