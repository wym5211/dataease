# 数据集备份增强实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `core_dataset_table` 和 `core_dataset_table_field` 纳入数据集备份导出/导入流程，支持跨环境迁移

**Architecture:** 在 `BackupDataset` 中嵌套 `tables` 和 `fields`，导出时按 `dataset_group_id` 查询关联数据，导入时通过 ID 映射表处理关联关系

**Tech Stack:** Java, Spring Boot, MyBatis-Plus, Lombok

---

## 文件结构

| 文件 | 操作 |
|------|------|
| `sdk/common/src/main/java/io/dataease/model/backup/BackupDatasetTable.java` | 新增 |
| `sdk/common/src/main/java/io/dataease/model/backup/BackupDatasetTableField.java` | 新增 |
| `sdk/common/src/main/java/io/dataease/model/backup/BackupDataset.java` | 修改 |
| `core/core-backend/.../BackupDatasetServiceImpl.java` | 修改 |
| `core/core-backend/.../BackupCenterManage.java` | 修改 |

## 关键设计决策

### ID 映射表设计

导入时维护**独立的映射表**，避免 ID 冲突：

```java
Map<String, String> datasourceIdMapping;  // old_datasource_id → new_datasource_id
Map<String, String> datasetIdMapping;       // old_dataset_id → new_dataset_id
Map<String, String> tableIdMapping;        // old_table_id → new_table_id
```

### Datasource 名称匹配

在 `BackupDatasetTable` 中增加 `datasourceName` 字段，导出时存储原始数据源名称，导入时按名称匹配。

---

## Task 1: 新增 BackupDatasetTableField.java

**Files:**
- Create: `sdk/common/src/main/java/io/dataease/model/backup/BackupDatasetTableField.java`
- Reference: `core/core-backend/.../CoreDatasetTableField.java`

- [ ] **Step 1: 创建 BackupDatasetTableField.java**

```java
package io.dataease.model.backup;

import lombok.Data;

/**
 * 数据集表字段备份信息
 */
@Data
public class BackupDatasetTableField {

    private String id;
    private String originName;
    private String name;
    private String dataeaseName;
    private String fieldShortName;
    private String groupType;
    private String type;
    private Integer size;
    private Integer deType;
    private Integer deExtractType;
    private Integer extField;
    private Boolean checked;
    private Integer columnIndex;
    private Integer accuracy;
    private String dateFormat;
    private String dateFormatType;
    private String params;
    private Boolean orderChecked;
    private String groupList;
    private String otherGroup;
}
```

- [ ] **Step 2: Commit**

```bash
git add sdk/common/src/main/java/io/dataease/model/backup/BackupDatasetTableField.java
git commit -m "feat(backup): add BackupDatasetTableField model"
```

---

## Task 2: 新增 BackupDatasetTable.java

**Files:**
- Create: `sdk/common/src/main/java/io/dataease/model/backup/BackupDatasetTable.java`
- Reference: `core/core-backend/.../CoreDatasetTable.java`

- [ ] **Step 1: 创建 BackupDatasetTable.java**

```java
package io.dataease.model.backup;

import lombok.Data;
import java.util.List;

/**
 * 数据集物理表备份信息
 */
@Data
public class BackupDatasetTable {

    private String id;
    private String name;
    private String tableName;
    private String datasourceId;   // 原始ID，备用
    private String datasourceName; // 数据源名称，导入时按名称匹配
    private String type;
    private String info;
    private String sqlVariableDetails;
    private List<BackupDatasetTableField> fields;
}
```

- [ ] **Step 2: Commit**

```bash
git add sdk/common/src/main/java/io/dataease/model/backup/BackupDatasetTable.java
git commit -m "feat(backup): add BackupDatasetTable model"
```

---

## Task 3: 修改 BackupDataset.java 增加嵌套 tables

**Files:**
- Modify: `sdk/common/src/main/java/io/dataease/model/backup/BackupDataset.java:10-72`

- [ ] **Step 1: 添加 tables 字段**

在 `BackupDataset.java` 中添加：

```java
import java.util.List;

// 在现有字段后添加
private List<BackupDatasetTable> tables;
```

- [ ] **Step 2: Commit**

```bash
git add sdk/common/src/main/java/io/dataease/model/backup/BackupDataset.java
git commit -m "feat(backup): add nested tables field to BackupDataset"
```

---

## Task 4: 修改 BackupDatasetServiceImpl 实现导出逻辑

