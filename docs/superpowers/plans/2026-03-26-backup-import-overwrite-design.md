# 备份导入覆盖选项实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在导入图表时支持 overwrite 选项：overwrite=true 覆盖已有图表，overwrite=false 重命名后创建

**Architecture:** 在 `BackupDashboardService.importCharts` 方法增加 `overwrite` 参数，在创建图表前检查是否已存在同名图表

**Tech Stack:** Java, Spring Boot, MyBatis

---

## 文件变更概览

| 文件 | 改动 |
|------|------|
| `BackupDashboardService.java` | 接口增加 overwrite 参数 |
| `BackupDashboardServiceImpl.java` | 实现 overwrite 逻辑 |
| `BackupCenterManage.java` | 透传 overwrite 参数给 importCharts |

---

## Task 1: 修改 BackupDashboardService 接口

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/backup/service/BackupDashboardService.java:20`

- [ ] **Step 1: 修改接口签名**

将:
```java
void importCharts(List<BackupChartView> charts, Map<String, Long> dashboardIdMapping, Map<String, Long> datasetIdMapping, Map<String, Long> chartIdMapping);
```

改为:
```java
void importCharts(List<BackupChartView> charts, Map<String, Long> dashboardIdMapping, Map<String, Long> datasetIdMapping, Map<String, Long> chartIdMapping, boolean overwrite);
```

- [ ] **Step 2: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/service/BackupDashboardService.java
git commit -m "feat(backup): add overwrite param to importCharts interface"
```

---

## Task 2: 修改 BackupCenterManage 透传 overwrite

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java:189`

- [ ] **Step 1: 修改 importCharts 调用**

将:
```java
backupDashboardService.importCharts(exportPackage.getCharts(), dashboardIdMapping, datasetIdMapping, chartIdMapping);
```

改为:
```java
backupDashboardService.importCharts(exportPackage.getCharts(), dashboardIdMapping, datasetIdMapping, chartIdMapping, request.isOverwrite());
```

- [ ] **Step 2: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java
git commit -m "feat(backup): pass overwrite flag to importCharts"
```

---

