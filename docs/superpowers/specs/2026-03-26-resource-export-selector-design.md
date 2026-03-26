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

## 交互流程

```
用户选择类型 → 点击导出按钮
        ↓
类型是 combined？
   ↓是              ↓否
直接导出       打开 ResourceSelectDialog
                      ↓
               用户选择资源 → 确认
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
│            [取消]  [确认导出]         │  ← 操作按钮
└─────────────────────────────────────┘
```

**功能:**
1. 搜索过滤资源
2. 勾选目录时自动展开并全选子资源
3. 底部显示已选数量
4. 确认时返回选中的 ID 列表

### ExportImportDialog.vue 修改

1. **移除现有的内联 `el-tree-select`**
   - 删除数据源/数据集的内联选择器

2. **修改导出按钮逻辑**
   ```typescript
   const handleExport = async () => {
     if (exportForm.type === 'combined') {
       // 直接导出全部
       doExport([])
     } else {
       // 打开资源选择弹窗
       resourceSelectDialog.value.open(exportForm.type)
     }
   }
   ```

3. **处理资源选择回调**
   ```typescript
   const onResourceSelected = async (selectedIds: string[]) => {
     // 检测依赖
     if (['dataset', 'dashboard', 'dataview'].includes(exportForm.type)) {
       const deps = await checkDependencies(exportForm.type, selectedIds)
       if (deps.hasDependencies) {
         // 显示依赖确认对话框
         showDependencyDialog(deps, selectedIds)
       } else {
         doExport(selectedIds)
       }
     } else {
       doExport(selectedIds)
     }
   }
   ```

## 依赖检测设计

### 依赖关系

- 数据集 → 依赖数据源
- 仪表板/数据大屏 → 依赖数据集、数据源

### 依赖确认对话框

```
┌─────────────────────────────────────┐
│  ⚠️ 检测到依赖资源                    │
├─────────────────────────────────────┤
│  您选择的数据集依赖以下数据源：         │
│                                     │
│  ☑ MySQL生产库                       │  ← 默认全选
│  ☑ PostgreSQL测试库                  │
│                                     │
│  是否一并导出？                       │
├─────────────────────────────────────┤
│        [取消]  [仅导出所选]  [全部导出] │
└─────────────────────────────────────┘
```

**按钮含义:**
- **取消** → 关闭对话框，回到选择界面
- **仅导出所选** → 只导出用户选中的资源，不包含依赖项
- **全部导出** → 导出用户选中的资源 + 依赖的资源

## 后端 API

### 新增接口

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

### 现有接口复用

- `exportData` - 已支持 `resourceIds`，无需修改
- 各资源树的加载接口 - 已存在，直接复用

## 实现步骤

1. **前端 - 创建 ResourceSelectDialog 组件**
   - 实现资源选择弹窗 UI
   - 实现搜索、树形选择功能
   - 实现目录展开全选逻辑

2. **前端 - 修改 ExportImportDialog**
   - 移除内联选择器
   - 集成 ResourceSelectDialog
   - 实现依赖检测调用

3. **后端 - 新增依赖检测接口**
   - 实现 `/backupCenter/checkDependencies` 接口

4. **前端 - 实现依赖确认对话框**
   - 显示依赖资源列表
   - 处理用户选择

5. **测试**
   - 各类型资源导出测试
   - 依赖检测测试
   - 边界情况测试