**Files:**
- Modify: `core/core-backend/.../BackupDatasetServiceImpl.java`
- Add Autowired: `CoreDatasetTableMapper`, `CoreDatasetTableFieldMapper`, `CoreDatasourceMapper`

- [ ] **Step 1: 添加新的 Mapper 注入**

```java
@Autowired
private CoreDatasetTableMapper coreDatasetTableMapper;

@Autowired
private CoreDatasetTableFieldMapper coreDatasetTableFieldMapper;

@Autowired
private CoreDatasourceMapper coreDatasourceMapper;
```

- [ ] **Step 2: 修改 exportDatasets() 方法**

在循环中，为每个 dataset 查询其关联的 tables 和 fields：

```java
@Override
public List<BackupDataset> exportDatasets() {
    List<BackupDataset> result = new ArrayList<>();
    try {
        QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("node_type", "dataset");
        List<CoreDatasetGroup> datasets = coreDatasetGroupMapper.selectList(queryWrapper);

        for (CoreDatasetGroup ds : datasets) {
            BackupDataset backup = new BackupDataset();
            backup.setId(String.valueOf(ds.getId()));
            backup.setName(ds.getName());
            backup.setType(ds.getType());
            backup.setModel(ds.getInfo());
            backup.setUnionSql(ds.getUnionSql());
            backup.setCreateBy(ds.getCreateBy());
            backup.setCreateTime(ds.getCreateTime());
            backup.setUpdateTime(ds.getLastUpdateTime());

            // 查询关联的 tables
            QueryWrapper<CoreDatasetTable> tableQuery = new QueryWrapper<>();
            tableQuery.eq("dataset_group_id", ds.getId());
            List<CoreDatasetTable> tables = coreDatasetTableMapper.selectList(tableQuery);

            List<BackupDatasetTable> backupTables = new ArrayList<>();
            for (CoreDatasetTable table : tables) {
                BackupDatasetTable backupTable = new BackupDatasetTable();
                backupTable.setId(String.valueOf(table.getId()));
                backupTable.setName(table.getName());
                backupTable.setTableName(table.getTableName());
                backupTable.setDatasourceId(String.valueOf(table.getDatasourceId()));
                // 按 dataSourceId 查询数据源名称并存储
                if (table.getDatasourceId() != null) {
                    CoreDatasource ds = coreDatasourceMapper.selectById(table.getDatasourceId());
                    if (ds != null) {
                        backupTable.setDatasourceName(ds.getName());
                    }
                }
                backupTable.setType(table.getType());
                backupTable.setInfo(table.getInfo());
                backupTable.setSqlVariableDetails(table.getSqlVariableDetails());

                // 查询关联的 fields
                QueryWrapper<CoreDatasetTableField> fieldQuery = new QueryWrapper<>();
                fieldQuery.eq("dataset_table_id", table.getId());
                List<CoreDatasetTableField> fields = coreDatasetTableFieldMapper.selectList(fieldQuery);

                List<BackupDatasetTableField> backupFields = new ArrayList<>();
                for (CoreDatasetTableField field : fields) {
                    BackupDatasetTableField backupField = new BackupDatasetTableField();
                    backupField.setId(String.valueOf(field.getId()));
                    backupField.setOriginName(field.getOriginName());
                    backupField.setName(field.getName());
                    backupField.setDataeaseName(field.getDataeaseName());
                    backupField.setFieldShortName(field.getFieldShortName());
                    backupField.setGroupType(field.getGroupType());
                    backupField.setType(field.getType());
                    backupField.setSize(field.getSize());
                    backupField.setDeType(field.getDeType());
                    backupField.setDeExtractType(field.getDeExtractType());
                    backupField.setExtField(field.getExtField());
                    backupField.setChecked(field.getChecked());
                    backupField.setColumnIndex(field.getColumnIndex());
                    backupField.setAccuracy(field.getAccuracy());
                    backupField.setDateFormat(field.getDateFormat());
                    backupField.setDateFormatType(field.getDateFormatType());
                    backupField.setParams(field.getParams());
                    backupField.setOrderChecked(field.getOrderChecked());
                    backupField.setGroupList(field.getGroupList());
                    backupField.setOtherGroup(field.getOtherGroup());
                    backupFields.add(backupField);
                }
                backupTable.setFields(backupFields);
                backupTables.add(backupTable);
            }
            backup.setTables(backupTables);
            result.add(backup);
        }
    } catch (Exception e) {
        LogUtil.getLogger().error("Export datasets failed", e);
    }
    return result;
}
```

