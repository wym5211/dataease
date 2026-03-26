# 备份导入：同名目录跳过 + 覆盖选项设计文档

## 需求

在导入目录时，检查当前系统是否已有同名的目录：
1. 如存在同名目录 → 不创建新目录，直接复用已有目录，继续导入其内容
2. 子内容（子文件夹、图表）导入时，查找/创建到系统已有目录下
3. 叶子节点（图表）如有重名：
   - 用户选择"覆盖已有内容" → 覆盖已有
   - 用户未选择"覆盖已有内容" → 重命名（如 "图表_1"）

## 背景

当前导入逻辑在遇到同名目录时会自动重命名为 `name_1`, `name_2` 等。这种行为在用户希望合并备份与现有目录时不够灵活——用户可能希望直接复用已有目录，而不是创建一堆带数字后缀的重复目录。

## 设计方案

### 方案 A：最小改动 + 统一覆盖选项（推荐）

**核心改动：**

1. **新增 `overwrite` 选项**
   - 在 `BackupCenterManage.importData()` 增加 `overwrite` 参数
   - 透传给各服务（BackupDatasourceServiceImpl、BackupDatasetServiceImpl、BackupDashboardServiceImpl）

2. **目录创建逻辑确认**
   - 各服务的 `createOrFindXxxFolder()` 方法**当前已实现**"存在则复用"的逻辑
   - 本次改动是**确认和完善**：确保在 `overwrite` 选项下，目录复用行为一致

3. **叶子节点导入逻辑**
   - `overwrite=true` → 覆盖已有
   - `overwrite=false` → 重命名（调用现有的 `generateUniqueName()` 方法）

## 数据流

### 目录导入流程

```
导入请求（含 overwrite 选项）
  ↓
BackupCenterManage.importData(overwrite)
  ↓
1. 按 level ASC 排序处理文件夹
   ↓
2. 对于每个 BackupFolder：
   - key = name + "_" + parentName
   - 检查 folderMapping 是否已有 key
   - 如已有 → 跳过（复用已有目录）
   - 如没有 → 查找系统是否存在同名目录
       - 存在 → 加入 folderMapping，复用已有 ID
       - 不存在 → 创建新目录，加入 folderMapping
  ↓
3. 导入叶子节点（图表等）：
   - overwrite=true → 覆盖已有
   - overwrite=false → generateUniqueName() 重命名
```

### 目录匹配方式

通过 `name + parentName` 唯一标识目录：

```
备份目录: "子目录" (父目录名为 "父目录")
系统查找: 查找名为 "子目录" 且父目录名为 "父目录" 的目录
```

## 具体改动点

### 1. BackupCenterManage.java

**文件**: `core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java`

**改动**: `importData()` 方法增加 `overwrite` 参数

```java
public void importData(MultipartFile file, String type, boolean overwrite) throws Exception {
    // ...
    // 透传给各服务
    if ("dataset".equals(type)) {
        backupDatasetService.importDatasets(backupData, folderMapping, overwrite);
    } else if ("dashboard".equals(type)) {
        backupDashboardService.importDashboards(backupData, folderMapping, overwrite);
    } else if ("datasource".equals(type)) {
        backupDatasourceService.importDatasources(backupData, folderMapping, overwrite);
    }
    // ...
}
```

### 2. 各服务接口增加 overwrite 参数

**BackupDatasourceService**
```java
void importDatasources(BackupData backupData, Map<String, Long> folderMapping, boolean overwrite);
```

**BackupDatasetService**
```java
void importDatasets(BackupData backupData, Map<String, Long> folderMapping, boolean overwrite);
```

**BackupDashboardService**
```java
void importDashboards(BackupData backupData, Map<String, Long> folderMapping, boolean overwrite);
```

### 3. 各服务实现改动

#### 3.1 createOrFind 方法（目录查找/创建）

**BackupDatasourceServiceImpl**

```java
public Long createOrFindDatasourceFolder(BackupFolder folder, Map<String, Long> folderMapping) {
    String key = buildKey(folder); // name + "_" + parentName
    if (folderMapping.containsKey(key)) {
        return folderMapping.get(key); // 已处理过，直接返回
    }

    // 查找父目录 ID
    Long parentId = findParentId(folder);

    // 查找系统是否已有同名目录
    CoreDatasource existing = findDatasourceFolderByNameAndParent(folder.getName(), parentId);
    if (existing != null) {
        folderMapping.put(key, existing.getId());
        return existing.getId(); // 复用已有目录，不创建新的
    }

    // 创建新目录...
    Long newId = createDatasourceFolder(folder, parentId);
    folderMapping.put(key, newId);
    return newId;
}
```

