# 资源导出选择器设计文档

## 概述

在资源导出功能中，除了"组合导出"选项外，其他导出类型需要弹出资源选择窗口，让用户选择具体导出哪些资源（包括目录）。

## 需求总结

| 项目 | 决策 |
|------|------|
| "全部"选项 | 指现有的"组合导出"类型，选择后直接导出所有资源 |
| 触发方式 | 点击"导出"按钮时弹出选择窗口 |
| 选择方式 | 复选框树（el-tree + show-checkbox） |
| 选中目录行为 | 自动展开 + 自动全选子资源，用户可手动调整 |
| 弹窗尺寸 | 中等弹窗（600-800px） |
| 已选显示 | 底部显示"已选择 X 个资源" |
| 依赖处理 | 分开选择，系统检测依赖后提示用户 |

## 组件结构

```
core/core-frontend/src/components/backup/
├── ExportImportDialog.vue      # 现有组件（修改）
├── ResourceSelectDialog.vue    # 新增：资源选择弹窗
└── types.ts                    # 新增：类型定义（可选）
```

## 数据流图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        ExportImportDialog                            │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 1. 用户选择导出类型（datasource/dataset/dashboard/dataview）   │   │
│  │ 2. 用户点击"导出"按钮                                         │   │
│  │ 3. combined → 直接调用 doExport([])                           │   │
│  │    其他类型 → 调用 resourceSelectDialog.open(type)            │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              ↓                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              ResourceSelectDialog                             │   │
│  │  4. 加载对应类型的资源树                                       │   │
│  │  5. 用户选择资源 → 点击"确认导出"                              │   │
│  │  6. emit('confirm', selectedIds)                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              ↓                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  7. onResourceSelected(selectedIds)                          │   │
│  │     - datasource → 直接 doExport(selectedIds)                │   │
│  │     - dataset/dashboard/dataview → 检测依赖                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              ↓                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              DependencyConfirmDialog（内嵌）                   │   │
│  │  8. 显示依赖资源列表                                           │   │
│  │  9. 用户选择：仅导出所选 / 全部导出 / 取消                      │   │
│  │ 10. 调用 doExport(finalIds)                                  │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

## 交互流程

```
用户选择类型 → 点击导出按钮
        ↓
类型是 combined？
   ↓是              ↓否
直接导出       打开 ResourceSelectDialog
                      ↓
               用户选择资源 → 点击确认
                      ↓
               selectedIds.length === 0？
                ↓是              ↓否
              提示选择        检测依赖（非 datasource）
                              ↓
                         有依赖项？
                          ↓是           ↓否
                    显示依赖确认      直接导出
                    对话框
                          ↓
                    用户选择后导出
```

## 组件设计

### ResourceSelectDialog.vue

**Props:**
```typescript
interface Props {
  resourceType: 'datasource' | 'dataset' | 'dashboard' | 'dataview'
}
```

**Emit:**
```typescript
emit('confirm', selectedIds: string[])
```

**布局:**
```
┌─────────────────────────────────────┐
│  选择要导出的数据源                    │  ← 标题（动态）
├─────────────────────────────────────┤
│  🔍 搜索资源...                       │  ← 搜索框
├─────────────────────────────────────┤
│  ☑ 📁 目录A                          │
│    ├─ ☑ 资源1                        │  ← 资源树（带复选框）
│    └─ ☐ 资源2                        │
│  ☐ 📁 目录B                          │
│    └─ ☐ 资源3                        │
│                                     │
├─────────────────────────────────────┤
│  已选择 2 个资源                      │  ← 底部状态
├─────────────────────────────────────┤
│            [取消]  [确认导出]         │  ← 确认按钮：未选择时 disabled
└─────────────────────────────────────┘
```

**功能:**
1. 搜索过滤资源
2. 勾选目录时自动展开并全选子资源
3. 底部显示已选数量
4. 未选择任何资源时，确认按钮禁用
5. 确认时返回选中的 ID 列表