- [ ] **Step 3: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDatasetServiceImpl.java
git commit -m "feat(backup): implement export of dataset tables and fields"
```

---

## Task 5: 修改 importDataset() 实现完整的导入逻辑

**Files:**
- Modify: `core/core-backend/.../BackupDatasetServiceImpl.java`

- [ ] **Step 1: 添加新方法 importDatasetWithTables()**

原有 `importDataset(dataset, overwrite, idMapping)` 方法保持兼容，新增带 table 导入的方法：

```java
/**
 * 导入数据集及其关联的 tables 和 fields
 * @param dataset 数据集备份信息
 * @param overwrite 是否覆盖
 * @param datasourceIdMapping 数据源 ID 映射表 (old_id → new_id)
 * @return 新数据集 ID
 */
public String importDatasetWithTables(BackupDataset dataset, boolean overwrite,
                                       Map<String, String> datasourceIdMapping) {
    Map<String, String> datasetIdMapping = new HashMap<>();
    return importDatasetWithTables(dataset, overwrite, datasourceIdMapping, datasetIdMapping);
}

private String importDatasetWithTables(BackupDataset dataset, boolean overwrite,
                                       Map<String, String> datasourceIdMapping,
                                       Map<String, String> datasetIdMapping) {
    try {
        QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", dataset.getName());
        CoreDatasetGroup existing = coreDatasetGroupMapper.selectOne(queryWrapper);

        String newDatasetId;
        if (existing != null && overwrite) {
            existing.setType(dataset.getType());
            existing.setInfo(dataset.getModel());
            coreDatasetGroupMapper.updateById(existing);
            newDatasetId = String.valueOf(existing.getId());
            // 覆盖模式下，先删除原有的 tables 和 fields
            deleteExistingTablesAndFields(Long.parseLong(newDatasetId));
        } else {
            String newName = dataset.getName();
            if (existing != null) {
                newName = generateUniqueName(dataset.getName());
            }
            CoreDatasetGroup newDs = new CoreDatasetGroup();
            newDs.setName(newName);
            newDs.setPid(0L);
            newDs.setLevel(0);
            newDs.setNodeType("dataset");
            newDs.setType(dataset.getType());
            newDs.setInfo(dataset.getModel());
            newDs.setUnionSql(dataset.getUnionSql());
            newDs.setCreateBy("1");
            newDs.setCreateTime(System.currentTimeMillis());
            coreDatasetGroupMapper.insert(newDs);
            newDs = coreDatasetGroupMapper.selectOne(new QueryWrapper<CoreDatasetGroup>().eq("name", newName));
            newDatasetId = String.valueOf(newDs.getId());
        }

        // 更新 dataset ID 映射
        datasetIdMapping.put(dataset.getId(), newDatasetId);

        // 导入 tables 和 fields
        if (dataset.getTables() != null) {
            Map<String, String> tableIdMapping = new HashMap<>();
            for (BackupDatasetTable backupTable : dataset.getTables()) {
                importDatasetTable(backupTable, newDatasetId, datasourceIdMapping, tableIdMapping);
            }
        }

        LogUtil.getLogger().info("=== Backup import dataset: id={}, name={}, newId={} ===", dataset.getId(), dataset.getName(), newDatasetId);
        return newDatasetId;
    } catch (Exception e) {
        LogUtil.getLogger().error("Import dataset failed: " + dataset.getName(), e);
        throw e;
    }
}

/**
 * 覆盖模式下，先删除目标数据集中原有的 tables 和 fields
 */
private void deleteExistingTablesAndFields(Long datasetId) {
    // 查询该数据集下的所有 table
    QueryWrapper<CoreDatasetTable> tableQuery = new QueryWrapper<>();
    tableQuery.eq("dataset_group_id", datasetId);
    List<CoreDatasetTable> existingTables = coreDatasetTableMapper.selectList(tableQuery);

    for (CoreDatasetTable table : existingTables) {
        // 删除关联的 fields
        QueryWrapper<CoreDatasetTableField> fieldQuery = new QueryWrapper<>();
        fieldQuery.eq("dataset_table_id", table.getId());
        coreDatasetTableFieldMapper.delete(fieldQuery);
    }
    // 删除 tables
    coreDatasetTableMapper.delete(tableQuery);
}

