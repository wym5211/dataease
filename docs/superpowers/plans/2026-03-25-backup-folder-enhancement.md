# 备份目录增强实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 支持导入导出目录结构，备份整个目录树和资源的关系

**Architecture:** 在现有备份模型基础上新增目录备份模型，修改导出/导入流程以收集和恢复目录结构。通过 name + parentName 匹配目录，确保跨环境兼容性。

**Tech Stack:** Java, Spring Boot, MyBatis-Plus

---

## 文件结构

### 新增
- `sdk/common/src/main/java/io/dataease/model/backup/BackupFolder.java`

### 修改
- `sdk/common/src/main/java/io/dataease/model/backup/ExportPackage.java` - 增加 `folders` 字段
- `sdk/common/src/main/java/io/dataease/model/backup/BackupDataset.java` - 增加 `folderPath` 字段
- `sdk/common/src/main/java/io/dataease/model/backup/BackupDatasource.java` - 增加 `folderPath` 字段
- `sdk/common/src/main/java/io/dataease/model/backup/BackupDashboard.java` - 增加 `folderPath` 字段
- `core/core-backend/.../backup/service/BackupDatasetService.java` - 增加导出目录方法签名
- `core/core-backend/.../backup/service/BackupDatasourceService.java` - 增加导出目录方法签名
- `core/core-backend/.../backup/service/BackupDashboardService.java` - 增加导出目录方法签名
- `core/core-backend/.../backup/service/impl/BackupDatasetServiceImpl.java` - 实现目录导出/导入
- `core/core-backend/.../backup/service/impl/BackupDatasourceServiceImpl.java` - 实现目录导出/导入
- `core/core-backend/.../backup/service/impl/BackupDashboardServiceImpl.java` - 实现目录导出/导入
- `core/core-backend/.../backup/manage/BackupCenterManage.java` - 调用目录导出/导入

---

## Task 1: 创建 BackupFolder 模型类

**Files:**
- Create: `sdk/common/src/main/java/io/dataease/model/backup/BackupFolder.java`

- [ ] **Step 1: 创建 BackupFolder.java**

```java
package io.dataease.model.backup;

import lombok.Data;

/**
 * 目录备份信息
 */
@Data
public class BackupFolder {
    /**
     * 原ID
     */
    private String id;

    /**
     * 目录名称
     */
    private String name;

    /**
     * 父目录ID
     */
    private Long pid;

    /**
     * 层级深度
     */
    private Integer level;

    /**
     * 节点类型: folder
     */
    private String nodeType;

    /**
     * 资源类型: dataset | dashboard | datasource
     */
    private String resourceType;

    /**
     * 子类型: dashboard/dataV (仅 dashboard 类型需要)
     */
    private String subType;

    /**
     * 父目录名称（用于跨环境匹配）
     */
    private String parentName;
}
```

- [ ] **Step 2: Commit**

```bash
git add sdk/common/src/main/java/io/dataease/model/backup/BackupFolder.java
git commit -m "feat(backup): add BackupFolder model for directory structure"
```

---

## Task 2: 修改 ExportPackage 增加 folders 字段

**Files:**
- Modify: `sdk/common/src/main/java/io/dataease/model/backup/ExportPackage.java`

- [ ] **Step 1: 添加 folders 字段到 ExportPackage**

在 `ExportPackage.java` 的 `folders` 字段之后添加：

```java
/**
 * 目录列表
 */
private List<BackupFolder> folders;
```

- [ ] **Step 2: Commit**

```bash
git add sdk/common/src/main/java/io/dataease/model/backup/ExportPackage.java
git commit -m "feat(backup): add folders field to ExportPackage"
```

---

## Task 3: 为 Backup 模型增加 folderPath 字段

**Files:**
- Modify: `sdk/common/src/main/java/io/dataease/model/backup/BackupDataset.java`
- Modify: `sdk/common/src/main/java/io/dataease/model/backup/BackupDatasource.java`
- Modify: `sdk/common/src/main/java/io/dataease/model/backup/BackupDashboard.java`

