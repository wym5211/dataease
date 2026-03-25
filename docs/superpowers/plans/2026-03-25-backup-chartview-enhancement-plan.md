# 仪表板图表备份增强实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 导出仪表板/大屏时，同时导出其引用的图表数据；导入时建立 ID 映射并替换 componentData 中的图表 ID

**Architecture:** 在现有备份系统中新增图表备份功能，通过 sceneId 关联仪表板与图表，导入时建立 ID 映射表进行替换

**Tech Stack:** Java, MyBatis-Plus, Spring Boot

---

## 文件结构

| 操作 | 文件 |
|------|------|
| 新增 | `sdk/common/src/main/java/io/dataease/model/backup/BackupChartView.java` |
| 修改 | `sdk/common/src/main/java/io/dataease/model/backup/ExportPackage.java` |
| 修改 | `core/core-backend/src/main/java/io/dataease/backup/service/BackupDashboardService.java` |
| 修改 | `core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDashboardServiceImpl.java` |
| 修改 | `core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java` |

---

## Task 1: 创建 BackupChartView 模型类

**Files:**
- Create: `sdk/common/src/main/java/io/dataease/model/backup/BackupChartView.java`

- [ ] **Step 1: 创建 BackupChartView.java**

```java
package io.dataease.model.backup;

import lombok.Data;

/**
 * 图表备份信息
 */
@Data
public class BackupChartView {
    private String id;                    // 原 ID (Long -> String)
    private String title;                 // 标题
    private Long sceneId;                 // 场景ID（导入时替换为新仪表板ID）
    private Long tableId;                 // 数据集表ID（导入时替换为新数据集ID）
    private String type;                  // 图表类型
    private String render;                // 渲染方式
    private Integer resultCount;
    private String resultMode;
    private String xAxis;
    private String xAxisExt;
    private String yAxis;
    private String yAxisExt;
    private String extStack;
    private String extBubble;
    private String extLabel;
    private String extTooltip;
    private String customAttr;
    private String customStyle;
    private String customFilter;
    private String drillFields;
    private String senior;
    private String createBy;
    private Long createTime;
    private Long updateTime;
    private String snapshot;
    private String stylePriority;
    private String chartType;
    private Boolean isPlugin;
    private String dataFrom;
    private String viewFields;
    private Boolean refreshViewEnable;
    private String refreshUnit;
    private Integer refreshTime;
    private Boolean linkageActive;
    private Boolean jumpActive;
    private Long copyFrom;
    private Long copyId;
    private Boolean aggregate;
    private String flowMapStartName;
    private String flowMapEndName;
    private String extColor;
    private String customAttrMobile;
    private String customStyleMobile;
    private String sortPriority;
}
```

- [ ] **Step 2: Commit**

```bash
git add sdk/common/src/main/java/io/dataease/model/backup/BackupChartView.java
git commit -m "feat(backup): add BackupChartView model for chart backup"
```

---

## Task 2: 修改 ExportPackage - 添加 charts 字段

**Files:**
- Modify: `sdk/common/src/main/java/io/dataease/model/backup/ExportPackage.java:51-56`

- [ ] **Step 1: 在 ExportPackage.java 中添加 charts 字段**

在 `dataviews` 字段后、`folders` 字段前添加：

```java
/**
 * 图表列表
 */
private List<BackupChartView> charts;

/**
 * 目录列表
 */
private List<BackupFolder> folders;
```

- [ ] **Step 2: Commit**

```bash
git add sdk/common/src/main/java/io/dataease/model/backup/ExportPackage.java
git commit -m "feat(backup): add charts field to ExportPackage"
```

---

## Task 3: 修改 BackupDashboardService 接口

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/backup/service/BackupDashboardService.java`

- [ ] **Step 1: 添加 collectCharts、importCharts 和 importDashboards 重载方法签名**

在接口中添加：

```java
import io.dataease.model.backup.BackupChartView;

List<BackupChartView> collectCharts(List<BackupDashboard> dashboards);

void importCharts(List<BackupChartView> charts,
                  Map<String, Long> dashboardIdMapping,
                  Map<String, Long> datasetIdMapping,
                  Map<String, Long> chartIdMapping);

// 新增重载方法：支持图表ID映射替换 componentData
void importDashboards(List<BackupDashboard> dashboards,
                      List<BackupFolder> folders,
                      boolean overwrite,
                      Map<String, Long> chartIdMapping);
```

- [ ] **Step 2: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/service/BackupDashboardService.java
git commit -m "feat(backup): add collectCharts and importCharts to BackupDashboardService"
```