**BackupDatasetServiceImpl** 和 **BackupDashboardServiceImpl** 同理修改。

#### 3.2 叶子节点导入（图表/数据集等）

以 BackupDashboardServiceImpl 为例：

```java
private void importChart(BackupChartView backupChart, Long folderId,
                         Map<String, Long> folderMapping, boolean overwrite) {
    // 查找是否已有同名图表
    DataVisualizationInfo existing = findChartByNameAndFolder(
        backupChart.getName(), folderId);

    if (existing != null) {
        if (overwrite) {
            // 覆盖已有图表
            updateChart(existing.getId(), backupChart);
        } else {
            // 重命名后创建
            String newName = generateUniqueName(backupChart.getName());
            createChart(backupChart, folderId, newName);
        }
    } else {
        // 创建新图表
        createChart(backupChart, folderId, backupChart.getName());
    }
}
```

### 4. importCharts 方法增加 overwrite 支持

**BackupDashboardServiceImpl**

当前 `importCharts` 方法缺少 overwrite 参数，需要新增重载方法或修改现有方法：

```java
// 新增重载方法
void importCharts(List<BackupChartView> charts,
                 Map<String, Long> dashboardIdMapping,
                 Map<String, Long> datasetIdMapping,
                 Map<String, Long> chartIdMapping,
                 boolean overwrite);
```

内部逻辑调整：
```java
private void importChart(BackupChartView backupChart, Long folderId,
                         Map<String, Long> chartIdMapping, boolean overwrite) {
    // 查找是否已有同名图表
    DataVisualizationInfo existing = findChartByNameAndFolder(
        backupChart.getName(), folderId);

    if (existing != null) {
        if (overwrite) {
            // 覆盖已有图表
            updateChart(existing.getId(), backupChart);
            chartIdMapping.put(backupChart.getId(), existing.getId());
        } else {
            // 重命名后创建
            String newName = generateUniqueName(backupChart.getName());
            Long newId = createChart(backupChart, folderId, newName);
            chartIdMapping.put(backupChart.getId(), newId);
        }
    } else {
        // 创建新图表
        Long newId = createChart(backupChart, folderId, backupChart.getName());
        chartIdMapping.put(backupChart.getId(), newId);
    }
}
```

### 5. 前端覆盖选项

需确认前端文件路径，查找备份导入相关 Vue 组件：
- 搜索包含"backup"和"import"的 Vue 文件
- 添加"覆盖已有内容"复选框

## 测试场景

### 目录导入测试

1. **同名目录存在，内容不冲突**
   - 系统有 "我的文件夹"，备份有 "我的文件夹/图表A"
   - 导入后：图表A 在系统的 "我的文件夹" 下

2. **同名目录存在，子目录也存在**
   - 系统有 "我的文件夹/子文件夹"，备份有 "我的文件夹/子文件夹/图表A"
   - 导入后：图表A 在系统的 "我的文件夹/子文件夹" 下

3. **同名目录存在，子目录不存在**
   - 系统有 "我的文件夹"，备份有 "我的文件夹/新子文件夹/图表A"
   - 导入后：创建 "新子文件夹"，图表A 在其下

4. **同名目录不存在**
   - 导入行为与现有逻辑一致，创建新目录

### 覆盖选项测试

5. **覆盖选项 OFF，叶子节点重名**
   - 系统有 "文件夹/图表A"，备份有 "文件夹/图表A"
   - 导入后：新增 "图表A_1"

6. **覆盖选项 ON，叶子节点重名**
   - 系统有 "文件夹/图表A"，备份有 "文件夹/图表A"
   - 导入后：图表A 被备份内容覆盖

7. **跨目录同名文件夹**
   - 系统有 "文件夹A/子目录"，备份有 "文件夹B/子目录"
   - 导入后：两个"子目录"都存在，分别在各自的父目录下（不冲突）

## 实现文件清单

### 修改

| 文件 | 改动 |
|------|------|
| `BackupCenterManage.java` | importData() 增加 overwrite 参数，透传给 importCharts |
| `BackupDatasourceService.java` | 接口增加 overwrite |
| `BackupDatasourceServiceImpl.java` | 确认 createOrFind 逻辑，添加叶子节点覆盖逻辑 |
| `BackupDatasetService.java` | 接口增加 overwrite |
| `BackupDatasetServiceImpl.java` | 确认 createOrFind 逻辑，添加叶子节点覆盖逻辑 |
| `BackupDashboardService.java` | 接口增加 overwrite |
| `BackupDashboardServiceImpl.java` | 确认 createOrFind 逻辑，添加 importCharts overwrite 重载方法 |
| `BackupImport.vue` (前端) | 添加覆盖选项复选框（需确认文件路径） |