- [ ] **Step 1: 为 BackupDataset 添加 folderPath 字段**

在 `BackupDataset.java` 中添加：

```java
/**
 * 目录路径（用于日志追踪）
 */
private String folderPath;
```

- [ ] **Step 2: 为 BackupDatasource 添加 folderPath 字段**

在 `BackupDatasource.java` 中添加：

```java
/**
 * 目录路径（用于日志追踪）
 */
private String folderPath;
```

- [ ] **Step 3: 为 BackupDashboard 添加 folderPath 字段**

在 `BackupDashboard.java` 中添加：

```java
/**
 * 目录路径（用于日志追踪）
 */
private String folderPath;
```

- [ ] **Step 4: Commit**

```bash
git add sdk/common/src/main/java/io/dataease/model/backup/BackupDataset.java
git add sdk/common/src/main/java/io/dataease/model/backup/BackupDatasource.java
git add sdk/common/src/main/java/io/dataease/model/backup/BackupDashboard.java
git commit -m "feat(backup): add folderPath field to backup models"
```

---

## Task 4: 实现 BackupDatasetServiceImpl 目录导出

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDatasetServiceImpl.java`

- [ ] **Step 1: 添加辅助方法 - 递归收集目录链**

```java
/**
 * 递归收集数据集的目录链
 */
private List<BackupFolder> collectDatasetFolders(Long datasetId, Map<String, BackupFolder> folderMap) {
    List<BackupFolder> result = new ArrayList<>();
    CoreDatasetGroup group = coreDatasetGroupMapper.selectById(datasetId);
    if (group == null || group.getPid() == null || group.getPid() == 0L) {
        return result;
    }
    CoreDatasetGroup parent = coreDatasetGroupMapper.selectById(group.getPid());
    if (parent == null) {
        return result;
    }
    if ("folder".equals(parent.getNodeType())) {
        BackupFolder folder = new BackupFolder();
        folder.setId(String.valueOf(parent.getId()));
        folder.setName(parent.getName());
        folder.setPid(parent.getPid());
        folder.setLevel(parent.getLevel());
        folder.setNodeType(parent.getNodeType());
        folder.setResourceType("dataset");
        folderMap.put(parent.getName(), folder);
        result.add(folder);
        result.addAll(collectDatasetFolders(parent.getId(), folderMap));
    }
    return result;
}
```

- [ ] **Step 2: 修改 exportDatasets 方法收集目录**

在 `exportDatasets()` 方法中，导出数据集时同时收集目录链：

```java
@Override
public List<BackupDataset> exportDatasets(List<String> datasetIds) {
    List<BackupDataset> result = new ArrayList<>();
    Map<String, BackupFolder> folderMap = new HashMap<>();
    try {
        List<CoreDatasetGroup> datasets;
        if (datasetIds != null && !datasetIds.isEmpty()) {
            datasets = coreDatasetGroupMapper.selectBatchIds(datasetIds.stream().map(Long::parseLong).collect(Collectors.toList()));
        } else {
            QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("node_type", "dataset");
            datasets = coreDatasetGroupMapper.selectList(queryWrapper);
        }

        for (CoreDatasetGroup ds : datasets) {
            // 收集目录链
            List<BackupFolder> folders = collectDatasetFolders(ds.getId(), folderMap);

            BackupDataset backup = new BackupDataset();
            backup.setId(String.valueOf(ds.getId()));
            backup.setName(ds.getName());
            // ... 其他字段设置

            // 设置 folderPath
            if (!folders.isEmpty()) {
                String folderPath = folders.stream().map(BackupFolder::getName).collect(Collectors.joining("/"));
                backup.setFolderPath(folderPath);
            }

            result.add(backup);
        }
        // 返回结果，包含目录信息
    } catch (Exception e) {
        LogUtil.getLogger().error("Export datasets failed", e);
    }
    return result;
}
```

注意：需要修改接口签名 `List<BackupDataset> exportDatasets()` → `List<BackupDataset> exportDatasets(List<String> datasetIds)`

- [ ] **Step 3: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDatasetServiceImpl.java
git commit -m "feat(backup): collect folder chain when exporting datasets"
```