---

## Task 4: 实现 BackupDashboardServiceImpl 图表导出/导入逻辑

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDashboardServiceImpl.java`

需要完成的实现：
1. `collectCharts(List<BackupDashboard> dashboards)` - 收集仪表板关联的图表
2. `importCharts(...)` - 导入图表并填充 chartIdMapping
3. `importDashboards(..., Map<String, Long> chartIdMapping)` - 重载方法，替换 componentData 中的图表 ID

- [ ] **Step 1: 添加 CoreChartViewMapper 依赖**

在类中添加：

```java
@Autowired
private CoreChartViewMapper coreChartViewMapper;
```

需要 import：
```java
import io.dataease.chart.dao.auto.entity.CoreChartView;
import io.dataease.chart.dao.auto.mapper.CoreChartViewMapper;
import io.dataease.model.backup.BackupChartView;
```

- [ ] **Step 2: 实现 collectCharts 方法**

在类末尾添加：

```java
@Override
public List<BackupChartView> collectCharts(List<BackupDashboard> dashboards) {
    List<BackupChartView> result = new ArrayList<>();
    if (dashboards == null || dashboards.isEmpty()) {
        return result;
    }
    try {
        // 提取所有仪表板 ID
        List<Long> dashboardIds = dashboards.stream()
            .map(d -> Long.parseLong(d.getId()))
            .toList();

        // 查询 sceneId 匹配的图表
        QueryWrapper<CoreChartView> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("scene_id", dashboardIds);
        List<CoreChartView> charts = coreChartViewMapper.selectList(queryWrapper);

        for (CoreChartView chart : charts) {
            BackupChartView backup = new BackupChartView();
            backup.setId(String.valueOf(chart.getId()));
            backup.setTitle(chart.getTitle());
            backup.setSceneId(chart.getSceneId());
            backup.setTableId(chart.getTableId());
            backup.setType(chart.getType());
            backup.setRender(chart.getRender());
            backup.setResultCount(chart.getResultCount());
            backup.setResultMode(chart.getResultMode());
            backup.setxAxis(chart.getxAxis());
            backup.setxAxisExt(chart.getxAxisExt());
            backup.setyAxis(chart.getyAxis());
            backup.setyAxisExt(chart.getyAxisExt());
            backup.setExtStack(chart.getExtStack());
            backup.setExtBubble(chart.getExtBubble());
            backup.setExtLabel(chart.getExtLabel());
            backup.setExtTooltip(chart.getExtTooltip());
            backup.setCustomAttr(chart.getCustomAttr());
            backup.setCustomStyle(chart.getCustomStyle());
            backup.setCustomFilter(chart.getCustomFilter());
            backup.setDrillFields(chart.getDrillFields());
            backup.setSenior(chart.getSenior());
            backup.setCreateBy(chart.getCreateBy());
            backup.setCreateTime(chart.getCreateTime());
            backup.setUpdateTime(chart.getUpdateTime());
            backup.setSnapshot(chart.getSnapshot());
            backup.setStylePriority(chart.getStylePriority());
            backup.setChartType(chart.getChartType());
            backup.setIsPlugin(chart.getIsPlugin());
            backup.setDataFrom(chart.getDataFrom());
            backup.setViewFields(chart.getViewFields());
            backup.setRefreshViewEnable(chart.getRefreshViewEnable());
            backup.setRefreshUnit(chart.getRefreshUnit());
            backup.setRefreshTime(chart.getRefreshTime());
            backup.setLinkageActive(chart.getLinkageActive());
            backup.setJumpActive(chart.getJumpActive());
            backup.setCopyFrom(chart.getCopyFrom());
            backup.setCopyId(chart.getCopyId());
            backup.setAggregate(chart.getAggregate());
            backup.setFlowMapStartName(chart.getFlowMapStartName());
            backup.setFlowMapEndName(chart.getFlowMapEndName());
            backup.setExtColor(chart.getExtColor());
            backup.setCustomAttrMobile(chart.getCustomAttrMobile());
            backup.setCustomStyleMobile(chart.getCustomStyleMobile());
            backup.setSortPriority(chart.getSortPriority());
            result.add(backup);
        }
    } catch (Exception e) {
        LogUtil.getLogger().error("Collect charts failed", e);
    }
    return result;
}
```

- [ ] **Step 3: 实现 importCharts 方法**

在类末尾添加：

```java
@Override
public void importCharts(List<BackupChartView> charts,
                         Map<String, Long> dashboardIdMapping,
                         Map<String, Long> datasetIdMapping,
                         Map<String, Long> chartIdMapping) {
    if (charts == null || charts.isEmpty()) {
        return;
    }
    for (BackupChartView chart : charts) {
        try {
            // 查找是否已存在同名图表
            QueryWrapper<CoreChartView> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("title", chart.getTitle());
            CoreChartView existing = coreChartViewMapper.selectOne(queryWrapper);

            Long newId;
            if (existing != null) {
                newId = existing.getId();
            } else {
                // 创建新图表
                CoreChartView newChart = new CoreChartView();
                newChart.setTitle(chart.getTitle());
                // 替换 sceneId
                Long oldSceneId = chart.getSceneId();
                Long newSceneId = dashboardIdMapping.get(String.valueOf(oldSceneId));
                newChart.setSceneId(newSceneId != null ? newSceneId : oldSceneId);
                // 替换 tableId
                Long oldTableId = chart.getTableId();
                Long newTableId = datasetIdMapping.get(String.valueOf(oldTableId));
                newChart.setTableId(newTableId != null ? newTableId : oldTableId);
                // 设置其他字段
                newChart.setType(chart.getType());
                newChart.setRender(chart.getRender());
                newChart.setResultCount(chart.getResultCount());
                newChart.setResultMode(chart.getResultMode());
                newChart.setxAxis(chart.getxAxis());
                newChart.setxAxisExt(chart.getxAxisExt());
                newChart.setyAxis(chart.getyAxis());
                newChart.setyAxisExt(chart.getyAxisExt());
                newChart.setExtStack(chart.getExtStack());
                newChart.setExtBubble(chart.getExtBubble());
                newChart.setExtLabel(chart.getExtLabel());
                newChart.setExtTooltip(chart.getExtTooltip());
                newChart.setCustomAttr(chart.getCustomAttr());
                newChart.setCustomStyle(chart.getCustomStyle());
                newChart.setCustomFilter(chart.getCustomFilter());
                newChart.setDrillFields(chart.getDrillFields());
                newChart.setSenior(chart.getSenior());
                newChart.setCreateBy("1");
                newChart.setCreateTime(System.currentTimeMillis());
                newChart.setUpdateTime(System.currentTimeMillis());
                newChart.setSnapshot(chart.getSnapshot());
                newChart.setStylePriority(chart.getStylePriority());
                newChart.setChartType(chart.getChartType());
                newChart.setIsPlugin(chart.getIsPlugin());
                newChart.setDataFrom(chart.getDataFrom());
                newChart.setViewFields(chart.getViewFields());
                newChart.setRefreshViewEnable(chart.getRefreshViewEnable());
                newChart.setRefreshUnit(chart.getRefreshUnit());
                newChart.setRefreshTime(chart.getRefreshTime());
                newChart.setLinkageActive(chart.getLinkageActive());
                newChart.setJumpActive(chart.getJumpActive());
                newChart.setCopyFrom(chart.getCopyFrom());
                newChart.setCopyId(chart.getCopyId());
                newChart.setAggregate(chart.getAggregate());
                newChart.setFlowMapStartName(chart.getFlowMapStartName());
                newChart.setFlowMapEndName(chart.getFlowMapEndName());
                newChart.setExtColor(chart.getExtColor());
                newChart.setCustomAttrMobile(chart.getCustomAttrMobile());
                newChart.setCustomStyleMobile(chart.getCustomStyleMobile());
                newChart.setSortPriority(chart.getSortPriority());

                coreChartViewMapper.insert(newChart);
                newId = newChart.getId();
            }

            // 记录 ID 映射
            chartIdMapping.put(chart.getId(), newId);
        } catch (Exception e) {
            LogUtil.getLogger().error("Import chart failed: " + chart.getTitle(), e);
        }
    }
}
```

- [ ] **Step 4: 实现 importDashboards 重载方法**

在类末尾添加（接收 chartIdMapping 参数，用于替换 componentData 中的图表 ID）：

```java
@Override
public void importDashboards(List<BackupDashboard> dashboards,
                            List<BackupFolder> folders,
                            boolean overwrite,
                            Map<String, Long> chartIdMapping) {
    // 使用现有逻辑导入仪表板，但 componentData 中的图表 ID 需要替换
    for (BackupDashboard dashboard : dashboards) {
        try {
            // 替换 componentData 中的图表 ID
            String componentData = dashboard.getComponentData();
            if (componentData != null && chartIdMapping != null && !chartIdMapping.isEmpty()) {
                for (Map.Entry<String, Long> entry : chartIdMapping.entrySet()) {
                    componentData = componentData.replaceAll(
                        "\"id\"\\s*:\\s*\"" + Pattern.quote(entry.getKey()) + "\"",
                        "\"id\":\"" + entry.getValue() + "\""
                    );
                }
                dashboard.setComponentData(componentData);
            }

            // 调用原有导入逻辑
            importDashboard(dashboard, overwrite);
        } catch (Exception e) {
            LogUtil.getLogger().error("Import dashboard with chart mapping failed: " + dashboard.getName(), e);
        }
    }
}
```

注意：需要添加 `import java.util.regex.Pattern;`

- [ ] **Step 5: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDashboardServiceImpl.java
git commit -m "feat(backup): implement collectCharts, importCharts and importDashboards overload"
```

