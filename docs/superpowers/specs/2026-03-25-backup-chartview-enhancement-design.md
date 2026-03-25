# 仪表板图表备份增强设计

> **Goal:** 导出仪表板/大屏时，同时导出其引用的图表数据；导入时建立 ID 映射并替换 `componentData` 中的图表 ID

## 背景

当前备份导出仪表板/大屏时，`componentData` JSON 中引用了 `core_chart_view` 表的图表 ID，但这些图表数据本身并未导出。导入到新环境后，图表 ID 无法对应，导致仪表板组件配置失效。

## 数据模型

### core_chart_view 关键字段

| 字段 | 说明 |
|------|------|
| `id` | 图表 ID |
| `title` | 图表标题 |
| `sceneId` | 所属仪表板 ID（`chart_type='private'` 时有效） |
| `tableId` | 关联的数据集表 ID |
| `type` | 图表类型 |
| `render` | 渲染方式 |
| `xAxis` / `yAxis` | 轴配置 |
| `customAttr` | 图形属性 |
| `customStyle` | 组件样式 |
| `chartType` | `public`=公共可复用历史图表, `private`=私有专属某仪表板 |

### 图表引用方式

`componentData` JSON 中通过 `"id":"图表ID"` 格式引用每个图表：
```java
componentData.indexOf("\"id\":\"" + chartId) > 0
```

## 设计方案

### 1. 导出流程

**前置：** 仪表板和大屏先完成导出（已有逻辑）

**图表导出步骤：**
1. 从已导出的仪表板/大屏列表中，提取所有 `id`（String 格式）
2. 查询 `core_chart_view` 表，条件：`sceneId` IN (导出的仪表板ID列表)
3. 将查询到的图表构建为 `BackupChartView` 列表

**关键点：**
- 只导出 `sceneId` 能匹配到已导出仪表板的图表
- `sceneId` 为 null 或不匹配任何导出仪表板的图表不导出
- `tableId` 保持原值，不做转换

### 2. 导入流程

**步骤：**
1. 建立 ID 映射表：
   - 数据集 ID 映射（如果同时导出了数据集）
   - 仪表板 ID 映射（已有）
2. 导入图表：
   - 替换 `sceneId` 为新仪表板 ID（通过仪表板 ID 映射查找）
   - 替换 `tableId` 为新数据集 ID（通过数据集 ID 映射查找）
   - 创建新图表记录
3. 建立图表 ID 映射：旧图表 ID → 新图表 ID
4. 导入仪表板：
   - 替换 `componentData` JSON 中的图表 ID（通过图表 ID 映射）

**注意：`drillFields`（钻取字段）和 `viewFields`（图表字段集合）是 JSON 字段，可能包含嵌套的数据集 ID 引用。这些字段在导入时会整体替换 `tableId`，因此内部引用的数据集 ID 也会相应更新。**

**图表 ID 替换逻辑：**
```java
// componentData 中 "id":"旧图表ID" → "id":"新图表ID"
String newComponentData = componentData;
for (Map.Entry<String, String> entry : chartIdMapping.entrySet()) {
    // 使用精确匹配，只替换 "id":"xxx" 格式的图表ID
    newComponentData = newComponentData.replaceAll(
        "\"id\"\\s*:\\s*\"" + Pattern.quote(entry.getKey()) + "\"",
        "\"id\":\"" + entry.getValue() + "\""
    );
}
```

### 3. 模型变更

#### 新增 BackupChartView

位置：`sdk/common/src/main/java/io/dataease/model/backup/BackupChartView.java`

字段（核心字段）：
- `id` - 原图表 ID
- `title` - 图表标题
- `sceneId` - 场景 ID（原仪表板 ID，导入时替换）
- `tableId` - 数据集表 ID（导入时替换）
- `type` - 图表类型
- `render` - 渲染方式
- `xAxis` / `yAxis` - 轴配置
- `customAttr` - 图形属性
- `customStyle` - 组件样式
- `customFilter` - 结果过滤
- `senior` - 高级配置
- `chartType` - 图表类型
- `dataFrom` - 数据来源
- `viewFields` - 图表字段集合
- `createBy` / `createTime` / `updateTime`

