# 备份目录增强设计文档

## 需求

支持导入导出目录结构，备份整个目录树和资源的关系。

## 背景

当前备份系统只导出资源本身，没有导出目录结构。导入后所有资源散落在根目录，无法恢复原有的目录归属关系。

## 三套独立的目录树

| 资源类型 | 目录表 | 目录标识 | 资源类型标识 |
|---------|-------|---------|-------------|
| 数据集 | `core_dataset_group` | `node_type='folder'` | `node_type='dataset'` |
| 仪表板/数据大屏 | `data_visualization_info` | `node_type='folder'` | `type='dashboard'/'dataV'` |
| 数据源 | `core_datasource` | `pid != 0` | - |

## 数据模型

### BackupFolder.java

```java
@Data
public class BackupFolder {
    private String id;
    private String name;
    private Long pid;           // 父目录ID
    private Integer level;       // 层级深度
    private String nodeType;     // folder
    private String resourceType; // dataset | dashboard | datasource
    private String subType;      // dashboard/dataV (仅 dashboard 类型需要)
}
```

## 导出流程

```
导出请求 (type=dataset)
  → 遍历 core_dataset_group，构建 dataset 文件夹树
  → 递归收集选中数据集的目录链（直到根）
  → 打包：folders + datasets

导出请求 (type=dashboard)
  → 遍历 data_visualization_info，构建 dashboard 文件夹树
  → 递归收集选中仪表板的目录链
  → 打包：folders + dashboards

导出请求 (type=datasource)
  → 遍历 core_datasource，构建 datasource 目录树
  → 递归收集选中数据源的目录链
  → 打包：folders + datasources
```

### 导出步骤详解

1. **构建目录树**：查询所有 `node_type='folder'` 的记录，构建树形结构
2. **递归收集目录链**：对于选中的资源，递归向上收集所有父目录直到根节点
3. **去重合并**：确保目录列表不重复，资源只出现一次
4. **打包导出**：将 folders 和 resources 一起序列化到备份文件

## 导入流程

```
收到备份文件
  ↓
1. 按 level ASC 排序，依次创建目录
   - dataset 类型 → 在 core_dataset_group 创建/查找
   - dashboard 类型 → 在 data_visualization_info 创建/查找
   - datasource 类型 → 在 core_datasource 创建/查找
   - 跳过已存在的目录（按 name + parentName 匹配）
  ↓
2. 建立 "name + parentName" → 新ID 的映射
  ↓
3. 导入资源，设置正确的 pid
```

### 导入步骤详解

1. **排序处理**：按 `level` 升序排列，确保父目录先于子目录创建
2. **目录匹配**：通过 `name + parentName` 唯一标识目录，而非 ID
3. **跳过策略**：如果目录已存在，跳过该目录，保持原样
4. **ID 映射**：建立原目录 ID → 新目录 ID 的映射，供资源导入时使用
5. **资源导入**：使用目录映射设置正确的 `pid`

## 目录匹配方式

通过 `name + parentName` 唯一标识，而不是 ID：

```
备份目录: "子目录" (父目录名为 "父目录")
目标目录: 查找名为 "子目录" 且父目录名为 "父目录" 的目录
```

这种方式确保跨环境导入时能正确匹配目录结构。

## 循环引用保护

通过 `level` 排序确保父目录先创建：

```java
folders.sort(Comparator.comparingInt(BackupFolder::getLevel));
for (BackupFolder folder : folders) {
    // 创建目录，此时父目录已存在
}
```

## 向后兼容

旧备份文件没有目录信息，导入时：
- 直接散落在根目录（`pid = 0`）
- 不影响现有功能

## 实现文件

### 新增

- `sdk/common/src/main/java/io/dataease/model/backup/BackupFolder.java`

### 修改

- `core/core-backend/.../backup/service/impl/BackupDatasetServiceImpl.java`
  - 导出时收集目录链
  - 导入时创建目录并建立映射

- `core/core-backend/.../backup/service/impl/BackupDatasourceServiceImpl.java`
  - 导出时收集目录链
  - 导入时创建目录并建立映射

- `core/core-backend/.../backup/service/impl/BackupDashboardServiceImpl.java`
  - 实现目录导出/导入逻辑

- `core/core-backend/.../backup/service/impl/BackupServiceImpl.java` (如果存在)
  - 统一处理目录的导出/导入

- `sdk/common/.../model/backup/BackupDataset.java`
  - 增加 `folderPath` 字段（可选，用于日志追踪）

- `sdk/common/.../model/backup/BackupDatasource.java`
  - 增加 `folderPath` 字段

- `sdk/common/.../model/backup/BackupDashboard.java`
  - 增加 `folderPath` 字段

## 测试场景

1. **导出有目录结构的数据集** → 验证目录树完整导出
2. **导出仪表板** → 验证 dashboard 和 dataV 类型都正确
3. **跨环境导入** → 验证目录正确匹配
4. **导入时目录已存在** → 验证跳过策略
5. **旧备份文件导入** → 验证向后兼容性
