<template>
  <div class="permission-assign">
    <!-- 角色选择器 -->
    <div class="role-selector">
      <el-alert
        v-if="selectedRole"
        :title="`当前角色：${selectedRole.name} (${selectedRole.code})`"
        type="info"
        :closable="false"
        show-icon
      />
      <el-alert v-else title="请先选择一个角色" type="warning" :closable="false" show-icon />
    </div>

    <!-- 权限标签页 -->
    <el-tabs v-model="activeTab" type="border-card" class="permission-tabs">
      <!-- 菜单权限 -->
      <el-tab-pane label="菜单权限" name="menu">
        <div v-if="selectedRole" class="tab-content">
          <div class="tab-toolbar">
            <el-button type="primary" @click="saveMenuPermissions" :loading="saving">
              保存菜单权限
            </el-button>
            <el-button @click="resetMenuPermissions">重置</el-button>
          </div>
          <MenuPermissionTree
            ref="menuTreeRef"
            :role-id="selectedRole.id"
            v-model="menuPermissions"
            @change="handleMenuChange"
          />
        </div>
        <div v-else class="empty-tip">
          <el-empty description="请先选择一个角色" />
        </div>
      </el-tab-pane>

      <!-- 资源权限 -->
      <el-tab-pane label="资源权限" name="resource">
        <div v-if="selectedRole" class="tab-content">
          <div class="tab-toolbar">
            <el-button type="primary" @click="saveResourcePermissions" :loading="saving">
              保存资源权限
            </el-button>
            <el-button @click="resetResourcePermissions">重置</el-button>
          </div>
          <ResourcePermission
            ref="resourceRef"
            :role="selectedRole"
            @change="handleResourceChange"
          />
        </div>
        <div v-else class="empty-tip">
          <el-empty description="请先选择一个角色" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 保存确认对话框 -->
    <el-dialog v-model="confirmVisible" title="确认保存" width="400px">
      <p>确定要保存权限变更吗？</p>
      <template #footer>
        <el-button @click="confirmVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSave" :loading="saving"> 确定 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus-secondary'
import MenuPermissionTree from './MenuPermissionTree.vue'
import ResourcePermission from './ResourcePermission.vue'
import type { Role, ResourcePermission as ResourcePermissionType } from '../types'

interface Props {
  role: Role | null
}

const props = defineProps<Props>()

const activeTab = ref('menu')
const saving = ref(false)
const confirmVisible = ref(false)
const pendingSaveType = ref<'menu' | 'resource' | null>(null)

const menuTreeRef = ref<InstanceType<typeof MenuPermissionTree>>()
const resourceRef = ref<InstanceType<typeof ResourcePermission>>()

const selectedRole = ref<Role | null>(props.role)
const menuPermissions = ref<string[]>([])
const resourcePermissions = ref<ResourcePermissionType[]>([])

// 监听role变化
watch(
  () => props.role,
  newRole => {
    selectedRole.value = newRole
    if (newRole) {
      loadRolePermissions(newRole.id)
    }
  },
  { immediate: true }
)

// 加载角色权限
const loadRolePermissions = async (_roleId: string) => {
  try {
    // TODO: 后端API就绪后启用
    // const res = await getRolePermissions(roleId)
    // menuPermissions.value = res.menus.map(m => m.id)
    // resourcePermissions.value = res.resources

    // 模拟加载
    menuPermissions.value = []
    resourcePermissions.value = []
  } catch (error) {
    console.error('加载权限失败:', error)
  }
}

// 菜单权限变化
const handleMenuChange = (keys: string[]) => {
  menuPermissions.value = keys
}

// 资源权限变化
const handleResourceChange = (data: ResourcePermissionType[]) => {
  resourcePermissions.value = data
}

// 保存菜单权限
const saveMenuPermissions = () => {
  pendingSaveType.value = 'menu'
  confirmVisible.value = true
}

// 保存资源权限
const saveResourcePermissions = () => {
  pendingSaveType.value = 'resource'
  confirmVisible.value = true
}

// 确认保存
const confirmSave = async () => {
  if (!selectedRole.value) return

  saving.value = true
  try {
    if (pendingSaveType.value === 'menu') {
      // TODO: 后端API就绪后启用
      // await saveRolePermissions(selectedRole.value.id, {
      //   menuIds: menuPermissions.value,
      //   resourceIds: resourcePermissions.value.map(r => r.id)
      // })
      ElMessage.success('菜单权限保存成功')
    } else if (pendingSaveType.value === 'resource') {
      const _permissions = resourceRef.value?.getPermissions() || []
      ElMessage.success('资源权限保存成功')
    }
    confirmVisible.value = false
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 重置菜单权限
const resetMenuPermissions = () => {
  if (selectedRole.value) {
    loadRolePermissions(selectedRole.value.id)
    menuTreeRef.value?.loadMenuTree()
  }
}

// 重置资源权限
const resetResourcePermissions = () => {
  if (selectedRole.value) {
    loadRolePermissions(selectedRole.value.id)
  }
}

// 暴露方法
defineExpose({
  refresh: () => {
    if (selectedRole.value) {
      loadRolePermissions(selectedRole.value.id)
    }
  }
})
</script>

<style scoped lang="less">
.permission-assign {
  .role-selector {
    margin-bottom: 16px;
  }

  .permission-tabs {
    .tab-content {
      padding: 16px;

      .tab-toolbar {
        margin-bottom: 16px;
        padding-bottom: 16px;
        border-bottom: 1px solid #dee0e3;
      }
    }

    .empty-tip {
      padding: 48px;
    }
  }
}
</style>
