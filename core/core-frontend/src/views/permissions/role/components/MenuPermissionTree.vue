<template>
  <div class="menu-permission-tree">
    <div class="tree-toolbar">
      <el-button-group size="small">
        <el-button @click="expandAll">
          <el-icon><ArrowDown /></el-icon>
          展开全部
        </el-button>
        <el-button @click="collapseAll">
          <el-icon><ArrowUp /></el-icon>
          收起全部
        </el-button>
        <el-button @click="selectAll">
          <el-icon><Check /></el-icon>
          全选
        </el-button>
        <el-button @click="clearAll">
          <el-icon><Close /></el-icon>
          清空
        </el-button>
      </el-button-group>
    </div>

    <el-tree
      ref="treeRef"
      :data="menuTree"
      :props="defaultProps"
      node-key="id"
      show-checkbox
      :default-expand-all="false"
      :expand-on-click-node="false"
      @check-change="handleCheckChange"
    >
      <template #default="{ data }">
        <span class="custom-tree-node">
          <el-icon v-if="data.icon" class="menu-icon">
            <component :is="getIcon(data.icon)" />
          </el-icon>
          <span>{{ data.name }}</span>
          <el-tag
            v-if="data.type"
            size="small"
            :type="data.type === 'menu' ? 'primary' : 'info'"
            class="node-type"
          >
            {{ data.type === 'menu' ? '菜单' : '按钮' }}
          </el-tag>
        </span>
      </template>
    </el-tree>

    <div v-if="loading" class="tree-loading">
      <el-skeleton :rows="5" animated />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { ArrowDown, ArrowUp, Check, Close } from '@element-plus/icons-vue'
import { getMenuTree } from '../api'
import type { MenuPermission } from '../types'

interface Props {
  roleId?: string
  modelValue: string[]
}

interface Emits {
  (e: 'update:modelValue', value: string[]): void
  (e: 'change', value: string[]): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const treeRef = ref()
const loading = ref(false)
const menuTree = ref<MenuPermission[]>([])

const defaultProps = {
  children: 'children',
  label: 'name'
}

// 图标映射
const iconMap: Record<string, string> = {
  dashboard: 'Odometer',
  chart: 'TrendCharts',
  table: 'Grid',
  setting: 'Setting',
  user: 'User',
  menu: 'Menu',
  file: 'Document',
  folder: 'Folder'
}

const getIcon = (iconName: string) => {
  return iconMap[iconName] || 'Folder'
}

interface BackendMenuItem {
  id: string | number
  name: string
  path?: string
  type?: number
  meta?: {
    title?: string
    icon?: string
  }
  children?: BackendMenuItem[]
}

interface AdaptedMenuPermission extends MenuPermission {
  id: string
  name: string
  type: string
  path: string
  icon: string
  children?: AdaptedMenuPermission[]
}

// 数据适配：将后端MenuVO格式转换为组件期望的格式
const adaptMenuData = (menuList: BackendMenuItem[]): MenuPermission[] => {
  if (!menuList || !Array.isArray(menuList)) return []

  return menuList.map(menu => {
    const adapted: AdaptedMenuPermission = {
      id: String(menu.id || ''),
      name: menu.meta?.title || menu.name || '',
      type: menu.type === 1 ? 'button' : 'menu',
      path: menu.path || '',
      icon: menu.meta?.icon || '',
      children: undefined
    }

    // 递归处理子菜单
    if (menu.children && menu.children.length > 0) {
      adapted.children = adaptMenuData(menu.children)
    }

    return adapted
  })
}

// 加载菜单树
const loadMenuTree = async () => {
  loading.value = true
  try {
    const res = await getMenuTree()
    // 将后端MenuVO格式转换为组件期望的格式
    menuTree.value = adaptMenuData(res)

    // 设置选中状态
    await nextTick()
    if (props.modelValue.length > 0) {
      treeRef.value?.setCheckedKeys(props.modelValue)
    }
  } catch (error) {
    console.error('加载菜单树失败:', error)
    menuTree.value = []
  } finally {
    loading.value = false
  }
}

// 展开全部
const expandAll = () => {
  treeRef.value?.expandAll()
}

// 收起全部
const collapseAll = () => {
  treeRef.value?.collapseAll()
}

// 全选
const selectAll = () => {
  const allKeys = getAllKeys(menuTree.value)
  treeRef.value?.setCheckedKeys(allKeys)
  emit('update:modelValue', allKeys)
  emit('change', allKeys)
}

// 清空
const clearAll = () => {
  treeRef.value?.setCheckedKeys([])
  emit('update:modelValue', [])
  emit('change', [])
}

// 获取所有节点key
const getAllKeys = (nodes: MenuPermission[]): string[] => {
  const keys: string[] = []
  const traverse = (list: MenuPermission[]) => {
    list.forEach(node => {
      keys.push(node.id)
      if (node.children?.length) {
        traverse(node.children)
      }
    })
  }
  traverse(nodes)
  return keys
}

// 选中变化
const handleCheckChange = () => {
  const checkedKeys = treeRef.value?.getCheckedKeys() || []
  const halfCheckedKeys = treeRef.value?.getHalfCheckedKeys() || []
  const allKeys = [...checkedKeys, ...halfCheckedKeys]
  emit('update:modelValue', allKeys)
  emit('change', allKeys)
}

// 暴露方法
defineExpose({
  getCheckedKeys: () => treeRef.value?.getCheckedKeys() || [],
  setCheckedKeys: (keys: string[]) => treeRef.value?.setCheckedKeys(keys),
  loadMenuTree
})

// 监听roleId变化
watch(
  () => props.roleId,
  () => {
    loadMenuTree()
  },
  { immediate: true }
)
</script>

<style scoped lang="less">
.menu-permission-tree {
  padding: 16px;

  .tree-toolbar {
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid #dee0e3;
  }

  .custom-tree-node {
    display: flex;
    align-items: center;
    gap: 8px;

    .menu-icon {
      color: #8f959e;
    }

    .node-type {
      margin-left: 8px;
    }
  }

  .tree-loading {
    padding: 20px;
  }

  :deep(.el-tree-node__content) {
    height: 36px;
  }
}
</style>
