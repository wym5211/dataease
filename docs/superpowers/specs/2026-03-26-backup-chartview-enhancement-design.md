# 图表数据集按名称匹配导入增强设计（修订版）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在导入仪表板/大屏时，通过数据集名称智能匹配图表的数据集引用，解决"Cannot read properties of undefined (reading 'type')" 错误。

**Architecture:** 在导入流程中增加数据集名称查找逻辑，同时匹配 CoreDatasetTable 和 CoreDatasetGroup 的名称。

**Tech Stack:** Spring Boot 3.3 + Java 21 + MyBatis-Plus

---

## 背景

导入仪表板/大屏时，图表(chart) 关联的数据集通过 `tableId` 字段引用。由于不同环境间的 ID 不同，需要将 `tableId` 映射到目标环境中的数据集。

**数据模型关系:**
```
CoreChartView.tableId → CoreDatasetTable.id
CoreDatasetTable.datasetGroupId → CoreDatasetGroup.id
CoreDatasetTable.name → 表名称
CoreDatasetGroup.name → 数据集名称
```

当前实现通过 `datasetIdMapping`（`Map<String, Long>`）进行 ID 映射，但该映射存储的是 `CoreDatasetGroup.id`，而图表的 `tableId` 实际关联的是 `CoreDatasetTable.id`，导致 ID 不匹配。

## 解决方案

**核心思路:** 改为通过**数据集名称 + 表名称**进行匹配，而不是依赖 ID 映射。

**查找链路:**
1. 通过原 `tableId` 查询 `CoreDatasetTable` 获取表名称和 `datasetGroupId`
2. 通过 `datasetGroupId` 查询 `CoreDatasetGroup` 获取数据集名称
3. 按数据集名称在目标系统的 `core_dataset_group` 表中查找数据集
4. 按数据集ID + 表名称在目标系统的 `core_dataset_table` 表中查找表
5. 找到则使用新 `tableId`
6. 找不到则记录缺失信息

## 修改的文件

### 1. 新增模型类 (sdk/common/src/main/java/io/dataease/model/backup/)
```java
package io.dataease.model.backup;

import lombok.Data;
import java.util.List;

/**
 * 图表导入结果
 */
@Data
public class ChartImportResult {
    /**
     * 成功导入的图表
     */
    private List<ChartInfo> importedCharts;

    /**
     * 缺失数据集的图表
     */
    private List<ChartMissingDataset> missingDatasets;

    /**
     * 成功数量
     */
    private int successCount;

    /**
     * 缺失数量
     */
    private int missingCount;

    @Data
    public static class ChartInfo {
        private String oldId;
        private String title;
        private Long newTableId;
    }

    @Data
    public static class ChartMissingDataset {
        private String chartId;
        private String chartTitle;
        private Long oldTableId;
        private String datasetName;
        private String tableName;
    }
}
```

### 2. BackupDashboardService.java (接口)
```java
// 新增方法
/**
 * 导入图表并返回结果
 * @param charts 图表列表
 * @param dashboardIdMapping 仪表板ID映射
 * @param overwrite 是否覆盖
 * @return 导入结果（包含成功和缺失信息）
 */
ChartImportResult importChartsWithResult(List<BackupChartView> charts,
                                            Map<String, Long> dashboardIdMapping,
                                            boolean overwrite);
```

### 3. BackupDashboardServiceImpl.java (实现)

