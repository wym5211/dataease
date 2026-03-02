# 角色API认证问题修复

## 问题描述
菜单权限管理页面加载角色列表失败，提示"加载角色列表失败"

## 根本原因
**未登录或token已过期**

后端日志显示：
```
io.dataease.exception.DEException: token is empty for uri {/role/byCurOrg}
```

## 数据流分析

### Token传递链路
1. **前端缓存**：`wsCache.get('user.token')`
2. **请求拦截器**：`refresh.ts:56` 添加 `X-DE-TOKEN` header
3. **后端验证**：`TokenFilter.java:83` 验证token是否存在

### 当前状态
- ❌ 前端缓存中无token：`wsCache.get('user.token')` → null
- ❌ 请求未携带token：`X-DE-TOKEN` header缺失
- ❌ 后端拒绝访问：返回500错误

## 解决方案

### 方案一：先登录（推荐）
1. 访问登录页面
2. 使用默认账号登录：
   - 用户名：`admin`
   - 密码：`DataEase@123456`
3. 登录成功后访问菜单权限管理页面

### 方案二：错误处理优化
已添加到代码中：
- ✅ 检测401/token相关错误，提示用户登录
- ✅ 降级方案：API失败时使用store中的模拟数据
- ✅ 友好的错误提示信息

## 代码变更

### 文件
`core/core-frontend/src/views/permissions/menu/index.vue`

### 变更内容
```typescript
// 添加错误类型判断
if (error?.response?.status === 401 || error?.msg?.includes('token')) {
  ElMessage.warning('请先登录系统')
} else if (error?.msg) {
  ElMessage.error(`加载角色列表失败: ${error.msg}`)
} else {
  ElMessage.error('加载角色列表失败，请稍后重试')
}

// 降级方案：使用store中的模拟数据
if (permissionStore.roleList && permissionStore.roleList.length > 0) {
  roleOptions.value = permissionStore.roleList
  console.log('使用降级数据（模拟角色）:', roleOptions.value)
}
```

## 测试步骤
1. 清除浏览器缓存和localStorage
2. 访问登录页面
3. 登录系统
4. 访问菜单权限管理页面
5. 验证角色下拉框是否显示数据库中的角色

## 技术细节

### Token存储位置
- **Key**: `user.token`
- **Storage**: wsCache (基于localStorage)
- **Set位置**: 登录成功后 (`user.store.ts`)

### API认证流程
```
前端请求
  ↓
refresh.ts: configHandler()
  ↓
检查 wsCache.get('user.token')
  ↓
添加 header: X-DE-TOKEN
  ↓
后端 TokenFilter
  ↓
验证token → 通过/拒绝
```

## 相关文件
- `core/core-frontend/src/config/axios/refresh.ts` - Token处理
- `core/core-frontend/src/store/modules/user.ts` - 用户状态管理
- `core/core-backend/src/main/java/io/dataease/auth/filter/TokenFilter.java` - Token验证
