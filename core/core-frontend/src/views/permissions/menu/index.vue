<template>
  <div class="menu-auth-container">
    <div class="header-bar">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="角色">
          <el-select v-model="selectedRole" placeholder="请选择角色" clearable style="width: 200px">
            <el-option label="超级管理员" value="role_001" />
            <el-option label="普通用户" value="role_002" />
            <el-option label="数据分析师" value="role_003" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索菜单">
          <el-input
            v-model="searchKey"
            placeholder="输入菜单名称"
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
          <el-button type="primary" @click="saveChanges" :disabled="!selectedRole">
            保存变更
          </el-button>
          <el-button @click="resetChanges"> 重置 </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-area">
      <div class="tree-panel">
        <div class="panel-header">
          <span>菜单权限树</span>
          <el-button-group size="small">
            <el-button @click="expandAll">全部展开</el-button>
            <el-button @click="collapseAll">全部收起</el-button>
          </el-button-group>
        </div>
        <div class="tree-wrapper">
          <el-tree
            ref="menuTreeRef"
            :data="menuTree"
            :props="treeProps"
            :default-expand-all="false"
            :expand-on-click-node="false"
            node-key="id"
            show-checkbox
            check-strictly
          >
            <template #default="{ data }">
              <div class="tree-node">
                <el-icon v-if="data.icon" class="node-icon">
                  <component :is="data.icon" />
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
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <div class="detail-panel">
        <div class="panel-header">权限详情</div>
        <div v-if="selectedNode" class="detail-content">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="菜单名称">{{ selectedNode.name }}</el-descriptions-item>
            <el-descriptions-item label="菜单路径">{{
              selectedNode.path || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="菜单图标">{{
              selectedNode.icon || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="排序">{{ selectedNode.sort || 0 }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="selectedNode.status === 'enabled' ? 'success' : 'danger'">
                {{ selectedNode.status === 'enabled' ? '启用' : '禁用' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <div class="permission-actions">
            <h4>权限操作</h4>
            <el-checkbox-group v-model="selectedPermissions">
              <el-checkbox label="view">查看</el-checkbox>
              <el-checkbox label="create">创建</el-checkbox>
              <el-checkbox label="update">修改</el-checkbox>
              <el-checkbox label="delete">删除</el-checkbox>
            </el-checkbox-group>
          </div>

          <!-- 关联资源暂时注释
          <div v-if="relatedResources.length > 0" class="related-resources">
            <h4>关联资源</h4>
            <el-tag
              v-for="resource in relatedResources"
              :key="resource.id"
              type="info"
              style="margin-right: 8px; margin-bottom: 4px"
            >
              {{ resource.name }}
            </el-tag>
          </div>
          -->
        </div>
        <div v-else class="empty-detail">
          <el-empty description="请选择菜单节点" />
        </div>
      </div>
    </div>

    <!-- 暂时注释变更预览
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
    -->
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus-secondary'
import { Search, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import type { TreeInstance } from 'element-plus-secondary'
import { usePermissionStore } from '@/stores/permission'
import type { MenuItem, RoleItem } from '@/stores/permission'

const permissionStore = usePermissionStore()

const selectedRole = ref<string>('')
const searchKey = ref<string>('')
const menuTreeRef = ref<TreeInstance>()
const selectedNode = ref<MenuItem | null>(null)
const selectedPermissions = ref<string[]>([])
const expandedKeys = ref<string[]>([])

// 使用计算属性从 store 获取数据
const roleList = computed<RoleItem[]>(() => {
  try {
    return permissionStore.roleList || []
  } catch {
    return []
  }
})

const menuTree = computed<MenuItem[]>(() => {
  try {
    return permissionStore.menuTree || []
  } catch {
    return []
  }
})

// 初始化数据
onMounted(async () => {
  // 加载菜单数据
  await permissionStore.loadMenuTree()
  console.log('菜单数据加载完成:', menuTree.value)
})

const hasChanges = computed(() => {
  try {
    return permissionStore.hasChanges || false
  } catch {
    return false
  }
})

const changeSummary = computed(() => {
  try {
    return permissionStore.changeSummary || []
  } catch {
    return []
  }
})

const relatedResources = computed(() => {
  if (!selectedNode.value) return []
  try {
    if (typeof permissionStore.getRelatedResources === 'function') {
      return permissionStore.getRelatedResources(selectedNode.value.id) || []
    }
    return []
  } catch (error) {
    console.error('getRelatedResources error:', error)
    return []
  }
})

const treeProps = {
  children: 'children',
  label: 'name'
}

const handleRoleChange = async (roleId: string) => {
  if (!roleId) return
  try {
    await permissionStore.loadRolePermissions(roleId)
    await permissionStore.loadMenuTree()
    updateTreeChecks()
  } catch (error) {
    ElMessage.error('加载角色权限失败')
  }
}

const handleSearch = () => {
  menuTreeRef.value?.filter(searchKey.value)
}

const filterNode = (value: string, data: MenuItem) => {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

const handleCheckChange = (data: MenuItem, checkedInfo: any) => {
  const checked = checkedInfo.checkedKeys.includes(data.id)
  permissionStore.updatePermission(data.id, checked)

  if (checked) {
    // 级联授权父节点
    cascadeGrantParent(data)
  } else {
    // 级联取消子节点
    cascadeRevokeChildren(data)
  }
}

const cascadeGrantParent = (node: MenuItem) => {
  if (node.parentId) {
    permissionStore.updatePermission(node.parentId, true)
    const parent = findNodeInTree(menuTree.value, node.parentId)
    if (parent) cascadeGrantParent(parent)
  }
}

const cascadeRevokeChildren = (node: MenuItem) => {
  if (node.children?.length) {
    node.children.forEach(child => {
      permissionStore.updatePermission(child.id, false)
      cascadeRevokeChildren(child)
    })
  }
}

const findNodeInTree = (tree: MenuItem[], id: string): MenuItem | null => {
  for (const node of tree) {
    if (node.id === id) return node
    if (node.children?.length) {
      const found = findNodeInTree(node.children, id)
      if (found) return found
    }
  }
  return null
}

const handleNodeExpand = (data: MenuItem) => {
  if (!expandedKeys.value.includes(data.id)) {
    expandedKeys.value.push(data.id)
  }
}

const handleNodeCollapse = (data: MenuItem) => {
  const index = expandedKeys.value.indexOf(data.id)
  if (index > -1) {
    expandedKeys.value.splice(index, 1)
  }
}

const expandAll = () => {
  const allKeys = getAllNodeKeys(menuTree.value)
  menuTreeRef.value?.setExpandedKeys(allKeys)
}

const collapseAll = () => {
  menuTreeRef.value?.setExpandedKeys([])
}

const getAllNodeKeys = (tree: MenuItem[]): string[] => {
  const keys: string[] = []
  const traverse = (nodes: MenuItem[]) => {
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
    const checkedKeys = permissionStore.getCheckedKeys()
    menuTreeRef.value?.setCheckedKeys(checkedKeys)
  })
}

const getPermissionStatus = (data: MenuItem): string | null => {
  if (!selectedRole.value) return null
  return permissionStore.hasPermission(data.id) ? 'granted' : 'revoked'
}

const saveChanges = async () => {
  if (!selectedRole.value) return

  try {
    await ElMessageBox.confirm('确定要保存权限变更吗？', '确认保存', {
      type: 'warning'
    })

    await permissionStore.savePermissionChanges(selectedRole.value)
    ElMessage.success('权限保存成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('权限保存失败')
    }
  }
}

const resetChanges = () => {
  permissionStore.resetChanges()
  updateTreeChecks()
  ElMessage.info('已重置为上次保存状态')
}

// 监听选中节点变化
const handleNodeClick = (data: MenuItem) => {
  selectedNode.value = data
  selectedPermissions.value = permissionStore.getNodePermissions(data.id)
}

// 监听权限选择变化
const handlePermissionChange = () => {
  if (selectedNode.value) {
    permissionStore.updateNodePermissions(selectedNode.value.id, selectedPermissions.value)
  }
}
</script>

<style lang="less" scoped>
.menu-auth-container {
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
  height: calc(100vh - 280px);
}

.tree-panel {
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

.related-resources {
  margin-top: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.related-resources h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 500;
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
</style>