```java
@Autowired
private CoreDatasetTableMapper coreDatasetTableMapper;

@Autowired
private CoreDatasetGroupMapper coreDatasetGroupMapper;

/**
 * 通过 tableId 解析数据集名称和表名称
 */
private String[] resolveDatasetAndTableNames(Long tableId) {
    if (tableId == null) {
        return null;
    }

    // 1. 查询原表信息
    CoreDatasetTable table = coreDatasetTableMapper.selectById(tableId);
    if (table == null) {
        LogUtil.getLogger().warn("resolveDatasetAndTableNames: 未找到 tableId={}", tableId);
        return null;
    }

    // 2. 查询数据集信息
    CoreDatasetGroup group = coreDatasetGroupMapper.selectById(table.getDatasetGroupId());
    if (group == null) {
        LogUtil.getLogger().warn("resolveDatasetAndTableNames: 未找到 datasetGroupId={}", table.getDatasetGroupId());
        return null;
    }

    return new String[]{group.getName(), table.getName()};
}

/**
 * 按名称查找目标表ID
 */
private Long resolveNewTableId(String datasetName, String tableName) {
    if (datasetName == null || tableName == null) {
        return null;
    }

    // 1. 按名称查找数据集
    CoreDatasetGroup targetGroup = coreDatasetGroupMapper.selectOne(
        new LambdaQueryWrapper<CoreDatasetGroup>()
            .eq(CoreDatasetGroup::getName, datasetName)
            .eq(CoreDatasetGroup::getNodeType, "dataset")
    );

    if (targetGroup == null) {
        LogUtil.getLogger().info("resolveNewTableId: 未找到数据集 name={}", datasetName);
        return null;
    }

    // 2. 按数据集ID和表名称查找表
    CoreDatasetTable targetTable = coreDatasetTableMapper.selectOne(
        new LambdaQueryWrapper<CoreDatasetTable>()
            .eq(CoreDatasetTable::getDatasetGroupId, targetGroup.getId())
            .eq(CoreDatasetTable::getName, tableName)
    );

    if (targetTable == null) {
        LogUtil.getLogger().info("resolveNewTableId: 未找到表 datasetGroupId={}, tableName={}", targetGroup.getId(), tableName);
        return null;
    }

    return targetTable.getId();
}

@Override
public ChartImportResult importChartsWithResult(List<BackupChartView> charts,
                                                 Map<String, Long> dashboardIdMapping,
                                                 boolean overwrite) {
    ChartImportResult result = new ChartImportResult();
    result.setImportedCharts(new ArrayList<>());
    result.setMissingDatasets(new ArrayList<>());

    if (charts == null || charts.isEmpty()) {
        LogUtil.getLogger().info("importChartsWithResult: charts 为空");
        return result;
    }

    LogUtil.getLogger().info("importChartsWithResult: 开始导入 {} 个图表, overwrite={}", charts.size(), overwrite);

    for (BackupChartView chart : charts) {
        try {
            Long oldTableId = chart.getTableId();

            // 处理 tableId 为 null 的情况
            if (oldTableId == null) {
                LogUtil.getLogger().warn("importChartsWithResult: 图表 tableId 为 null, title={}", chart.getTitle());
                ChartMissingDataset missing = new ChartMissingDataset();
                missing.setChartId(chart.getId());
                missing.setChartTitle(chart.getTitle());
                missing.setOldTableId(null);
                missing.setDatasetName("(未知)");
                missing.setTableName("(未知)");
                result.getMissingDatasets().add(missing);
                result.setMissingCount(result.getMissingCount() + 1);
                continue;
            }

            // 解析数据集名称和表名称
            String[] names = resolveDatasetAndTableNames(oldTableId);
            if (names == null) {
                ChartMissingDataset missing = new ChartMissingDataset();
                missing.setChartId(chart.getId());
                missing.setChartTitle(chart.getTitle());
                missing.setOldTableId(oldTableId);
                missing.setDatasetName("(解析失败)");
                missing.setTableName("(解析失败)");
                result.getMissingDatasets().add(missing);
                result.setMissingCount(result.getMissingCount() + 1);
                continue;
            }

            String datasetName = names[0];
            String tableName = names[1];

            // 按名称查找目标表ID
            Long newTableId = resolveNewTableId(datasetName, tableName);

            if (newTableId == null) {
                // 未找到同名数据集
                ChartMissingDataset missing = new ChartMissingDataset();
                missing.setChartId(chart.getId());
                missing.setChartTitle(chart.getTitle());
                missing.setOldTableId(oldTableId);
                missing.setDatasetName(datasetName);
                missing.setTableName(tableName);
                result.getMissingDatasets().add(missing);
                result.setMissingCount(result.getMissingCount() + 1);
                LogUtil.getLogger().warn("importChartsWithResult: 未找到数据集 datasetName={}, tableName={}", datasetName, tableName);
                continue;
            }

            // 找到数据集，            Long oldSceneId = chart.getSceneId();
            Long newSceneId = dashboardIdMapping.get(String.valueOf(oldSceneId));
            newSceneId = newSceneId != null ? newSceneId : oldSceneId;

            // 创建图表
            createChartWithNewTableId(chart, newTableId, newSceneId, overwrite, chartIdMapping);

            ChartInfo chartInfo = new ChartInfo();
            chartInfo.setOldId(chart.getId());
            chartInfo.setTitle(chart.getTitle());
            chartInfo.setNewTableId(newTableId);
            result.getImportedCharts().add(chartInfo);
            result.setSuccessCount(result.getSuccessCount() + 1);

        } catch (Exception e) {
            LogUtil.getLogger().error("importChartsWithResult: 处理图表失败 title={}", chart.getTitle(), e);
        }
    }

    LogUtil.getLogger().info("importChartsWithResult: 完成, 成功={}, 缺失={}", result.getSuccessCount(), result.getMissingCount());
    return result;
}
```

### 4. BackupCenterManage.java
调用 `importChartsWithResult` 方法，处理返回结果，将缺失信息加入响应。

### 5. BackupResponse.java
添加字段：
```java
private List<ChartMissingDataset> missingDatasets;
private int missingChartCount;
```

## 测试策略

1. **单元测试:**
   - 测试 `resolveDatasetAndTableNames()` 方法
   - 测试 `resolveNewTableId()` 方法
   - 测试 `importChartsWithResult()` 正常流程
   - 测试 `importChartsWithResult()` 缺失数据集场景
   - 测试 `importChartsWithResult()` tableId 为 null 场景

2. **集成测试:**
   - 导入包含图表的仪表板，数据集存在
   - 导入包含图表的仪表板，数据集不存在
   - 导入包含图表的仪表板，同名数据集在不同目录

## 错误处理
- `resolveDatasetAndTableNames` 返回 null 时记录缺失
- `resolveNewTableId` 返回 null 时记录缺失
- 图表创建失败时记录日志
- 缺失数据集信息通过 `BackupResponse` 返回给前端

## 边缘情况
1. **tableId 为 null**: 记录为缺失，跳过导入
2. **解析失败**: 源数据已不存在，记录为缺失
3. **同名数据集**: 在目标系统中按名称查找，不考虑目录结构
4. **同名表**: 在同一数据集下按表名称查找