## Task 3: 实现 importCharts overwrite 逻辑

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDashboardServiceImpl.java:615-693`

- [ ] **Step 1: 修改方法签名增加 overwrite 参数**

```java
@Override
public void importCharts(List<BackupChartView> charts,
                         Map<String, Long> dashboardIdMapping,
                         Map<String, Long> datasetIdMapping,
                         Map<String, Long> chartIdMapping,
                         boolean overwrite) {
```

- [ ] **Step 2: 在创建图表前添加查找逻辑**

首先计算 `newSceneId`（因为它用于去重和重名检查），然后在创建 newChart 之前添加查找逻辑：

在 `LogUtil.getLogger().info("importCharts: 创建新图表 title={}", chart.getTitle());` 之前添加:

```java
// 先计算 newSceneId（用于后续去重检查）
Long oldSceneId = chart.getSceneId();
Long newSceneId = dashboardIdMapping.get(String.valueOf(oldSceneId));
newSceneId = newSceneId != null ? newSceneId : oldSceneId;

// 检查是否已存在同名图表（在同一仪表板下）
CoreChartView existingChart = coreChartViewMapper.selectOne(
    new LambdaQueryWrapper<CoreChartView>()
        .eq(CoreChartView::getTitle, chart.getTitle())
        .eq(CoreChartView::getSceneId, newSceneId)
);

if (existingChart != null) {
    if (overwrite) {
        // 覆盖模式：更新已有图表
        LogUtil.getLogger().info("importCharts: 覆盖已有图表 title={}, id={}", chart.getTitle(), existingChart.getId());
        // 更新图表字段
        updateChartFromBackup(existingChart, chart, datasetIdMapping, newSceneId);
        chartIdMapping.put(chart.getId(), existingChart.getId());
        continue;
    } else {
        // 非覆盖模式：重命名后创建
        LogUtil.getLogger().info("importCharts: 重命名图表 title={}", chart.getTitle());
        String newTitle = generateUniqueChartTitle(chart.getTitle(), newSceneId);
        chart.setTitle(newTitle);
    }
}
```

- [ ] **Step 3: 添加 updateChartFromBackup 方法**

在 importCharts 方法后添加私有方法:

```java
private void updateChartFromBackup(CoreChartView existing, BackupChartView backup, Map<String, Long> datasetIdMapping, Long newSceneId) {
    // 设置 sceneId（不更改，保持关联到目标仪表板）
    existing.setSceneId(newSceneId);

    // 替换 tableId
    Long oldTableId = backup.getTableId();
    Long newTableId = datasetIdMapping.get(String.valueOf(oldTableId));
    existing.setTableId(newTableId != null ? newTableId : oldTableId);

    // 更新其他可变更字段
    existing.setType(backup.getType());
    existing.setRender(backup.getRender());
    existing.setResultCount(backup.getResultCount());
    existing.setResultMode(backup.getResultMode());
    existing.setxAxis(backup.getxAxis());
    existing.setxAxisExt(backup.getxAxisExt());
    existing.setyAxis(backup.getyAxis());
    existing.setyAxisExt(backup.getyAxisExt());
    existing.setExtStack(backup.getExtStack());
    existing.setExtBubble(backup.getExtBubble());
    existing.setExtLabel(backup.getExtLabel());
    existing.setExtTooltip(backup.getExtTooltip());
    existing.setCustomAttr(backup.getCustomAttr());
    existing.setCustomStyle(backup.getCustomStyle());
    existing.setCustomFilter(backup.getCustomFilter());
    existing.setDrillFields(backup.getDrillFields());
    existing.setSenior(backup.getSenior());
    existing.setSnapshot(backup.getSnapshot());
    existing.setStylePriority(backup.getStylePriority());
    existing.setChartType(backup.getChartType());
    existing.setDataFrom(backup.getDataFrom());
    existing.setViewFields(backup.getViewFields());
    existing.setRefreshViewEnable(backup.getRefreshViewEnable());
    existing.setRefreshUnit(backup.getRefreshUnit());
    existing.setRefreshTime(backup.getRefreshTime());
    existing.setLinkageActive(backup.getLinkageActive());
    existing.setJumpActive(backup.getJumpActive());
    existing.setCopyFrom(backup.getCopyFrom());
    existing.setCopyId(backup.getCopyId());
    existing.setAggregate(backup.getAggregate());
    existing.setFlowMapStartName(backup.getFlowMapStartName());
    existing.setFlowMapEndName(backup.getFlowMapEndName());
    existing.setExtColor(backup.getExtColor());
    existing.setCustomAttrMobile(backup.getCustomAttrMobile());
    existing.setCustomStyleMobile(backup.getCustomStyleMobile());
    existing.setSortPriority(backup.getSortPriority());
    existing.setUpdateTime(System.currentTimeMillis());

    coreChartViewMapper.updateById(existing);
}
```

- [ ] **Step 4: 添加 generateUniqueChartTitle 方法**

```java
private String generateUniqueChartTitle(String baseTitle, Long sceneId) {
    for (int i = 1; i <= 1000; i++) {
        String newTitle = baseTitle + "_" + i;
        Long count = coreChartViewMapper.selectCount(
            new LambdaQueryWrapper<CoreChartView>()
                .eq(CoreChartView::getTitle, newTitle)
                .eq(CoreChartView::getSceneId, sceneId)
        );
        if (count == 0) {
            return newTitle;
        }
    }
    // Fallback: use timestamp
    return baseTitle + "_" + System.currentTimeMillis();
}
```

- [ ] **Step 5: Commit**

```bash
git add core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDashboardServiceImpl.java
git commit -m "feat(backup): implement overwrite logic in importCharts"
```

---

## Task 4: 验证构建

- [ ] **Step 1: 运行 Maven 构建**

```bash
cd core/core-backend
mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Commit (if successful)**

---

## 注意事项

1. **覆盖模式不会更新图表的 sceneId**：因为 sceneId 是用于关联仪表板的，更新 sceneId 会破坏关联关系
2. **重命名逻辑只在当前仪表板范围内唯一**：不同仪表板下可以存在同名图表
3. **updateChartFromBackup 不更新 chartIdMapping 的主键映射**：覆盖模式下旧ID映射到同一个已有ID（这意味着 chartIdMapping 中旧ID不会产生新的映射条目）
4. **sceneId 重映射提前计算**：在检查重复和创建图表前，先计算 `newSceneId = dashboardIdMapping.get(oldSceneId) ?: oldSceneId`
5. **覆盖模式边界情况**：如果备份中有多个同名图表（相同 title+sceneId），覆盖模式下后一个会覆盖前一个，chartIdMapping 中后一个的映射会覆盖前一个的映射
