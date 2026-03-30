<template>
  <div class="template-container">
    <div class="header-bar">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="搜索模板">
          <el-input
            v-model="searchKey"
            placeholder="输入模板名称或描述"
            clearable
            style="width: 250px"
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="模板分类">
          <el-select
            v-model="selectedCategory"
            placeholder="选择分类"
            clearable
            style="width: 150px"
            @change="handleCategoryChange"
          >
            <el-option label="系统模板" value="system" />
            <el-option label="用户模板" value="user" />
            <el-option label="角色模板" value="role" />
            <el-option label="部门模板" value="department" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="createTemplate">
            <el-icon><Plus /></el-icon>
            新建模板
          </el-button>
          <el-button @click="refreshTemplates">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-form-item>
        <el-form-item style="margin-left: auto">
          <el-button-group>
            <el-button :type="viewMode === 'card' ? 'primary' : ''" @click="viewMode = 'card'">
              <el-icon><Grid /></el-icon>
              卡片
            </el-button>
            <el-button :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'">
              <el-icon><List /></el-icon>
              列表
            </el-button>
          </el-button-group>
        </el-form-item>
      </el-form>
    </div>

    <!-- 卡片视图 -->
    <div v-if="viewMode === 'card'" class="card-view">
      <div class="template-grid">
        <div
          v-for="template in filteredTemplates"
          :key="template.id"
          class="template-card"
          :class="{ 'system-template': template.category === 'system' }"
        >
          <div class="card-header">
            <div class="template-icon">
              <el-icon :size="32">
                <Document v-if="template.category === 'system'" />
                <User v-else-if="template.category === 'user'" />
                <Avatar v-else-if="template.category === 'role'" />
                <OfficeBuilding v-else />
              </el-icon>
            </div>
            <div class="template-actions">
              <el-dropdown trigger="click" @command="handleTemplateCommand($event, template)">
                <el-button type="text" size="small">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="view">
                      <el-icon><View /></el-icon>
                      查看详情
                    </el-dropdown-item>
                    <el-dropdown-item command="apply">
                      <el-icon><Promotion /></el-icon>
                      应用模板
                    </el-dropdown-item>
                    <el-dropdown-item command="edit" v-if="template.category !== 'system'">
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-dropdown-item>
                    <el-dropdown-item command="copy">
                      <el-icon><CopyDocument /></el-icon>
                      复制
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="delete"
                      v-if="template.category !== 'system'"
                      divided
                    >
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          <div class="card-content">
            <h4 class="template-name">{{ template.name }}</h4>
            <p class="template-desc">{{ template.description }}</p>
            <div class="template-meta">
              <el-tag size="small" :type="getCategoryType(template.category)">
                {{ getCategoryLabel(template.category) }}
              </el-tag>
              <span class="template-time">{{ formatDate(template.createTime) }}</span>
            </div>
            <div class="template-stats">
              <div class="stat-item">
                <el-icon><Menu /></el-icon>
                <span>{{ template.menuCount }} 菜单</span>
              </div>
              <div class="stat-item">
                <el-icon><Document /></el-icon>
                <span>{{ template.resourceCount }} 资源</span>
              </div>
            </div>
          </div>
          <div class="card-footer">
            <el-button type="primary" size="small" @click="applyTemplate(template)">
              应用模板
            </el-button>
            <el-button size="small" @click="previewTemplate(template)"> 预览 </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 列表视图 -->
    <div v-else class="list-view">
      <el-table :data="filteredTemplates" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="模板名称" min-width="200">
          <template #default="{ row }">
            <div class="template-name-cell">
              <el-icon class="template-icon">
                <Document v-if="row.category === 'system'" />
                <User v-else-if="row.category === 'user'" />
                <Avatar v-else-if="row.category === 'role'" />
                <OfficeBuilding v-else />
              </el-icon>
              <span class="name-text">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="getCategoryType(row.category)">
              {{ getCategoryLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="menuCount" label="菜单数" width="100" align="center" />
        <el-table-column prop="resourceCount" label="资源数" width="100" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="viewTemplate(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button type="text" size="small" @click="applyTemplate(row)">
              <el-icon><Promotion /></el-icon>
              应用
            </el-button>
            <el-button
              v-if="row.category !== 'system'"
              type="text"
              size="small"
              @click="editTemplate(row)"
            >
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              v-if="row.category !== 'system'"
              type="text"
              size="small"
              style="color: #f54a45"
              @click="deleteTemplate(row)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 模板详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="模板详情" width="800px" top="5vh">
      <div class="template-detail" v-if="selectedTemplate">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="模板名称">{{ selectedTemplate.name }}</el-descriptions-item>
          <el-descriptions-item label="分类">
            <el-tag size="small" :type="getCategoryType(selectedTemplate.category)">
              {{ getCategoryLabel(selectedTemplate.category) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{
            selectedTemplate.description
          }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{
            formatDate(selectedTemplate.createTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{
            formatDate(selectedTemplate.updateTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="菜单权限"
            >{{ selectedTemplate.menuCount }} 项</el-descriptions-item
          >
          <el-descriptions-item label="资源权限"
            >{{ selectedTemplate.resourceCount }} 项</el-descriptions-item
          >
        </el-descriptions>

        <div class="permission-preview">
          <h4>权限配置预览</h4>
          <el-tabs v-model="previewTab">
            <el-tab-pane label="菜单权限" name="menu">
              <div class="permission-list">
                <el-tag
                  v-for="menu in selectedTemplate.permissions?.menus || []"
                  :key="menu.id"
                  type="info"
                  style="margin: 4px"
                >
                  {{ menu.name }}
                </el-tag>
              </div>
            </el-tab-pane>
            <el-tab-pane label="资源权限" name="resource">
              <div class="permission-list">
                <el-tag
                  v-for="resource in selectedTemplate.permissions?.resources || []"
                  :key="resource.id"
                  type="info"
                  style="margin: 4px"
                >
                  {{ resource.name }}
                </el-tag>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="applyTemplate(selectedTemplate!)"> 应用此模板 </el-button>
      </template>
    </el-dialog>

    <!-- 应用模板对话框 -->
    <el-dialog v-model="applyDialogVisible" title="应用权限模板" width="600px">
      <div class="apply-template" v-if="selectedTemplate">
        <el-form :model="applyForm" label-width="100px">
          <el-form-item label="目标角色" required>
            <el-select v-model="applyForm.roleId" placeholder="选择角色" style="width: 100%">
              <el-option
                v-for="role in roleList"
                :key="role.id"
                :label="role.name"
                :value="role.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="应用范围">
            <el-radio-group v-model="applyForm.scope">
              <el-radio label="merge">合并到现有权限</el-radio>
              <el-radio label="replace">替换现有权限</el-radio>
              <el-radio label="selective">选择性应用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="权限类型" v-if="applyForm.scope === 'selective'">
            <el-checkbox-group v-model="applyForm.permissionTypes">
              <el-checkbox label="menu">菜单权限</el-checkbox>
              <el-checkbox label="resource">资源权限</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </el-form>
        <div class="apply-preview">
          <h4>预览效果</h4>
          <p>
            将{{
              applyForm.scope === 'merge'
                ? '合并'
                : applyForm.scope === 'replace'
                ? '替换'
                : '选择性应用'
            }}以下权限：
          </p>
          <ul>
            <li>菜单权限：{{ selectedTemplate.menuCount }} 项</li>
            <li>资源权限：{{ selectedTemplate.resourceCount }} 项</li>
          </ul>
        </div>
      </div>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmApplyTemplate" :loading="applying">
          确认应用
        </el-button>
      </template>
    </el-dialog>

    <!-- 创建模板对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建权限模板" width="600px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="createForm.name" placeholder="输入模板名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="createForm.category" placeholder="选择分类" style="width: 100%">
            <el-option label="用户模板" value="user" />
            <el-option label="角色模板" value="role" />
            <el-option label="部门模板" value="department" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="描述模板的用途和适用场景"
          />
        </el-form-item>
        <el-form-item label="权限来源">
          <el-radio-group v-model="createForm.source">
            <el-radio label="current">当前角色权限</el-radio>
            <el-radio label="selective">选择性添加</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreateTemplate" :loading="creating">
          创建模板
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus-secondary'
import {
  Search,
  Plus,
  Refresh,
  Grid,
  List,
  Document,
  User,
  Avatar,
  OfficeBuilding,
  MoreFilled,
  View,
  Promotion,
  Edit,
  CopyDocument,
  Delete,
  Menu
} from '@element-plus/icons-vue'
import { usePermissionStore } from '@/stores/permission'
import type { PermissionTemplate } from '@/stores/permission'

const permissionStore = usePermissionStore()

const searchKey = ref<string>('')
const selectedCategory = ref<string>('')
const viewMode = ref<'card' | 'list'>('card')
const loading = ref<boolean>(false)
const applying = ref<boolean>(false)
const creating = ref<boolean>(false)

const detailDialogVisible = ref<boolean>(false)
const applyDialogVisible = ref<boolean>(false)
const createDialogVisible = ref<boolean>(false)

const selectedTemplate = ref<PermissionTemplate | null>(null)
const previewTab = ref<'menu' | 'resource'>('menu')

const createFormRef = ref()
const createForm = ref({
  name: '',
  category: 'user',
  description: '',
  source: 'current'
})

const createRules = {
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  description: [{ max: 500, message: '描述不能超过 500 个字符', trigger: 'blur' }]
}

const applyForm = ref({
  roleId: '',
  scope: 'merge',
  permissionTypes: ['menu', 'resource']
})

const templateList = computed(() => permissionStore.templateList)
const roleList = computed(() => permissionStore.roleList)

const filteredTemplates = computed(() => {
  let list = templateList.value

  if (searchKey.value) {
    const key = searchKey.value.toLowerCase()
    list = list.filter(
      item => item.name.toLowerCase().includes(key) || item.description.toLowerCase().includes(key)
    )
  }

  if (selectedCategory.value) {
    list = list.filter(item => item.category === selectedCategory.value)
  }

  return list
})

const handleSearch = () => {
  // 搜索逻辑由 computed 自动处理
}

const handleCategoryChange = () => {
  // 分类过滤由 computed 自动处理
}

const getCategoryType = (category: string): string => {
  const types = {
    system: 'info',
    user: 'primary',
    role: 'success',
    department: 'warning'
  }
  return types[category as keyof typeof types] || 'info'
}

const getCategoryLabel = (category: string): string => {
  const labels = {
    system: '系统模板',
    user: '用户模板',
    role: '角色模板',
    department: '部门模板'
  }
  return labels[category as keyof typeof labels] || category
}

const formatDate = (timestamp: number): string => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString('zh-CN')
}

const handleTemplateCommand = (command: string, template: PermissionTemplate) => {
  switch (command) {
    case 'view':
      viewTemplate(template)
      break
    case 'apply':
      applyTemplate(template)
      break
    case 'edit':
      editTemplate(template)
      break
    case 'copy':
      copyTemplate(template)
      break
    case 'delete':
      deleteTemplate(template)
      break
  }
}

const viewTemplate = (template: PermissionTemplate) => {
  selectedTemplate.value = template
  previewTab.value = 'menu'
  detailDialogVisible.value = true
}

const applyTemplate = (template: PermissionTemplate) => {
  selectedTemplate.value = template
  applyForm.value = {
    roleId: '',
    scope: 'merge',
    permissionTypes: ['menu', 'resource']
  }
  applyDialogVisible.value = true
}

const editTemplate = (template: PermissionTemplate) => {
  ElMessage.info(`编辑模板: ${template.name}`)
  // 打开编辑对话框
}

const copyTemplate = (template: PermissionTemplate) => {
  ElMessageBox.prompt('请输入新模板名称', '复制模板', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: `${template.name} - 副本`
  })
    .then(({ value }) => {
      permissionStore.copyTemplate(template.id, value)
      ElMessage.success('模板复制成功')
    })
    .catch(action => {
      if (action === 'cancel' || action === 'close') return
      ElMessage.error('模板复制失败')
    })
}

const deleteTemplate = async (template: PermissionTemplate) => {
  try {
    await ElMessageBox.confirm(`确定要删除模板 "${template.name}" 吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await permissionStore.deleteTemplate(template.id)
    ElMessage.success('模板删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('模板删除失败')
    }
  }
}

const createTemplate = () => {
  createForm.value = {
    name: '',
    category: 'user',
    description: '',
    source: 'current'
  }
  createDialogVisible.value = true
}

const confirmCreateTemplate = async () => {
  if (!createFormRef.value) return

  try {
    await createFormRef.value.validate()
    creating.value = true

    await permissionStore.createTemplate(createForm.value)
    ElMessage.success('模板创建成功')
    createDialogVisible.value = false
  } catch (error) {
    if (error !== false) {
      ElMessage.error('模板创建失败')
    }
  } finally {
    creating.value = false
  }
}

const confirmApplyTemplate = async () => {
  if (!selectedTemplate.value || !applyForm.value.roleId) {
    ElMessage.warning('请选择目标角色')
    return
  }

  try {
    applying.value = true

    await permissionStore.applyTemplate({
      templateId: selectedTemplate.value.id,
      roleId: applyForm.value.roleId,
      scope: applyForm.value.scope,
      permissionTypes: applyForm.value.permissionTypes
    })

    ElMessage.success('模板应用成功')
    applyDialogVisible.value = false
  } catch (error) {
    ElMessage.error('模板应用失败')
  } finally {
    applying.value = false
  }
}

const previewTemplate = (template: PermissionTemplate) => {
  selectedTemplate.value = template
  previewTab.value = 'menu'
  detailDialogVisible.value = true
}

const refreshTemplates = async () => {
  loading.value = true
  try {
    await permissionStore.loadTemplates()
    ElMessage.success('模板列表已刷新')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    loading.value = false
  }
}

// 初始化
permissionStore.loadTemplates()
</script>

<style lang="less" scoped>
.template-container {
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 120px);
}

.header-bar {
  margin-bottom: 16px;
  padding: 16px;
  background: #f5f6f7;
  border-radius: 4px;
}

.filter-form {
  margin: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.card-view {
  margin-top: 16px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.template-card {
  border: 1px solid #dee0e3;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  &.system-template {
    border-color: #409eff;
    background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.template-icon {
  color: #409eff;
}

.template-actions {
  display: flex;
  gap: 4px;
}

.card-content {
  margin-bottom: 16px;
}

.template-name {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.template-desc {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.template-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.template-time {
  font-size: 12px;
  color: #8f959e;
}

.template-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #606266;

  .el-icon {
    color: #8f959e;
  }
}

.card-footer {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.list-view {
  margin-top: 16px;
}

.template-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .template-icon {
    color: #409eff;
  }

  .name-text {
    font-weight: 500;
  }
}

.template-detail {
  .permission-preview {
    margin-top: 16px;

    h4 {
      margin: 0 0 12px 0;
      font-size: 14px;
      font-weight: 500;
    }
  }
}

.permission-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 8px;
  background: #f5f6f7;
  border-radius: 4px;
}

.apply-template {
  .apply-preview {
    margin-top: 16px;
    padding: 16px;
    background: #f5f6f7;
    border-radius: 4px;

    h4 {
      margin: 0 0 8px 0;
      font-size: 14px;
      font-weight: 500;
    }

    p {
      margin: 0 0 8px 0;
      color: #606266;
    }

    ul {
      margin: 0;
      padding-left: 20px;
      color: #606266;
    }
  }
}
</style>
