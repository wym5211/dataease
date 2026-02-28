import { defineStore } from 'pinia'
import { ref, computed, readonly } from 'vue'

export interface MenuNode {
  id: string
  name: string
  type: string
  path?: string
  hasPermission: boolean
  children?: MenuNode[]
  createTime?: string
  parentId?: string
}

export interface ResourceNode {
  id: string
  name: string
  type: string
  hasPermission: boolean
  permissions: string[]
  children?: ResourceNode[]
  createTime?: string
  creator?: string
  parentId?: string
}

export interface Role {
  id: string
  name: string
  description: string
}

// 类型别名，用于兼容不同组件中的命名
export type MenuItem = MenuNode
export type RoleItem = Role
export type PermissionChange = {
  id: string
  type: 'grant' | 'revoke'
  description: string
}

export interface PermissionTemplate {
  id: string
  name: string
  description: string
  category: string
  tags: string[]
  permissions: Record<string, string[]> | { menus?: any[]; resources?: any[] }
  usageCount: number
  createdAt: string
  updatedAt: string
  creator: string
  menuCount?: number
  resourceCount?: number
}

export interface AuditLog {
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

export interface PermissionState {
  roles: Role[]
  selectedRoleId: string
  menuTreeData: MenuNode[]
  resourceTreeData: Record<string, ResourceNode[]>
  templates: PermissionTemplate[]
  auditLogs: AuditLog[]
  // 变更记录
  menuChanges: {
    grants: MenuNode[]
    revokes: MenuNode[]
  }
  resourceChanges: {
    grants: ResourceNode[]
    revokes: ResourceNode[]
  }
  // 联动状态
  linkageEnabled: boolean
  // 缓存状态
  cacheVersion: number
}

export const usePermissionStore = defineStore('permissionManager', () => {
  // 状态
  const state = ref<PermissionState>({
    roles: [
      { id: 'role_001', name: '超级管理员', description: '拥有所有权限' },
      { id: 'role_002', name: '普通用户', description: '基础权限用户' },
      { id: 'role_003', name: '数据分析师', description: '数据分析相关权限' }
    ],
    selectedRoleId: '',
    menuTreeData: [],
    resourceTreeData: {},
    templates: [],
    auditLogs: [],
    menuChanges: { grants: [], revokes: [] },
    resourceChanges: { grants: [], revokes: [] },
    linkageEnabled: true,
    cacheVersion: 0
  })

  // 计算属性
  const hasMenuChanges = computed(
    () => state.value.menuChanges.grants.length > 0 || state.value.menuChanges.revokes.length > 0
  )

  const hasResourceChanges = computed(
    () =>
      state.value.resourceChanges.grants.length > 0 ||
      state.value.resourceChanges.revokes.length > 0
  )

  const hasAnyChanges = computed(() => hasMenuChanges.value || hasResourceChanges.value)

  const selectedRole = computed(() =>
    state.value.roles.find(role => role.id === state.value.selectedRoleId)
  )

  // 获取角色列表
  const loadRoles = async () => {
    try {
      // TODO: 调用API获取角色列表
      // const response = await getRoles()
      // state.value.roles = response.data

      // 模拟数据
      state.value.roles = [
        { id: 'role_001', name: '超级管理员', description: '拥有所有权限' },
        { id: 'role_002', name: '普通用户', description: '基础权限用户' },
        { id: 'role_003', name: '数据分析师', description: '数据分析相关权限' }
      ]
    } catch (error) {
      console.error('加载角色列表失败:', error)
      throw error
    }
  }

  // 选择角色
  const selectRole = (roleId: string) => {
    if (state.value.selectedRoleId === roleId) return

    state.value.selectedRoleId = roleId
    // 清空之前的变更记录
    state.value.menuChanges = { grants: [], revokes: [] }
    state.value.resourceChanges = { grants: [], revokes: [] }
    state.value.cacheVersion++
  }

  // 加载菜单权限
  const loadMenuPermissions = async () => {
    try {
      // TODO: 调用API获取角色菜单权限
      // const response = await getMenuPermissions(state.value.selectedRoleId)
      // state.value.menuTreeData = response.data

      // 模拟数据 - 无论是否选择角色都加载菜单结构
      console.log('加载菜单数据...')
      state.value.menuTreeData = [
        {
          id: 'menu_001',
          name: '系统管理',
          type: 'folder',
          hasPermission: true,
          children: [
            {
              id: 'menu_001_01',
              name: '用户管理',
              type: 'menu',
              path: '/system/users',
              hasPermission: true,
              createTime: '2024-01-01 10:00:00'
            },
            {
              id: 'menu_001_02',
              name: '角色管理',
              type: 'menu',
              path: '/system/roles',
              hasPermission: false,
              createTime: '2024-01-01 10:00:00'
            }
          ]
        }
      ]
    } catch (error) {
      console.error('加载菜单权限失败:', error)
      throw error
    }
  }

  // 加载资源权限
  const loadResourcePermissions = async (resourceType: string) => {
    if (!state.value.selectedRoleId) return

    try {
      // TODO: 调用API获取角色资源权限
      // const response = await getResourcePermissions(state.value.selectedRoleId, resourceType)
      // state.value.resourceTreeData[resourceType] = response.data

      // 模拟数据
      const mockData = {
        dashboard: [
          {
            id: 'dashboard_001',
            name: '销售分析仪表板',
            type: 'dashboard',
            hasPermission: true,
            permissions: ['view', 'share'],
            createTime: '2024-01-01 10:00:00',
            creator: '张三'
          }
        ],
        dataset: [
          {
            id: 'dataset_001',
            name: '销售数据',
            type: 'dataset',
            hasPermission: true,
            permissions: ['view', 'create', 'update'],
            createTime: '2024-01-01 10:00:00',
            creator: '张三'
          }
        ]
      }

      state.value.resourceTreeData[resourceType] =
        mockData[resourceType as keyof typeof mockData] || []
    } catch (error) {
      console.error(`加载${resourceType}权限失败:`, error)
      throw error
    }
  }

  // 更新菜单权限
  const updateMenuPermission = (menuId: string, hasPermission: boolean) => {
    const updateNodePermission = (nodes: MenuNode[]): boolean => {
      for (const node of nodes) {
        if (node.id === menuId) {
          const oldPermission = node.hasPermission
          node.hasPermission = hasPermission

          // 记录变更
          if (oldPermission !== hasPermission) {
            if (hasPermission) {
              // 授权
              state.value.menuChanges.grants = state.value.menuChanges.grants.filter(
                item => item.id !== menuId
              )
              state.value.menuChanges.grants.push(node)
              state.value.menuChanges.revokes = state.value.menuChanges.revokes.filter(
                item => item.id !== menuId
              )
            } else {
              // 撤销
              state.value.menuChanges.revokes = state.value.menuChanges.revokes.filter(
                item => item.id !== menuId
              )
              state.value.menuChanges.revokes.push(node)
              state.value.menuChanges.grants = state.value.menuChanges.grants.filter(
                item => item.id !== menuId
              )
            }
          }

          state.value.cacheVersion++
          return true
        }

        if (node.children) {
          if (updateNodePermission(node.children)) {
            return true
          }
        }
      }
      return false
    }

    updateNodePermission(state.value.menuTreeData)
  }

  // 更新资源权限
  const updateResourcePermission = (
    resourceType: string,
    resourceId: string,
    permissions: string[]
  ) => {
    const nodes = state.value.resourceTreeData[resourceType]
    if (!nodes) return

    const updateNode = (nodes: ResourceNode[]): boolean => {
      for (const node of nodes) {
        if (node.id === resourceId) {
          const oldHasPermission = node.hasPermission
          const oldPermissions = [...node.permissions]

          node.permissions = permissions
          node.hasPermission = permissions.length > 0

          // 记录变更
          if (
            oldHasPermission !== node.hasPermission ||
            JSON.stringify(oldPermissions) !== JSON.stringify(permissions)
          ) {
            const changeNode = { ...node, type: resourceType }

            if (node.hasPermission) {
              // 授权或更新
              state.value.resourceChanges.grants = state.value.resourceChanges.grants.filter(
                item => !(item.id === resourceId && item.type === resourceType)
              )
              state.value.resourceChanges.grants.push(changeNode)
              state.value.resourceChanges.revokes = state.value.resourceChanges.revokes.filter(
                item => !(item.id === resourceId && item.type === resourceType)
              )
            } else {
              // 撤销
              state.value.resourceChanges.revokes = state.value.resourceChanges.revokes.filter(
                item => !(item.id === resourceId && item.type === resourceType)
              )
              state.value.resourceChanges.revokes.push(changeNode)
              state.value.resourceChanges.grants = state.value.resourceChanges.grants.filter(
                item => !(item.id === resourceId && item.type === resourceType)
              )
            }
          }

          state.value.cacheVersion++
          return true
        }

        if (node.children) {
          if (updateNode(node.children)) {
            return true
          }
        }
      }
      return false
    }

    updateNode(nodes)
  }

  // 保存菜单权限变更
  const saveMenuPermissionChanges = async () => {
    if (!hasMenuChanges.value) return

    try {
      // TODO: 调用API保存菜单权限变更
      // await saveMenuPermissions({
      //   roleId: state.value.selectedRoleId,
      //   grants: state.value.menuChanges.grants.map(item => item.id),
      //   revokes: state.value.menuChanges.revokes.map(item => item.id)
      // })

      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 500))

      // 清空变更记录
      state.value.menuChanges = { grants: [], revokes: [] }
      state.value.cacheVersion++

      console.log('菜单权限变更已保存')
    } catch (error) {
      console.error('保存菜单权限变更失败:', error)
      throw error
    }
  }

