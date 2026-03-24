# 数据集备份增强：core_dataset_table / core_dataset_table_field 导出设计

## 背景

当前数据集备份只导出 `core_dataset_group`，缺失 `core_dataset_table`（物理表）和 `core_dataset_table_field`（字段定义）。这导致跨环境迁移时数据集的表和字段信息丢失。

## 目标

将 `core_dataset_table` 和 `core_dataset_table_field` 纳入数据集备份导出/导入流程，支持跨环境迁移。

## 核心设计

### 1. 数据模型

在 `BackupDataset` 中嵌套 `tables` 和 `fields`：

```
BackupDataset
├── id, name, type, model, unionSql...
└── tables: List<BackupDatasetTable>
    ├── id, tableName, type, info, sqlVariableDetails, datasourceId (原始)
    └── fields: List<BackupDatasetTableField>
        ├── id, originName, name, dataeaseName, fieldShortName
        ├── groupType, type, dataeaseType, length, precision
        └── ...
```

### 2. 导出流程

1. 查询 `core_dataset_group`（node_type='dataset'）
2. 对每个 dataset，查询其关联的 `core_dataset_table`（按 dataset_group_id）
3. 对每个 table，查询其字段 `core_dataset_table_field`（按 dataset_table_id）
4. **datasource_id 按原值导出**（仅存储，用于导入时匹配）

### 3. 导入流程

**导入顺序：** datasource → dataset → dataset_table → dataset_table_field

#### 3.1 Dataset 导入
1. 按现有逻辑导入/覆盖 `core_dataset_group`
2. **记录 ID 映射**：`old_dataset_id → new_dataset_id`

#### 3.2 DatasetTable 导入
1. 遍历导出的 tables
2. **datasource_id 按名称匹配**：
   - 在目标库查找同名数据源
   - 找到则用新 ID，找不到则置为 null 并记录警告
3. **dataset_group_id 用 idMapping 映射**
4. 创建/覆盖 `core_dataset_table`，**记录 ID 映射**：`old_table_id → new_table_id`

#### 3.3 DatasetTableField 导入
1. 遍历导出的 fields
2. **dataset_table_id 用 idMapping 映射**（old_table_id → new_table_id）
3. 创建/覆盖 `core_dataset_table_field`

### 4. ID 映射表结构

```java
// 导入时维护两张映射
Map<String, String> datasourceIdMapping;  // old_datasource_id → new_datasource_id
Map<String, String> datasetIdMapping;       // old_dataset_id → new_dataset_id
Map<String, String> tableIdMapping;        // old_table_id → new_table_id
```

## 涉及文件

| 文件 | 改动 |
|------|------|
| `sdk/common/.../BackupDataset.java` | 增加 `tables` 字段 |
| `sdk/common/.../BackupDatasetTable.java` | 新增 |
| `sdk/common/.../BackupDatasetTableField.java` | 新增 |
| `BackupDatasetServiceImpl.java` | 实现导出/导入逻辑 |
| `BackupCenterManage.java` | 调整导入顺序（先datasource后dataset） |

## 导出 JSON 示例

```json
{
  "id": "123456",
  "name": "销售数据集",
  "type": "sql",
  "model": "...",
  "tables": [
    {
      "id": "1001",
      "tableName": "sales_order",
      "type": "db",
      "datasourceId": "99",
      "info": "...",
      "fields": [
        {
          "id": "10001",
          "originName": "order_id",
          "name": "订单ID",
          "dataeaseName": "order_id",
          "groupType": "d",
          "type": "INT"
        }
      ]
    }
  ]
}
```

## 测试要点

1. **正常导出/导入**：数据集及其 table、field 完整迁移
2. **同名覆盖**：overwrite=true 时，关联的 table 和 field 也覆盖
3. **数据源匹配**：目标库有/无同名数据源时的处理
4. **字段映射**：dataset_table_field 的 dataset_table_id 正确映射