---

## Task 5: 实现 BackupDatasetServiceImpl 目录导入

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDatasetServiceImpl.java`

- [ ] **Step 1: 添加辅助方法 - 按 name + parentName 查找目录**

```java
/**
 * 按 name + parentName 查找目录
 */
private CoreDatasetGroup findFolderByNameAndParent(String name, Long parentId) {
    QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", name).eq("node_type", "folder");
    if (parentId != null && parentId != 0L) {
        queryWrapper.eq("pid", parentId);
    } else {
        queryWrapper.and(w -> w.eq("pid", 0L).or().isNull("pid"));
    }
    return coreDatasetGroupMapper.selectOne(queryWrapper);
}

/**
 * 获取父目录名称
 */
private String getParentFolderName(Long parentId) {
    if (parentId == null || parentId == 0L) {
        return null;
    }
    CoreDatasetGroup parent = coreDatasetGroupMapper.selectById(parentId);
    return parent != null ? parent.getName() : null;
}
```

- [ ] **Step 2: 添加辅助方法 - 创建或查找目录**

```java
/**
 * 创建或查找目录，返回目录ID
 */
private Long createOrFindDatasetFolder(BackupFolder folder, Map<String, Long> folderMapping) {
    String key = folder.getName() + "_" + (folder.getParentName() != null ? folder.getParentName() : "root");
    if (folderMapping.containsKey(key)) {
        return folderMapping.get(key);
    }

    // 先查找父目录ID
    Long parentId = 0L;
    if (folder.getParentName() != null) {
        String parentKey = folder.getParentName() + "_" + (folder.getPid() != null ? String.valueOf(folder.getPid()) : "root");
        parentId = folderMapping.getOrDefault(parentKey, 0L);
    }

    // 查找是否已存在
    CoreDatasetGroup existing = findFolderByNameAndParent(folder.getName(), parentId);
    if (existing != null) {
        folderMapping.put(key, existing.getId());
        return existing.getId();
    }

    // 创建新目录
    CoreDatasetGroup newFolder = new CoreDatasetGroup();
    newFolder.setName(folder.getName());
    newFolder.setPid(parentId);
    newFolder.setLevel(folder.getLevel());
    newFolder.setNodeType("folder");
    newFolder.setCreateBy("1");
    newFolder.setCreateTime(System.currentTimeMillis());
    coreDatasetGroupMapper.insert(newFolder);

    // 重新查询获取ID
    newFolder = coreDatasetGroupMapper.selectOne(new QueryWrapper<CoreDatasetGroup>()
        .eq("name", folder.getName())
        .eq("node_type", "folder")
        .eq("pid", parentId));

    folderMapping.put(key, newFolder.getId());
    return newFolder.getId();
}
```

- [ ] **Step 3: 修改 importDatasetWithTables 方法支持目录**

在导入数据集之前，先导入目录链：

```java
@Override
public String importDatasetWithTables(BackupDataset dataset, boolean overwrite,
                                      Map<String, String> datasourceIdMapping) {
    Map<String, String> datasetIdMapping = new HashMap<>();
    return importDatasetWithTables(dataset, overwrite, datasourceIdMapping, datasetIdMapping);
}