---

## Task 5: 修改 BackupCenterManage 集成图表导出/导入

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java`

需要完成的修改：
1. 添加 `charts` 列表初始化
2. 导出时调用 `collectCharts` 收集图表
3. 导入时先导入图表，再导入仪表板

- [ ] **Step 1: 添加 charts 列表初始化**

在 `export()` 方法中，第 51-53 行附近添加：

```java
List<BackupChartView> charts = new ArrayList<>();
```

- [ ] **Step 2: 导出时收集图表**

在 `export()` 方法中，仪表板和大屏导出后添加图表收集：

```java
if ("dashboard".equals(request.getType()) || "combined".equals(request.getType())) {
    dashboards = backupDashboardService.exportDashboards();
    folders.addAll(backupDashboardService.collectDashboardFolders());
    charts.addAll(backupDashboardService.collectCharts(dashboards));
}
if ("dataview".equals(request.getType()) || "combined".equals(request.getType())) {
    dataviews = backupDashboardService.exportDataviews();
    charts.addAll(backupDashboardService.collectCharts(dataviews));
}
```

然后在 `exportPackage.setFolders(folders);` 后添加：
```java
exportPackage.setCharts(charts);
```

- [ ] **Step 3: 导入时处理图表**

在 `importData()` 方法中，导入仪表板之前添加图表导入：

```java
// 导入图表（建立图表 ID 映射）
Map<String, Long> chartIdMapping = new HashMap<>();
if (exportPackage.getCharts() != null && !exportPackage.getCharts().isEmpty()) {
    // dashboardIdMapping 和 datasetIdMapping 在各自导入后已建立
    // 此处传入的是当前已有的映射（从 datasets 和 dashboards 导入中获取）
    Map<String, Long> dashboardIdMapping = new HashMap<>();
    Map<String, Long> datasetIdMapping = new HashMap<>();

    // 从 idMapping 中提取已有的映射（如果有）
    if (exportPackage.getIdMapping() != null) {
        if (exportPackage.getIdMapping().getDatasetIds() != null) {
            for (ExportPackage.IdPair pair : exportPackage.getIdMapping().getDatasetIds()) {
                datasetIdMapping.put(pair.getOldId(), Long.parseLong(pair.getNewId()));
            }
        }
        if (exportPackage.getIdMapping().getDashboardIds() != null) {
            for (ExportPackage.IdPair pair : exportPackage.getIdMapping().getDashboardIds()) {
                dashboardIdMapping.put(pair.getOldId(), Long.parseLong(pair.getNewId()));
            }
        }
    }

    backupDashboardService.importCharts(
        exportPackage.getCharts(),
        dashboardIdMapping,
        datasetIdMapping,
        chartIdMapping
    );
}

// 导入仪表板/大屏（使用图表 ID 映射替换 componentData 中的图表 ID）
// 调用四参数重载方法
if (exportPackage.getDashboards() != null && !exportPackage.getDashboards().isEmpty()) {
    backupDashboardService.importDashboards(
        exportPackage.getDashboards(),
        folders,
        request.isOverwrite(),
        chartIdMapping
    );
}
```

- [ ] **Step 4: 添加必要的 import**

```java
import io.dataease.model.backup.BackupChartView;
import java.util.regex.Pattern;
```

- [ ] **Step 5: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java
git commit -m "feat(backup): integrate chart export/import in BackupCenterManage"
```

---

## Task 6: 验证编译

- [ ] **Step 1: 运行 Maven 编译**

```bash
cd core/core-backend
mvn compile -DskipTests
```

预期：无编译错误

- [ ] **Step 2: 如有错误，修复后重新编译**