**目录勾选状态处理:**
- 使用 `el-tree` 的 `check-strictly=false` 模式
- 当用户取消某个子资源的勾选时，父目录的勾选状态自动变为半选（indeterminate）
- 获取选中 ID 时，只返回叶子节点的 ID（通过 `tree.getCheckedNodes(true)` 获取）

### ExportImportDialog.vue 修改

1. **移除现有的内联 `el-tree-select`**
   - 删除数据源/数据集的内联选择器（第 29-51 行）
   - 删除 `datasourceTree` 和 `datasetTree` 相关代码

2. **新增组件引用**
   ```typescript
   const resourceSelectDialog = ref()
   const dependencyDialogVisible = ref(false)
   const pendingSelectedIds = ref<string[]>([])
   const dependencyInfo = ref<DependencyInfo | null>(null)
   ```

3. **修改导出按钮逻辑**
   ```typescript
   const handleExport = async () => {
     if (exportForm.type === 'combined') {
       doExport([])
     } else {
       resourceSelectDialog.value.open(exportForm.type)
     }
   }
   ```

4. **处理资源选择回调**
   ```typescript
   const onResourceSelected = async (selectedIds: string[]) => {
     if (exportForm.type === 'datasource') {
       doExport(selectedIds)
     } else {
       // 检测依赖
       const deps = await checkDependencies(exportForm.type, selectedIds)
       if (deps.hasDependencies) {
         pendingSelectedIds.value = selectedIds
         dependencyInfo.value = deps
         dependencyDialogVisible.value = true
       } else {
         doExport(selectedIds)
       }
     }
   }
   ```

5. **依赖确认回调**
   ```typescript
   const onDependencyConfirm = (includeDeps: boolean) => {
     dependencyDialogVisible.value = false
     if (includeDeps) {
       // 合并所选资源和依赖资源
       const allIds = [
         ...pendingSelectedIds.value,
         ...dependencyInfo.value.dependencies.datasources.map(d => d.id),
         ...dependencyInfo.value.dependencies.datasets.map(d => d.id)
       ]
       doExport(allIds)
     } else {
       doExport(pendingSelectedIds.value)
     }
   }
   ```

### 依赖确认对话框（内嵌在 ExportImportDialog.vue）

使用 `el-dialog` 直接内嵌在 ExportImportDialog 中，而非独立组件：

```vue
<el-dialog
  v-model="dependencyDialogVisible"
  title="检测到依赖资源"
  width="500px"
>
  <p>您选择的{{ typeName }}依赖以下资源，是否一并导出？</p>
  <el-checkbox-group v-model="selectedDependencyIds">
    <div v-for="ds in dependencyInfo?.dependencies.datasources" :key="ds.id">
      <el-checkbox :label="ds.id">{{ ds.name }} (数据源)</el-checkbox>
    </div>
    <div v-for="dt in dependencyInfo?.dependencies.datasets" :key="dt.id">
      <el-checkbox :label="dt.id">{{ dt.name }} (数据集)</el-checkbox>
    </div>
  </el-checkbox-group>
  <template #footer>
    <el-button @click="dependencyDialogVisible = false">取消</el-button>
    <el-button @click="onDependencyConfirm(false)">仅导出所选</el-button>
    <el-button type="primary" @click="onDependencyConfirm(true)">全部导出</el-button>
  </template>
</el-dialog>
```

## 资源树 API

### 前端 API 封装

需要在 `api/backup.ts` 或对应文件中添加：

```typescript
// 获取数据源树（已有，复用 listDatasources）
import { listDatasources } from '@/api/datasource'

// 获取数据集树
export const getDatasetTree = (): Promise<IResponse> => {
  return request.post({ url: '/dataset/tree' })
}

// 获取仪表板树
export const getDashboardTree = (): Promise<IResponse> => {
  return request.post({ url: '/dataVisualization/tree', data: { busiFlag: 'dashboard' } })
}

// 获取数据大屏树
export const getDataviewTree = (): Promise<IResponse> => {
  return request.post({ url: '/dataVisualization/tree', data: { busiFlag: 'dataV' } })
}
```