private String importDatasetWithTables(BackupDataset dataset, boolean overwrite,
                                      Map<String, String> datasourceIdMapping,
                                      Map<String, String> datasetIdMapping,
                                      Map<String, Long> folderMapping) {
    // 1. 先导入目录链
    if (dataset.getFolders() != null && !dataset.getFolders().isEmpty()) {
        // 按 level 排序确保父目录先创建
        dataset.getFolders().sort(Comparator.comparingInt(BackupFolder::getLevel));
        for (BackupFolder folder : dataset.getFolders()) {
            createOrFindDatasetFolder(folder, folderMapping);
        }
    }

    // 2. 查找或创建数据集
    QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", dataset.getName()).eq("node_type", "dataset");
    CoreDatasetGroup existing = coreDatasetGroupMapper.selectOne(queryWrapper);

    String newDatasetId;
    if (existing != null && overwrite) {
        // ... 覆盖逻辑
    } else {
        String newName = dataset.getName();
        if (existing != null) {
            newName = generateUniqueName(dataset.getName());
        }
        CoreDatasetGroup newDs = new CoreDatasetGroup();
        newDs.setName(newName);
        newDs.setPid(0L); // 默认根目录
        // 如果有 folderPath，设置正确的 pid
        if (dataset.getFolderPath() != null && folderMapping.containsKey(dataset.getFolderPath())) {
            newDs.setPid(folderMapping.get(dataset.getFolderPath()));
        }
        // ... 其他字段
    }
    // ... 后续逻辑
}
```

- [ ] **Step 4: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDatasetServiceImpl.java
git commit -m "feat(backup): import folder structure when importing datasets"
```

---

## Task 6: 实现 BackupDatasourceServiceImpl 目录导出

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDatasourceServiceImpl.java`

- [ ] **Step 1: 添加辅助方法 - 收集数据源目录**

```java
/**
 * 递归收集数据源的目录链
 */
private List<BackupFolder> collectDatasourceFolders(Long datasourceId, Map<String, BackupFolder> folderMap) {
    List<BackupFolder> result = new ArrayList<>();
    CoreDatasource ds = coreDatasourceMapper.selectById(datasourceId);
    if (ds == null || ds.getPid() == null || ds.getPid() == 0L) {
        return result;
    }
    CoreDatasource parent = coreDatasourceMapper.selectById(ds.getPid());
    if (parent == null) {
        return result;
    }
    BackupFolder folder = new BackupFolder();
    folder.setId(String.valueOf(parent.getId()));
    folder.setName(parent.getName());
    folder.setPid(parent.getPid());
    folder.setLevel(0); // 数据源目录默认 level
    folder.setNodeType("folder");
    folder.setResourceType("datasource");
    folderMap.put(parent.getName(), folder);
    result.add(folder);
    result.addAll(collectDatasourceFolders(parent.getId(), folderMap));
    return result;
}
```

- [ ] **Step 2: 修改 exportDatasources 方法收集目录**

```java
@Override
public List<BackupDatasource> exportDatasources(List<String> datasourceIds) {
    List<BackupDatasource> result = new ArrayList<>();
    Map<String, BackupFolder> folderMap = new HashMap<>();
    try {
        List<CoreDatasource> datasources;
        if (datasourceIds != null && !datasourceIds.isEmpty()) {
            datasources = coreDatasourceMapper.selectBatchIds(datasourceIds.stream().map(Long::parseLong).collect(Collectors.toList()));
        } else {
            QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("pid", 0);
            datasources = coreDatasourceMapper.selectList(queryWrapper);
        }

        for (CoreDatasource ds : datasources) {
            // 收集目录链
            List<BackupFolder> folders = collectDatasourceFolders(ds.getId(), folderMap);

            BackupDatasource backup = new BackupDatasource();
            backup.setId(String.valueOf(ds.getId()));
            backup.setName(ds.getName());
            // ... 其他字段

            // 设置 folderPath
            if (!folders.isEmpty()) {
                String folderPath = folders.stream().map(BackupFolder::getName).collect(Collectors.joining("/"));
                backup.setFolderPath(folderPath);
            }

            result.add(backup);
        }
    } catch (Exception e) {
        LogUtil.getLogger().error("Export datasources failed", e);
    }
    return result;
}
```

- [ ] **Step 3: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDatasourceServiceImpl.java
git commit -m "feat(backup): collect folder chain when exporting datasources"
```

---