private void importDatasetTable(BackupDatasetTable backupTable, String newDatasetId,
                                  Map<String, String> datasourceIdMapping,
                                  Map<String, String> tableIdMapping) {
    // 1. datasourceId 按名称匹配
    Long newDatasourceId = null;
    if (backupTable.getDatasourceName() != null) {
        // 按名称查找数据源
        QueryWrapper<CoreDatasource> dsQuery = new QueryWrapper<>();
        dsQuery.eq("name", backupTable.getDatasourceName());
        CoreDatasource ds = coreDatasourceMapper.selectOne(dsQuery);
        if (ds != null) {
            newDatasourceId = ds.getId();
        } else {
            LogUtil.getLogger().warn("=== Datasource not found by name: {} for table: {} ===",
                backupTable.getDatasourceName(), backupTable.getName());
        }
    }

    // 2. 创建或更新 table
    QueryWrapper<CoreDatasetTable> tableQuery = new QueryWrapper<>();
    tableQuery.eq("name", backupTable.getName()).eq("dataset_group_id", Long.parseLong(newDatasetId));
    CoreDatasetTable existingTable = coreDatasetTableMapper.selectOne(tableQuery);

    String newTableId;
    if (existingTable != null) {
        existingTable.setTableName(backupTable.getTableName());
        existingTable.setDatasourceId(newDatasourceId);
        existingTable.setType(backupTable.getType());
        existingTable.setInfo(backupTable.getInfo());
        existingTable.setSqlVariableDetails(backupTable.getSqlVariableDetails());
        coreDatasetTableMapper.updateById(existingTable);
        newTableId = String.valueOf(existingTable.getId());
    } else {
        CoreDatasetTable newTable = new CoreDatasetTable();
        newTable.setName(backupTable.getName());
        newTable.setTableName(backupTable.getTableName());
        newTable.setDatasourceId(newDatasourceId);
        newTable.setDatasetGroupId(Long.parseLong(newDatasetId));
        newTable.setType(backupTable.getType());
        newTable.setInfo(backupTable.getInfo());
        newTable.setSqlVariableDetails(backupTable.getSqlVariableDetails());
        coreDatasetTableMapper.insert(newTable);
        newTableId = String.valueOf(newTable.getId());
    }

    // 更新 table ID 映射
    tableIdMapping.put(backupTable.getId(), newTableId);

    // 3. 导入 fields
    QueryWrapper<CoreDatasetTable> tableQuery = new QueryWrapper<>();
    tableQuery.eq("name", backupTable.getName()).eq("dataset_group_id", Long.parseLong(newDatasetId));
    CoreDatasetTable existingTable = coreDatasetTableMapper.selectOne(tableQuery);

    String newTableId;
    if (existingTable != null) {
        // 覆盖
        existingTable.setTableName(backupTable.getTableName());
        existingTable.setDatasourceId(newDatasourceId);
        existingTable.setType(backupTable.getType());
        existingTable.setInfo(backupTable.getInfo());
        existingTable.setSqlVariableDetails(backupTable.getSqlVariableDetails());
        coreDatasetTableMapper.updateById(existingTable);
        newTableId = String.valueOf(existingTable.getId());
    } else {
        // 新建
        CoreDatasetTable newTable = new CoreDatasetTable();
        newTable.setName(backupTable.getName());
        newTable.setTableName(backupTable.getTableName());
        newTable.setDatasourceId(newDatasourceId);
        newTable.setDatasetGroupId(Long.parseLong(newDatasetId));
        newTable.setType(backupTable.getType());
        newTable.setInfo(backupTable.getInfo());
        newTable.setSqlVariableDetails(backupTable.getSqlVariableDetails());
        coreDatasetTableMapper.insert(newTable);
        newTableId = String.valueOf(newTable.getId());
    }

    // 更新 table ID 映射
    tableIdMapping.put(backupTable.getId(), newTableId);

    // 3. 导入 fields
    if (backupTable.getFields() != null) {
        for (BackupDatasetTableField backupField : backupTable.getFields()) {
            importDatasetTableField(backupField, newTableId, tableIdMapping);
        }
    }
}