### 后端接口映射

| 资源类型 | 接口 | 说明 |
|----------|------|------|
| datasource | `/datasource/list` | 已有，返回数据源列表（无树结构，平铺） |
| dataset | `/dataset/tree` | 已有，返回数据集树 |
| dashboard | `/dataVisualization/tree` + `busiFlag=dashboard` | 已有 |
| dataview | `/dataVisualization/tree` + `busiFlag=dataV` | 已有 |

**注意：** 数据源没有树结构（目录），是平铺列表。在 ResourceSelectDialog 中需要适配处理。

## 依赖检测设计

### 依赖关系

- 数据源 → 无依赖
- 数据集 → 依赖数据源
- 仪表板/数据大屏 → 依赖数据集、数据源

### 后端 API

**检测资源依赖:**

```typescript
// POST /backupCenter/checkDependencies
interface CheckDependenciesRequest {
  type: 'dataset' | 'dashboard' | 'dataview'
  resourceIds: string[]
}

interface CheckDependenciesResponse {
  hasDependencies: boolean
  dependencies: {
    datasources: { id: string, name: string }[]
    datasets: { id: string, name: string }[]  // 仅 dashboard/dataview 时返回
  }
}
```

**后端实现逻辑:**
1. dataset 类型：查询 `core_dataset_table` 表的 `datasource_id` 字段
2. dashboard/dataview 类型：查询视图关联的数据集，再递归查询数据集关联的数据源
3. 使用 SQL IN 查询批量检测，避免 N+1 问题

## 后端 API 总结

### 新增接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/backupCenter/checkDependencies` | POST | 检测资源依赖 |

### 复用接口

| 接口 | 说明 |
|------|------|
| `/backupCenter/export` | 已支持 resourceIds 参数 |
| `/datasource/list` | 获取数据源列表 |
| `/dataset/tree` | 获取数据集树 |
| `/dataVisualization/tree` | 获取仪表板/大屏树 |

## 国际化

需要在 `locales/zh-CN.ts`、`locales/en.ts`、`locales/tw.ts` 中添加：

```typescript
// zh-CN.ts
backup: {
  // ...existing
  select_resource_title: '选择要导出的{type}',
  selected_count: '已选择 {count} 个资源',
  no_selection: '请至少选择一个资源',
  dependency_detected: '检测到依赖资源',
  dependency_desc: '您选择的{type}依赖以下资源，是否一并导出？',
  export_selected_only: '仅导出所选',
  export_all: '全部导出',
  datasource_type: '数据源',
  dataset_type: '数据集',
  dashboard_type: '仪表板',
  dataview_type: '数据大屏',
}
```

## 实现步骤

### 第一阶段：前端基础组件

1. **添加国际化文案**
   - 在三个语言文件中添加新文案

2. **创建 ResourceSelectDialog 组件**
   - 实现资源选择弹窗 UI
   - 实现搜索、树形选择功能
   - 实现目录展开全选逻辑
   - 处理数据源平铺列表的特殊情况

3. **修改 ExportImportDialog**
   - 移除内联选择器
   - 集成 ResourceSelectDialog
   - 添加空选择校验

### 第二阶段：依赖检测

4. **后端 - 新增依赖检测接口**
   - 实现 `/backupCenter/checkDependencies` 接口
   - 处理批量查询优化

5. **前端 - 实现依赖确认对话框**
   - 显示依赖资源列表
   - 处理用户选择

### 第三阶段：测试

6. **测试**
   - 各类型资源导出测试
   - 依赖检测测试
   - 边界情况测试（空选择、大量资源）
   - 国际化测试

## 边界情况处理

| 场景 | 处理方式 |
|------|----------|
| 未选择任何资源 | 确认按钮 disabled，点击无响应 |
| 资源树为空 | 显示"暂无可导出的资源"提示 |
| 依赖检测失败 | 显示错误提示，允许用户选择"仅导出所选"继续 |
| 大量资源（>500） | 启用 el-tree 的虚拟滚动（如有性能问题） |