## Task 7: 实现 BackupDatasourceServiceImpl 目录导入

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDatasourceServiceImpl.java`

- [ ] **Step 1: 添加辅助方法 - 按 name + parentName 查找目录**

```java
/**
 * 按 name + parentName 查找数据源目录
 */
private CoreDatasource findDatasourceFolderByNameAndParent(String name, Long parentId) {
    QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", name);
    if (parentId != null && parentId != 0L) {
        queryWrapper.eq("pid", parentId);
    } else {
        queryWrapper.and(w -> w.eq("pid", 0L).or().isNull("pid"));
    }
    return coreDatasourceMapper.selectOne(queryWrapper);
}
```

- [ ] **Step 2: 添加辅助方法 - 创建或查找目录**

```java
/**
 * 创建或查找数据源目录，返回目录ID
 */
private Long createOrFindDatasourceFolder(BackupFolder folder, Map<String, Long> folderMapping) {
    String key = folder.getName() + "_" + (folder.getParentName() != null ? folder.getParentName() : "root");
    if (folderMapping.containsKey(key)) {
        return folderMapping.get(key);
    }

    // 先查找父目录ID
    Long parentId = 0L;
    if (folder.getParentName() != null) {
        String parentKey = folder.getParentName() + "_" + (folder.getPid() != null ? String.valueOf(folder.getPid()) : "root");
        parentId = folderMapping.getOrDefault(parentKey, 0L);
    }

    // 查找是否已存在
    CoreDatasource existing = findDatasourceFolderByNameAndParent(folder.getName(), parentId);
    if (existing != null) {
        folderMapping.put(key, existing.getId());
        return existing.getId();
    }

    // 创建新目录（实际上是创建父目录节点）
    CoreDatasource newFolder = new CoreDatasource();
    newFolder.setName(folder.getName());
    newFolder.setPid(parentId);
    newFolder.setEditType("1");
    newFolder.setCreateTime(System.currentTimeMillis());
    newFolder.setUpdateTime(System.currentTimeMillis());
    coreDatasourceMapper.insert(newFolder);

    newFolder = coreDatasourceMapper.selectOne(new QueryWrapper<CoreDatasource>()
        .eq("name", folder.getName())
        .eq("pid", parentId));

    folderMapping.put(key, newFolder.getId());
    return newFolder.getId();
}
```

- [ ] **Step 3: 修改 importDatasource 方法支持目录**

在导入数据源之前，先导入目录链：

```java
@Override
public String importDatasource(BackupDatasource datasource, boolean overwrite, Map<String, Long> folderMapping) {
    // 1. 先导入目录链
    if (datasource.getFolders() != null && !datasource.getFolders().isEmpty()) {
        datasource.getFolders().sort(Comparator.comparingInt(BackupFolder::getLevel));
        for (BackupFolder folder : datasource.getFolders()) {
            createOrFindDatasourceFolder(folder, folderMapping);
        }
    }

    // 2. 查找或创建数据源
    QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("name", datasource.getName());
    CoreDatasource existing = coreDatasourceMapper.selectOne(queryWrapper);

    if (existing != null && overwrite) {
        // ... 覆盖逻辑
    } else {
        String newName = datasource.getName();
        if (existing != null) {
            newName = generateUniqueName(datasource.getName());
        }
        CoreDatasource newDs = new CoreDatasource();
        newDs.setName(newName);
        newDs.setPid(0L); // 默认根目录
        // 如果有 folderPath，设置正确的 pid
        if (datasource.getFolderPath() != null && folderMapping.containsKey(datasource.getFolderPath())) {
            newDs.setPid(folderMapping.get(datasource.getFolderPath()));
        }
        // ... 其他字段
    }
    // ... 后续逻辑
}
```

- [ ] **Step 4: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDatasourceServiceImpl.java
git commit -m "feat(backup): import folder structure when importing datasources"
```

---