#### ExportPackage 变更

```java
// 新增字段
private List<BackupChartView> charts;
```

#### BackupCenterManage 变更

**导出时：**
```java
// 导出仪表板/大屏后，收集图表
if ("dashboard".equals(request.getType()) || "combined".equals(request.getType())) {
    dashboards = backupDashboardService.exportDashboards();
    folders.addAll(backupDashboardService.collectDashboardFolders());
    // 新增：收集图表
    charts.addAll(backupDashboardService.collectCharts(dashboards));
}
if ("dataview".equals(request.getType()) || "combined".equals(request.getType())) {
    dataviews = backupDashboardService.exportDataviews();
    // 新增：收集图表
    charts.addAll(backupDashboardService.collectCharts(dataviews));
}
```

**导入时：**
```java
// 1. 先导入图表（建立图表 ID 映射）
if (exportPackage.getCharts() != null && !exportPackage.getCharts().isEmpty()) {
    backupDashboardService.importCharts(
        exportPackage.getCharts(),
        dashboardIdMapping,  // 仪表板 ID 映射
        datasetIdMapping,    // 数据集 ID 映射
        chartIdMapping       // 输出：图表 ID 映射
    );
}

// 2. 再导入仪表板（使用图表 ID 映射替换 componentData）
if (exportPackage.getDashboards() != null && !exportPackage.getDashboards().isEmpty()) {
    backupDashboardService.importDashboards(
        exportPackage.getDashboards(),
        folders,
        request.isOverwrite(),
        chartIdMapping  // 新增：传入图表 ID 映射
    );
}
```

### 4. BackupDashboardService 变更

#### 接口新增

```java
// 收集图表数据
List<BackupChartView> collectCharts(List<BackupDashboard> dashboards);

// 导入图表
void importCharts(List<BackupChartView> charts,
                   Map<String, Long> dashboardIdMapping,
                   Map<String, Long> datasetIdMapping,
                   Map<String, Long> chartIdMapping);

// 新增重载方法：现有方法签名保持不变，此方法用于支持图表ID替换
void importDashboards(List<BackupDashboard> dashboards,
                      List<BackupFolder> folders,
                      boolean overwrite,
                      Map<String, Long> chartIdMapping);
```

### 5. BackupChartView 模型

> 注意：`id`、`sceneId`、`tableId` 在 CoreChartView 实体中是 Long 类型，但备份模型中统一用 String（与 BackupDashboard 保持一致），导出/导入时进行类型转换。

```java
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
    private String drillFields;           // 钻取字段（JSON，可能包含嵌套ID引用）
    private String senior;                // 高级配置
    private String createBy;
    private String updateBy;              // 修改人
    private Long createTime;
    private Long updateTime;
    private String snapshot;              // 缩略图
    private String stylePriority;
    private String chartType;            // public=公共, private=私有
    private Boolean isPlugin;
    private String dataFrom;
    private String viewFields;            // 图表字段集合（JSON）
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

## 文件变更清单

| 操作 | 文件 |
|------|------|
| 新增 | `sdk/common/src/main/java/io/dataease/model/backup/BackupChartView.java` |
| 修改 | `sdk/common/src/main/java/io/dataease/model/backup/ExportPackage.java` |
| 修改 | `core/core-backend/src/main/java/io/dataease/backup/service/BackupDashboardService.java` |
| 修改 | `core/core-backend/src/main/java/io/dataease/backup/service/impl/BackupDashboardServiceImpl.java` |
| 修改 | `core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java` |

## 依赖关系

导入顺序：
1. 目录 (folders)
2. 数据源 (datasources)
3. 数据集 (datasets) - 建立 datasetIdMapping
4. 图表 (charts) - 使用 dashboardIdMapping 和 datasetIdMapping
5. 仪表板/大屏 (dashboards/dataviews) - 使用 folderMapping 和 chartIdMapping