  // 保存资源权限变更
  const saveResourcePermissionChanges = async () => {
    if (!hasResourceChanges.value) return

    try {
      // TODO: 调用API保存资源权限变更
      // await saveResourcePermissions({
      //   roleId: state.value.selectedRoleId,
      //   changes: {
      //     grants: state.value.resourceChanges.grants,
      //     revokes: state.value.resourceChanges.revokes
      //   }
      // })

      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 500))

      // 清空变更记录
      state.value.resourceChanges = { grants: [], revokes: [] }
      state.value.cacheVersion++

      console.log('资源权限变更已保存')
    } catch (error) {
      console.error('保存资源权限变更失败:', error)
      throw error
    }
  }

  // 保存所有权限变更
  const saveAllPermissionChanges = async () => {
    if (!hasAnyChanges.value) return

    const promises = []

    if (hasMenuChanges.value) {
      promises.push(saveMenuPermissionChanges())
    }

    if (hasResourceChanges.value) {
      promises.push(saveResourcePermissionChanges())
    }

    await Promise.all(promises)
  }

  // 重置权限变更
  const resetPermissionChanges = () => {
    state.value.menuChanges = { grants: [], revokes: [] }
    state.value.resourceChanges = { grants: [], revokes: [] }
    state.value.cacheVersion++
  }

  // 联动机制：菜单权限变更时同步相关资源权限
  const syncMenuResourcePermissions = (menuId: string, hasPermission: boolean) => {
    if (!state.value.linkageEnabled) return

    // TODO: 根据菜单ID查找关联的资源
    // 这里可以实现具体的联动逻辑
    console.log(`菜单权限变更联动: ${menuId} -> ${hasPermission ? '授权' : '撤销'}`)
  }

  // 联动机制：资源权限变更时同步相关菜单权限
  const syncResourceMenuPermissions = (
    resourceType: string,
    resourceId: string,
    permissions: string[]
  ) => {
    if (!state.value.linkageEnabled) return

    // TODO: 根据资源ID查找关联的菜单
    // 这里可以实现具体的联动逻辑
    console.log(`资源权限变更联动: ${resourceType}/${resourceId} -> ${permissions.join(',')}`)
  }

  // 加载权限模板
  const loadPermissionTemplates = async () => {
    try {
      // TODO: 调用API获取权限模板
      // const response = await getPermissionTemplates()
      // state.value.templates = response.data

      // 模拟数据
      state.value.templates = [
        {
          id: 'template_001',
          name: '管理员权限模板',
          description: '包含系统管理和数据分析的完整权限',
          category: 'system',
          tags: ['管理员', '完整权限'],
          permissions: {
            menus: [
              { id: 'm1', name: '系统管理' },
              { id: 'm2', name: '用户管理' }
            ],
            resources: [
              { id: 'r1', name: '销售仪表板' },
              { id: 'r2', name: '数据集' }
            ]
          },
          usageCount: 15,
          createdAt: '2024-01-01 10:00:00',
          updatedAt: '2024-01-15 14:30:00',
          creator: '系统管理员',
          menuCount: 2,
          resourceCount: 2
        }
      ]
    } catch (error) {
      console.error('加载权限模板失败:', error)
      throw error
    }
  }

  // 应用权限模板
  const applyPermissionTemplate = async (
    templateId: string,
    targetRoleId: string,
    options: {
      mode: 'replace' | 'merge' | 'append'
      scope: string[]
    }
  ) => {
    try {
      // TODO: 调用API应用权限模板
      // await applyTemplate({ templateId, targetRoleId, options })

      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 1000))

      console.log(`权限模板已应用: ${templateId} -> ${targetRoleId}`)
    } catch (error) {
      console.error('应用权限模板失败:', error)
      throw error
    }
  }

  // 加载审计日志
  const loadAuditLogs = async (params?: {
    startDate?: string
    endDate?: string
    operationType?: string
    targetType?: string
    keyword?: string
    page?: number
    pageSize?: number
  }) => {
    try {
      // TODO: 调用API获取审计日志
      // const response = await getAuditLogs(params)
      // state.value.auditLogs = response.data

      // 模拟数据
      state.value.auditLogs = [
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
        }
      ]
    } catch (error) {
      console.error('加载审计日志失败:', error)
      throw error
    }
  }

  // 回滚权限变更
  const rollbackPermissionChange = async (auditId: string) => {
    try {
      // TODO: 调用API回滚权限变更
      // await rollbackPermission(auditId)

      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 500))

      console.log(`权限变更已回滚: ${auditId}`)
    } catch (error) {
      console.error('回滚权限变更失败:', error)
      throw error
    }
  }

  // 组件兼容方法
  const roleList = computed(() => state.value.roles)
  const menuTree = computed(() => state.value.menuTreeData)
  const resourceTree = computed(() => state.value.resourceTreeData)
  const hasChanges = computed(() => hasAnyChanges.value)
  const changeSummary = computed(() => {
    const changes: PermissionChange[] = []
    state.value.menuChanges.grants.forEach(item => {
      changes.push({ id: item.id, type: 'grant', description: `授权菜单: ${item.name}` })
    })
    state.value.menuChanges.revokes.forEach(item => {
      changes.push({ id: item.id, type: 'revoke', description: `撤销菜单: ${item.name}` })
    })
    return changes
  })

  // 模板相关计算属性和方法
  const templateList = computed(() => state.value.templates)

  const loadTemplates = async () => {
    await loadPermissionTemplates()
  }

  const copyTemplate = async (templateId: string, newName: string) => {
    const template = state.value.templates.find(t => t.id === templateId)
    if (template) {
      const newTemplate = {
        ...template,
        id: `template_${Date.now()}`,
        name: newName,
        category: 'user' as const,
        createdAt: new Date().toLocaleString('zh-CN'),
        updatedAt: new Date().toLocaleString('zh-CN'),
        creator: '当前用户'
      }
      state.value.templates.push(newTemplate)
    }
  }

  const deleteTemplate = async (templateId: string) => {
    const index = state.value.templates.findIndex(t => t.id === templateId)
    if (index > -1) {
      state.value.templates.splice(index, 1)
    }
  }

  const createTemplate = async (data: any) => {
    const newTemplate = {
      id: `template_${Date.now()}`,
      name: data.name,
      description: data.description,
      category: data.category,
      tags: [],
      permissions: {
        menu: ['view', 'create', 'update'],
        dashboard: ['view', 'create'],
        dataset: ['view'],
        datasource: ['view']
      },
      usageCount: 0,
      createdAt: new Date().toLocaleString('zh-CN'),
      updatedAt: new Date().toLocaleString('zh-CN'),
      creator: '当前用户',
      menuCount: 3,
      resourceCount: 4
    }
    state.value.templates.push(newTemplate as PermissionTemplate)
  }

  const applyTemplate = async (data: any) => {
    await applyPermissionTemplate(data.templateId, data.roleId, {
      mode: data.scope === 'replace' ? 'replace' : data.scope === 'merge' ? 'merge' : 'append',
      scope: data.permissionTypes || ['menu', 'resource']
    })
  }

  const loadRoleList = async () => {
    await loadRoles()
  }

  const loadRolePermissions = async (roleId: string) => {
    selectRole(roleId)
    await loadMenuPermissions()
  }

  const loadMenuTree = async () => {
    await loadMenuPermissions()
  }

  const updatePermission = (id: string, checked: boolean) => {
    updateMenuPermission(id, checked)
  }

  const savePermissionChanges = async (roleId: string) => {
    await saveMenuPermissionChanges()
  }

  const resetChanges = () => {
    resetPermissionChanges()
  }

  const hasPermission = (id: string): boolean => {
    const findNode = (nodes: MenuNode[]): boolean => {
      for (const node of nodes) {
        if (node.id === id) return node.hasPermission
        if (node.children?.length) {
          const found = findNode(node.children)
          if (found !== undefined) return found
        }
      }
      return false
    }
    return findNode(state.value.menuTreeData)
  }

  const getCheckedKeys = (): string[] => {
    const keys: string[] = []
    const collectKeys = (nodes: MenuNode[]) => {
      nodes.forEach(node => {
        if (node.hasPermission) {
          keys.push(node.id)
        }
        if (node.children?.length) {
          collectKeys(node.children)
        }
      })
    }
    collectKeys(state.value.menuTreeData)
    return keys
  }

  const getRelatedResources = (menuId: string): ResourceNode[] => {
    // TODO: 实现根据菜单ID查找关联资源的逻辑
    return []
  }

  const getNodePermissions = (nodeId: string): string[] => {
    const node = findNodeInTree(state.value.menuTreeData, nodeId)
    if (node) {
      // 根据节点类型返回权限列表
      return node.hasPermission ? ['view', 'create', 'update', 'delete'] : []
    }
    return []
  }

  const updateNodePermissions = (nodeId: string, permissions: string[]) => {
    // TODO: 实现更新节点权限的逻辑
    console.log(`更新节点权限: ${nodeId}`, permissions)
  }

  const findNodeInTree = (tree: MenuNode[], id: string): MenuNode | null => {
    for (const node of tree) {
      if (node.id === id) return node
      if (node.children?.length) {
        const found = findNodeInTree(node.children, id)
        if (found) return found
      }
    }
    return null
  }

  return {
    // 状态
    state: readonly(state),
    selectedRole,
    hasMenuChanges,
    hasResourceChanges,
    hasAnyChanges,

    // 组件兼容计算属性
    roleList,
    menuTree,
    resourceTree,
    hasChanges,
    changeSummary,
    templateList,

    // 模板相关方法
    loadTemplates,
    copyTemplate,
    deleteTemplate,
    createTemplate,
    applyTemplate,

    // 原始方法
    loadRoles,
    selectRole,
    loadMenuPermissions,
    loadResourcePermissions,
    updateMenuPermission,
    updateResourcePermission,
    saveMenuPermissionChanges,
    saveResourcePermissionChanges,
    saveAllPermissionChanges,
    resetPermissionChanges,
    syncMenuResourcePermissions,
    syncResourceMenuPermissions,
    loadPermissionTemplates,
    applyPermissionTemplate,
    loadAuditLogs,
    rollbackPermissionChange,

    // 组件兼容方法
    loadRoleList,
    loadRolePermissions,
    loadMenuTree,
    updatePermission,
    savePermissionChanges,
    resetChanges,
    hasPermission,
    getCheckedKeys,
    getRelatedResources,
    getNodePermissions,
    updateNodePermissions
  }
})