## Task 8: 实现 BackupDashboardServiceImpl 目录导出/导入

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDashboardServiceImpl.java`

**注意：** 目前 BackupDashboardServiceImpl 是空实现（TODO），需要完整实现仪表板和大屏的导出/导入逻辑。

- [ ] **Step 1: 添加依赖注入和辅助方法**

```java
@Autowired
private DataVisualizationInfoMapper dataVisualizationInfoMapper;

@Autowired
private CoreDatasetGroupMapper coreDatasetGroupMapper;

/**
 * 递归收集仪表板/大屏的目录链
 */
private List<BackupFolder> collectDashboardFolders(Long dvId, String type, Map<String, BackupFolder> folderMap) {
    List<BackupFolder> result = new ArrayList<>();
    DataVisualizationInfo dv = dataVisualizationInfoMapper.selectById(dvId);
    if (dv == null || dv.getPid() == null || dv.getPid() == 0L) {
        return result;
    }
    DataVisualizationInfo parent = dataVisualizationInfoMapper.selectById(dv.getPid());
    if (parent == null) {
        return result;
    }
    if ("folder".equals(parent.getNodeType())) {
        BackupFolder folder = new BackupFolder();
        folder.setId(String.valueOf(parent.getId()));
        folder.setName(parent.getName());
        folder.setPid(parent.getPid());
        folder.setLevel(parent.getLevel() != null ? parent.getLevel() : 0);
        folder.setNodeType(parent.getNodeType());
        folder.setResourceType("dashboard");
        folder.setSubType(type);
        folderMap.put(parent.getName(), folder);
        result.add(folder);
        result.addAll(collectDashboardFolders(parent.getId(), type, folderMap));
    }
    return result;
}
```

- [ ] **Step 2: 实现 exportDashboards 方法**

```java
@Override
public List<BackupDashboard> exportDashboards(List<String> dashboardIds) {
    List<BackupDashboard> result = new ArrayList<>();
    Map<String, BackupFolder> folderMap = new HashMap<>();
    try {
        List<DataVisualizationInfo> dashboards;
        if (dashboardIds != null && !dashboardIds.isEmpty()) {
            dashboards = dataVisualizationInfoMapper.selectBatchIds(dashboardIds.stream().map(Long::parseLong).collect(Collectors.toList()));
        } else {
            QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("node_type", "leaf").eq("type", "dashboard");
            dashboards = dataVisualizationInfoMapper.selectList(queryWrapper);
        }

        for (DataVisualizationInfo dv : dashboards) {
            List<BackupFolder> folders = collectDashboardFolders(dv.getId(), "dashboard", folderMap);

            BackupDashboard backup = new BackupDashboard();
            backup.setId(String.valueOf(dv.getId()));
            backup.setName(dv.getName());
            // ... 其他字段

            if (!folders.isEmpty()) {
                String folderPath = folders.stream().map(BackupFolder::getName).collect(Collectors.joining("/"));
                backup.setFolderPath(folderPath);
            }

            result.add(backup);
        }
    } catch (Exception e) {
        LogUtil.getLogger().error("Export dashboards failed", e);
    }
    return result;
}
```

- [ ] **Step 3: 实现 exportDataviews 方法**

类似 exportDashboards，但 type = "dataV"

- [ ] **Step 4: 实现 importDashboard 方法支持目录**

类似 BackupDatasourceServiceImpl 的实现

- [ ] **Step 5: 实现 importDataview 方法支持目录**

类似 importDashboard

- [ ] **Step 6: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDashboardServiceImpl.java
git commit -m "feat(backup): implement dashboard/dataview export/import with folder support"
```

---

## Task 9: 修改 BackupCenterManage 调用目录导出/导入

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java`

- [ ] **Step 1: 修改 export 方法收集目录**

在 `export()` 方法中，调用各服务的导出方法后，收集所有目录并打包：

```java
// 在 export() 方法中
List<BackupFolder> allFolders = new ArrayList<>();
Map<String, BackupFolder> folderMap = new HashMap<>();

