import { defineStore } from 'pinia'
import { ref, computed, readonly } from 'vue'
import { ElMessage } from 'element-plus-secondary'
import { menuTreeApi, menuPerSaveApi, menuPerApi, busiPerSaveApi, resourcePerApi } from '@/api/auth'
import { getRoleList } from '@/views/permissions/role/api'
import { getDatasetTree, getDatasourceList } from '@/api/dataset'
import { queryTreeApi } from '@/api/visualization/dataVisualization'

export interface MenuNode {
  id: string
  name: string
  type: string
  path?: string
  icon?: string
  sort?: number
  status?: string
  auth?: boolean
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
  code: string
  description?: string
  status: number
  createTime: string
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

// 后端菜单项接口
interface BackendMenuItem {
  id?: number | string
  auth?: boolean
  path: string
  component?: string
  hidden: boolean
  name: string
  inLayout: boolean
  redirect?: string | null
  meta: {
    title: string
    icon?: string | null
  }
  children?: BackendMenuItem[] | null
  plugin: boolean
}

// 需要过滤掉的菜单名称列表
const EXCLUDED_MENUS = ['sys-setting', 'permissions', 'template-market', 'toolbox']

// 转换后端菜单数据到前端格式
const transformMenuData = (backendMenus: BackendMenuItem[]): MenuNode[] => {
  if (!Array.isArray(backendMenus)) return []

  return backendMenus
    .filter(menu => menu && menu.name && !EXCLUDED_MENUS.includes(menu.name)) // 过滤掉空菜单和排除的菜单
    .map(menu => {
      // 检查是否有非空的子节点数组
      const hasChildren = menu.children && Array.isArray(menu.children) && menu.children.length > 0

      const node: MenuNode = {
        id: String(menu.id ?? menu.name),
        name: menu.meta?.title || menu.name,
        type: hasChildren ? 'folder' : 'menu',
        path: menu.path,
        icon: menu.meta?.icon || '',
        auth: menu.auth,
        hasPermission: false,
        parentId: undefined
      }

      // 递归处理子菜单（只在真正有子节点时）
      if (hasChildren) {
        node.children = transformMenuData(menu.children)
        // 为子节点设置 parentId
        node.children.forEach(child => {
          child.parentId = node.id
        })
      }

      return node
    })
}

const applyMenuPermissions = (nodes: MenuNode[], grantedMenuIds: Set<string>, isRoot: boolean) => {
  nodes.forEach(node => {
    const isPublic = node.auth !== true
    node.hasPermission = isRoot || isPublic || grantedMenuIds.has(node.id)
    if (node.children?.length) {
      applyMenuPermissions(node.children, grantedMenuIds, isRoot)
    }
  })
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
    roles: [],
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
      console.log('[Permission Store] 开始加载角色列表...')
      const response = await getRoleList({
        page: 1,
        pageSize: 1000,
        keyword: ''
      })
      console.log('[Permission Store] 角色列表响应:', response)
      console.log('[Permission Store] 角色数量:', response.records?.length || 0)
      console.log('[Permission Store] 角色数据:', response.records)
      state.value.roles = response.records || []
      console.log('[Permission Store] 更新后的 state.value.roles:', state.value.roles)
    } catch (error) {
      console.error('[Permission Store] 加载角色列表失败:', error)
      ElMessage.error('加载角色列表失败')
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
      console.log('加载菜单数据...')

      // 调用后端 API 获取菜单树
      const response = await menuTreeApi()
      console.log('后端返回的菜单数据:', response.data)

      // 转换后端数据格式到前端期望的格式
      const transformedMenus = transformMenuData(response.data || [])

      // 如果有选择角色，加载角色的菜单权限
      if (state.value.selectedRoleId) {
        try {
          const permResponse = await menuPerApi({ id: Number(state.value.selectedRoleId) })
          console.log('角色菜单权限响应:', permResponse)
          const permissionItems = Array.isArray(permResponse?.permissions)
            ? permResponse.permissions
            : Array.isArray(permResponse?.data?.permissions)
            ? permResponse.data.permissions
            : []
          console.log('角色菜单权限 permissions:', permissionItems)
          const isRoot = Boolean(permResponse?.root ?? permResponse?.data?.root)

          const grantedMenuIds = new Set<string>()
          if (permissionItems.length) {
            permissionItems.forEach((p: any) => {
              console.log('Permission item:', p, 'id:', p.id, 'type:', typeof p.id)
              grantedMenuIds.add(String(p.id))
            })
          }
          console.log('有权限的菜单 IDs:', Array.from(grantedMenuIds))
          console.log(
            '菜单节点 IDs:',
            transformedMenus.map((m: MenuNode) => ({ id: m.id, name: m.name }))
          )

          applyMenuPermissions(transformedMenus, grantedMenuIds, isRoot)
        } catch (error) {
          console.error('加载角色菜单权限失败，使用默认值:', error)
          // 如果获取角色权限失败，所有菜单默认有权限
        }
      }

      console.log('最终菜单数据:', transformedMenus)
      state.value.menuTreeData = transformedMenus
    } catch (error) {
      console.error('加载菜单权限失败:', error)
      // 如果 API 调用失败，使用空数组
      state.value.menuTreeData = []
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

      state.value.resourceTreeData = {
        ...state.value.resourceTreeData,
        [resourceType]: mockData[resourceType as keyof typeof mockData] || []
      }
    } catch (error) {
      console.error(`加载${resourceType}权限失败:`, error)
      throw error
    }
  }

  // 加载资源树（资源授权页面使用）
  const loadResourceTree = async (resourceType: string) => {
    try {
      console.log('[Permission Store] 加载资源树:', resourceType)

      let treeData: ResourceNode[] = []

      // 根据资源类型调用不同的API
      if (resourceType === 'dashboard' || resourceType === 'screen' || resourceType === 'chart') {
        // 仪表板、大屏、图表 - 使用可视化API
        // busiFlag: dashboard=dashboard-dataV, screen=screen
        const busiFlag = resourceType === 'dashboard' ? 'dashboard-dataV' : resourceType

        const response = await queryTreeApi({
          busiFlag,
          leaf: false,
          withLeaf: true
        })
        console.log('[Permission Store] 可视化资源响应:', response)

        // 转换数据格式
        const transformVisualizationNode = (node: any): ResourceNode => {
          return {
            id: String(node.id),
            name: node.name,
            type: resourceType,
            hasPermission: false,
            permissions: [],
            createTime: node.updateTime || new Date().toISOString(),
            creator: node.createBy || '未知',
            children: node.children?.map((child: any) => transformVisualizationNode(child))
          }
        }

        treeData = (response || []).map(transformVisualizationNode)
      } else if (resourceType === 'dataset') {
        // 数据集 - 使用数据集API
        const response = await getDatasetTree({
          busiFlag: 'dataset',
          leaf: false,
          withLeaf: true
        })
        console.log('[Permission Store] 数据集资源响应:', response)

        // 转换数据格式
        const transformDatasetNode = (node: any): ResourceNode => {
          return {
            id: String(node.id),
            name: node.name,
            type: 'dataset',
            hasPermission: false,
            permissions: [],
            createTime: node.updateTime || new Date().toISOString(),
            creator: node.createBy || '未知',
            children: node.children?.map((child: any) => transformDatasetNode(child))
          }
        }

        treeData = (response || []).map(transformDatasetNode)
      } else if (resourceType === 'datasource') {
        // 数据源 - 使用数据源API
        const response = await getDatasourceList()
        console.log('[Permission Store] 数据源资源响应:', response)

        // 转换数据格式
        const transformDatasourceNode = (node: any): ResourceNode => {
          return {
            id: String(node.id),
            name: node.name,
            type: 'datasource',
            hasPermission: false,
            permissions: [],
            createTime: node.updateTime || new Date().toISOString(),
            creator: node.createBy || '未知',
            children: node.children?.map((child: any) => transformDatasourceNode(child))
          }
        }

        treeData = (response || []).map(transformDatasourceNode)
      } else {
        // 其他类型暂时使用空数组
        console.log('[Permission Store] 资源类型暂未实现:', resourceType)
        treeData = []
      }

      // 如果有选择角色，加载该角色的资源权限
      if (state.value.selectedRoleId) {
        try {
          // 使用与后端数据库一致的资源类型标识（小写）
          const flagMap: Record<string, string> = {
            dashboard: 'dashboard',
            screen: 'dataV',
            chart: 'dashboard',
            dataset: 'dataset',
            datasource: 'datasource'
          }
          const flag = flagMap[resourceType]

          if (flag) {
            const requestParams = {
              id: state.value.selectedRoleId,
              type: 1,
              flag: flag
            }
            console.log('[Permission Store] 查询资源权限请求:', requestParams)
            const perResponse = await resourcePerApi(requestParams)
            console.log('[Permission Store] 资源权限响应:', perResponse)
            console.log('[Permission Store] 响应 data:', perResponse?.data)
            console.log('[Permission Store] 响应 permissions:', perResponse?.permissions)

            // 获取有权限的资源ID列表
            const grantedResourceIds = new Set<string>()
            // 后端可能直接返回 permissions 或在 data.permissions 中
            const permissionList = perResponse?.permissions || perResponse?.data?.permissions || []
            console.log('[Permission Store] 权限列表:', permissionList)
            if (permissionList.length > 0) {
              permissionList.forEach((r: any) => {
                console.log('[Permission Store] 权限项:', r, 'id:', r.id, 'type:', typeof r.id)
                if (r.id) {
                  grantedResourceIds.add(String(r.id))
                }
              })
            } else {
              console.log('[Permission Store] 权限列表为空，检查数据库是否有记录')
            }
            console.log('[Permission Store] 有权限的资源IDs:', Array.from(grantedResourceIds))

            // 应用权限到资源树
            console.log(
              '[Permission Store] 树节点IDs:',
              treeData.map((n: any) => ({ id: n.id, name: n.name }))
            )
            const applyPermissions = (nodes: ResourceNode[]) => {
              nodes.forEach(node => {
                const hasGrant = grantedResourceIds.has(node.id)
                console.log(
                  '[Permission Store] 检查节点:',
                  node.id,
                  node.name,
                  '是否有权限:',
                  hasGrant
                )
                if (hasGrant) {
                  node.hasPermission = true
                  node.permissions = ['view']
                }
                if (node.children?.length) {
                  applyPermissions(node.children)
                }
              })
            }
            applyPermissions(treeData)
            console.log('[Permission Store] 权限应用完成')
          }
        } catch (error) {
          console.error('[Permission Store] 加载资源权限失败:', error)
        }
      }

      // 使用展开运算符创建新对象以触发响应式更新
      state.value.resourceTreeData = {
        ...state.value.resourceTreeData,
        [resourceType]: treeData
      }
      console.log('[Permission Store] 资源树加载完成，数量:', treeData.length)
    } catch (error) {
      console.error('加载资源树失败:', error)
      ElMessage.error(`加载${resourceType}资源树失败`)
      // 失败时使用空数组
      state.value.resourceTreeData = {
        ...state.value.resourceTreeData,
        [resourceType]: []
      }
      throw error
    }
  }

  // 更新菜单权限
  const updateMenuPermission = (menuId: string, hasPermission: boolean) => {
    console.log('[DEBUG] updateMenuPermission - menuId:', menuId, 'hasPermission:', hasPermission)
    const updateNodePermission = (nodes: MenuNode[]): boolean => {
      for (const node of nodes) {
        if (node.id === menuId) {
          const oldPermission = node.hasPermission
          node.hasPermission = hasPermission
          console.log(
            '[DEBUG] Updated node hasPermission:',
            menuId,
            oldPermission,
            '->',
            hasPermission
          )

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
    console.log('[DEBUG] saveMenuPermissionChanges called')
    console.log('[DEBUG] selectedRoleId:', state.value.selectedRoleId)
    console.log('[DEBUG] hasMenuChanges:', hasMenuChanges.value)
    console.log('[DEBUG] menuTreeData length:', state.value.menuTreeData.length)

    if (!state.value.selectedRoleId) {
      throw new Error('未选择角色')
    }

    try {
      // 收集所有有权限的菜单ID
      const grantedMenuIds = new Set<string>()

      // 从菜单树中获取所有已授权的菜单
      const collectGrantedIds = (nodes: MenuNode[]) => {
        nodes.forEach(node => {
          if (node.hasPermission) {
            grantedMenuIds.add(node.id)
          }
          if (node.children?.length) {
            collectGrantedIds(node.children)
          }
        })
      }
      collectGrantedIds(state.value.menuTreeData)

      console.log('[DEBUG] Collected granted menu IDs:', Array.from(grantedMenuIds))

      const menuIdList = Array.from(grantedMenuIds)
      const invalidMenuIds = menuIdList.filter(menuId => !/^\d+$/.test(menuId))
      if (invalidMenuIds.length > 0) {
        throw new Error(`菜单ID异常，请刷新菜单树后重试：${invalidMenuIds.join(', ')}`)
      }

      // 构建权限项列表 - 包含 weight 字段以匹配后端 PermissionItem 格式
      const permissions = menuIdList.map(menuId => ({
        id: Number(menuId),
        weight: 1
      }))

      console.log('[DEBUG] Request payload:', {
        id: Number(state.value.selectedRoleId),
        permissions
      })

      // 调用后端API保存
      const result = await menuPerSaveApi({
        id: Number(state.value.selectedRoleId),
        permissions
      })
      console.log('[DEBUG] API response:', result)

      // 清空变更记录
      state.value.menuChanges = { grants: [], revokes: [] }
      state.value.cacheVersion++

      ElMessage.success('菜单权限保存成功')
    } catch (error) {
      console.error('保存菜单权限变更失败:', error)
      ElMessage.error('保存失败: ' + (error as Error).message)
      throw error
    }
  }

  // 保存资源权限变更
  const saveResourcePermissionChanges = async () => {
    if (!hasResourceChanges.value) return

    try {
      // 按资源类型分组收集当前有权限的资源ID
      const permissionsByType: Record<string, Set<string>> = {}

      // 初始化所有资源类型的空集合
      Object.keys(state.value.resourceTreeData).forEach(type => {
        permissionsByType[type] = new Set()
      })

      // 从资源树中收集所有当前有权限的资源
      Object.entries(state.value.resourceTreeData).forEach(([type, resources]) => {
        const collectGranted = (nodes: ResourceNode[]) => {
          nodes.forEach(node => {
            if (node.hasPermission) {
              permissionsByType[type].add(node.id)
            }
            if (node.children?.length) {
              collectGranted(node.children)
            }
          })
        }
        collectGranted(resources)
      })

      // 应用变更记录
      state.value.resourceChanges.grants.forEach(node => {
        if (permissionsByType[node.type]) {
          permissionsByType[node.type].add(node.id)
        }
      })
      state.value.resourceChanges.revokes.forEach(node => {
        if (permissionsByType[node.type]) {
          permissionsByType[node.type].delete(node.id)
        }
      })

      // 只保存有变更的资源类型（避免清空其他类型的权限）
      const savePromises: Promise<any>[] = []

      // 资源类型映射（与查询时保持一致）
      const saveFlagMap: Record<string, string> = {
        dashboard: 'dashboard',
        screen: 'dataV',
        chart: 'dashboard',
        dataset: 'dataset',
        datasource: 'datasource'
      }

      // 收集有变更的资源类型
      const changedTypes = new Set<string>()
      state.value.resourceChanges.grants.forEach(node => changedTypes.add(node.type))
      state.value.resourceChanges.revokes.forEach(node => changedTypes.add(node.type))

      // 只保存有变更的资源类型
      changedTypes.forEach(type => {
        const idSet = permissionsByType[type] || new Set<string>()
        const flag = saveFlagMap[type] || type
        const permissions = Array.from(idSet).map(id => ({
          id: id,
          weight: 1
        }))

        console.log('[DEBUG] 保存资源权限:', {
          id: state.value.selectedRoleId,
          type: 1, // 1=角色
          flag,
          permissions
        })

        savePromises.push(
          busiPerSaveApi({
            id: state.value.selectedRoleId,
            type: 1, // 1 表示角色
            flag,
            permissions
          })
        )
      })

      // 串行保存避免数据库死锁
      for (const promise of savePromises) {
        await promise
      }

      // 清空变更记录
      state.value.resourceChanges = { grants: [], revokes: [] }
      state.value.cacheVersion++

      ElMessage.success('资源权限保存成功')
    } catch (error) {
      console.error('保存资源权限变更失败:', error)
      ElMessage.error('保存资源权限失败')
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
    _options: {
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
  const loadAuditLogs = async (_params?: {
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

  const savePermissionChanges = async (_roleId: string) => {
    await saveMenuPermissionChanges()
  }

  const resetChanges = () => {
    resetPermissionChanges()
  }

  const hasPermission = (id: string): boolean => {
    const findNode = (nodes: MenuNode[]): boolean | undefined => {
      for (const node of nodes) {
        if (node.id === id) {
          console.log(
            '[DEBUG] hasPermission - found node:',
            id,
            'hasPermission:',
            node.hasPermission
          )
          return node.hasPermission
        }
        if (node.children?.length) {
          const found = findNode(node.children)
          if (found !== undefined) return found
        }
      }
      return undefined
    }
    const result = findNode(state.value.menuTreeData)
    console.log('[DEBUG] hasPermission for', id, ':', result)
    return result ?? false
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

  // 获取资源树已选中的key（用于资源授权页面）
  const getResourceCheckedKeys = (): string[] => {
    const keys: string[] = []
    // 遍历所有资源类型
    Object.values(state.value.resourceTreeData).forEach(resources => {
      const collectKeys = (nodes: ResourceNode[]) => {
        nodes.forEach(node => {
          if (node.hasPermission) {
            keys.push(node.id)
          }
          if (node.children?.length) {
            collectKeys(node.children)
          }
        })
      }
      collectKeys(resources)
    })
    return keys
  }

  const getRelatedResources = (_menuId: string): ResourceNode[] => {
    // TODO: 实现根据菜单ID查找关联资源的逻辑
    return []
  }

  // 检查资源是否有权限
  const hasResourcePermission = (resourceId: string): boolean => {
    // 遍历所有资源类型查找该资源
    for (const resourceType in state.value.resourceTreeData) {
      const resources = state.value.resourceTreeData[resourceType]
      const findNode = (nodes: ResourceNode[]): boolean => {
        for (const node of nodes) {
          if (node.id === resourceId) {
            return node.hasPermission
          }
          if (node.children?.length) {
            const found = findNode(node.children)
            if (found) return true
          }
        }
        return false
      }
      if (findNode(resources)) return true
    }
    return false
  }

  // 获取资源权限列表（用于资源授权页面）
  const getResourcePermissions = (resourceId: string): string[] => {
    // 遍历所有资源类型查找该资源
    for (const resourceType in state.value.resourceTreeData) {
      const resources = state.value.resourceTreeData[resourceType]
      const findNode = (nodes: ResourceNode[]): string[] | null => {
        for (const node of nodes) {
          if (node.id === resourceId) {
            return node.permissions || []
          }
          if (node.children?.length) {
            const found = findNode(node.children)
            if (found !== null) return found
          }
        }
        return null
      }
      const result = findNode(resources)
      if (result !== null) return result
    }
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
    loadResourceTree,
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
    getResourceCheckedKeys,
    getRelatedResources,
    getNodePermissions,
    updateNodePermissions,
    hasResourcePermission,
    getResourcePermissions
  }
})
