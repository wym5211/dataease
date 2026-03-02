# 修复菜单权限管理角色下拉框

## 问题描述
菜单权限管理窗口的角色下拉框数据不对，没有从数据库里取值

## 调查过程

### Phase 1: 根本原因调查

**症状**：角色下拉框显示硬编码的3个角色

**根本原因**：
1. **模板层**（index.vue:6-10）：使用硬编码 `<el-option>`
2. **Store层**（permission.ts:183-199）：`loadRoles()` 被注释，使用模拟数据
3. **API层**：前端定义了不存在的 `/role/options` 端点

**数据流追踪**：
- ❌ 模板：硬编码选项
- ❌ Store：未调用真实API
- ✅ 后端：存在 `/role/byCurOrg` 端点

### Phase 2: 模式分析

**工作示例**（user/index.vue）：
- 导入 `roleList` API
- 在 `onMounted` 中调用 API 加载数据
- 模板使用 `v-for` 遍历角色列表

### Phase 3: 假设与测试

**假设**：修复需要使用 `roleList` API 替代不存在的 `getRoleOptions()`

### Phase 4: 实施与验证

**修复内容**：
1. 导入 `roleList` API
2. 添加本地 `roleOptions` ref
3. 修改模板使用 `v-for` 遍历
4. 添加数据类型适配（Long → string）
5. 在 `onMounted` 中加载角色数据

**修改文件**：
- `core/core-frontend/src/views/permissions/menu/index.vue`

**代码变更**：
```vue
<!-- 修改前：硬编码 -->
<el-option label="超级管理员" value="role_001" />
<el-option label="普通用户" value="role_002" />
<el-option label="数据分析师" value="role_003" />

<!-- 修改后：从数据库加载 -->
<el-option
  v-for="role in roleList"
  :key="role.id"
  :label="role.name"
  :value="role.id"
/>
```

```typescript
// 添加数据加载
const roleOptions = ref<RoleItem[]>([])

onMounted(async () => {
  const res = await roleList({ page: 1, pageSize: 1000 })
  roleOptions.value = res.records.map(role => ({
    id: String(role.id),  // 类型适配
    name: role.name,
    code: role.code,
    description: role.description,
    createTime: role.createTime
  }))
})
```

## 测试验证
- [ ] 刷新前端页面
- [ ] 打开菜单权限管理页面
- [ ] 检查角色下拉框是否显示数据库中的角色
- [ ] 选择角色，验证菜单权限树是否正常加载

## 技术要点

### API 端点对比
- ❌ `/role/options` (GET) - 不存在
- ✅ `/role/byCurOrg` (POST) - 实际可用

### 数据类型适配
后端返回 `id: Long`，前端期望 `id: string`，需要转换：
```typescript
id: String(role.id)
```

### 修复原则
- **最小化修改**：只修改菜单权限管理页面
- **不修改Store**：避免影响其他组件
- **复用现有API**：使用 `roleList` 而非创建新端点