if ("dataset".equals(type)) {
    List<BackupDataset> datasets = backupDatasetService.exportDatasets(request.getResourceIds());
    // 收集目录链到 folderMap
    for (BackupDataset ds : datasets) {
        if (ds.getFolders() != null) {
            allFolders.addAll(ds.getFolders());
        }
    }
    exportPackage.setDatasets(datasets);
} else if ("datasource".equals(type)) {
    List<BackupDatasource> datasources = backupDatasourceService.exportDatasources(request.getResourceIds());
    // 收集目录链到 folderMap
    for (BackupDatasource ds : datasources) {
        if (ds.getFolders() != null) {
            allFolders.addAll(ds.getFolders());
        }
    }
    exportPackage.setDatasources(datasources);
} else if ("dashboard".equals(type)) {
    List<BackupDashboard> dashboards = backupDashboardService.exportDashboards(request.getResourceIds());
    // 收集目录链到 folderMap
    exportPackage.setDashboards(dashboards);
}

// 去重并设置到 exportPackage
exportPackage.setFolders(allFolders.stream().distinct().collect(Collectors.toList()));
```

- [ ] **Step 2: 修改 importData 方法导入目录**

在 `importData()` 方法中，先导入目录，再导入资源：

```java
// 在 importData() 方法中
Map<String, Long> datasetFolderMapping = new HashMap<>();
Map<String, Long> datasourceFolderMapping = new HashMap<>();
Map<String, Long> dashboardFolderMapping = new HashMap<>();

// 1. 先按 level 排序导入所有目录
if (exportPackage.getFolders() != null) {
    List<BackupFolder> folders = exportPackage.getFolders();
    folders.sort(Comparator.comparingInt(BackupFolder::getLevel));

    for (BackupFolder folder : folders) {
        if ("dataset".equals(folder.getResourceType())) {
            backupDatasetService.createOrFindDatasetFolder(folder, datasetFolderMapping);
        } else if ("datasource".equals(folder.getResourceType())) {
            backupDatasourceService.createOrFindDatasourceFolder(folder, datasourceFolderMapping);
        } else if ("dashboard".equals(folder.getResourceType())) {
            backupDashboardService.createOrFindDashboardFolder(folder, dashboardFolderMapping);
        }
    }
}

// 2. 再导入资源
if (exportPackage.getDatasets() != null) {
    for (BackupDataset ds : exportPackage.getDatasets()) {
        backupDatasetService.importDatasetWithTables(ds, request.isOverwrite(), datasourceIdMapping, datasetFolderMapping);
    }
}
// ... 类似处理 datasources, dashboards
```

- [ ] **Step 3: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java
git commit -m "feat(backup): integrate folder export/import in BackupCenterManage"
```

---

## Task 10: 测试验证

**Files:**
- 无

- [ ] **Step 1: 编译验证**

```bash
cd core/core-backend
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean compile -DskipTests
```

- [ ] **Step 2: 启动后端**

```bash
# 关闭已有进程
taskkill //F //PID $(tasklist | grep java | awk '{print $2}' | head -1)

# 启动
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.6.7-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
java -jar target/CoreApplication.jar --spring.profiles.active=standalone
```

- [ ] **Step 3: 功能测试**

1. 导出有目录结构的数据集 → 验证导出文件包含 folders 字段
2. 导出仪表板 → 验证 dashboard 和 dataV 类型都正确
3. 导入到新环境 → 验证目录结构正确恢复
4. 导入时目录已存在 → 验证跳过策略
5. 导入旧备份文件 → 验证向后兼容性

---

## 注意事项

1. **向后兼容**：旧备份文件没有 `folders` 字段，导入时应忽略，直接散落在根目录
2. **目录匹配**：通过 `name + parentName` 匹配，而非 ID，确保跨环境兼容
3. **循环引用保护**：通过 `level` 排序确保父目录先创建
4. **跳过策略**：目录已存在时跳过，不覆盖
5. **版本升级**：ExportPackage 的 version 从 "1.0" 升级到 "2.0"