private void importDatasetTableField(BackupDatasetTableField backupField, String newTableId,
                                       Map<String, String> tableIdMapping) {
    // 查询是否已存在（按 dataeaseName 和 dataset_table_id）
    QueryWrapper<CoreDatasetTableField> fieldQuery = new QueryWrapper<>();
    fieldQuery.eq("dataease_name", backupField.getDataeaseName())
              .eq("dataset_table_id", Long.parseLong(newTableId));
    CoreDatasetTableField existingField = coreDatasetTableFieldMapper.selectOne(fieldQuery);

    if (existingField != null) {
        // 覆盖
        existingField.setOriginName(backupField.getOriginName());
        existingField.setName(backupField.getName());
        existingField.setFieldShortName(backupField.getFieldShortName());
        existingField.setGroupType(backupField.getGroupType());
        existingField.setType(backupField.getType());
        existingField.setSize(backupField.getSize());
        existingField.setDeType(backupField.getDeType());
        existingField.setDeExtractType(backupField.getDeExtractType());
        existingField.setExtField(backupField.getExtField());
        existingField.setChecked(backupField.getChecked());
        existingField.setColumnIndex(backupField.getColumnIndex());
        existingField.setAccuracy(backupField.getAccuracy());
        existingField.setDateFormat(backupField.getDateFormat());
        existingField.setDateFormatType(backupField.getDateFormatType());
        existingField.setParams(backupField.getParams());
        existingField.setOrderChecked(backupField.getOrderChecked());
        existingField.setGroupList(backupField.getGroupList());
        existingField.setOtherGroup(backupField.getOtherGroup());
        coreDatasetTableFieldMapper.updateById(existingField);
    } else {
        // 新建
        CoreDatasetTableField newField = new CoreDatasetTableField();
        newField.setDatasetTableId(Long.parseLong(newTableId));
        newField.setOriginName(backupField.getOriginName());
        newField.setName(backupField.getName());
        newField.setDataeaseName(backupField.getDataeaseName());
        newField.setFieldShortName(backupField.getFieldShortName());
        newField.setGroupType(backupField.getGroupType());
        newField.setType(backupField.getType());
        newField.setSize(backupField.getSize());
        newField.setDeType(backupField.getDeType());
        newField.setDeExtractType(backupField.getDeExtractType());
        newField.setExtField(backupField.getExtField());
        newField.setChecked(backupField.getChecked());
        newField.setColumnIndex(backupField.getColumnIndex());
        newField.setAccuracy(backupField.getAccuracy());
        newField.setDateFormat(backupField.getDateFormat());
        newField.setDateFormatType(backupField.getDateFormatType());
        newField.setParams(backupField.getParams());
        newField.setOrderChecked(backupField.getOrderChecked());
        newField.setGroupList(backupField.getGroupList());
        newField.setOtherGroup(backupField.getOtherGroup());
        coreDatasetTableFieldMapper.insert(newField);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDatasetServiceImpl.java
git commit -m "feat(backup): implement import of dataset tables and fields with ID mapping"
```

---

## Task 6: 修改 BackupCenterManage 调用新方法

**Files:**
- Modify: `core/core-backend/.../BackupCenterManage.java`

- [ ] **Step 1: 修改 dataset 导入调用**

在 `BackupCenterManage.importData()` 方法中，将 dataset 导入调用从：

```java
backupDatasetService.importDataset(ds, request.isOverwrite(), idMapping);
```

改为：

```java
backupDatasetService.importDatasetWithTables(ds, request.isOverwrite(), idMapping);
```

- [ ] **Step 2: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java
git commit -m "feat(backup): call importDatasetWithTables for dataset import"
```

---

## Task 7: 验证编译

- [ ] **Step 1: 编译项目**

```bash
cd core/core-backend
mvn compile -q
```

预期：无编译错误

---

## 注意事项

1. **datasourceId 匹配**：已通过在 `BackupDatasetTable` 中增加 `datasourceName` 字段解决，导出时存储名称，导入时按名称匹配

2. **导入顺序**：必须确保 datasource 在 dataset 之前导入并完成 idMapping（当前实现已满足）

3. **现有方法兼容性**：保持原有 `importDataset(dataset, overwrite, idMapping)` 方法签名，向后兼容；新增 `importDatasetWithTables()` 方法

---

## 测试要点

1. 正常导出/导入：数据集及其 table、field 完整迁移
2. 同名覆盖：overwrite=true 时，关联的 table 和 field 也覆盖
3. 数据源匹配：目标库有/无同名数据源时的处理
4. 字段映射：dataset_table_field 的 dataset_table_id 正确映射
