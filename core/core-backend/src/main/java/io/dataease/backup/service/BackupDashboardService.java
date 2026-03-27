package io.dataease.backup.service;

import io.dataease.model.backup.BackupChartView;
import io.dataease.model.backup.BackupDashboard;
import io.dataease.model.backup.BackupDataview;
import io.dataease.model.backup.BackupFolder;
import io.dataease.model.backup.ChartImportResult;

import java.util.List;
import java.util.Map;

public interface BackupDashboardService {
    List<BackupDashboard> exportDashboards();
    /**
     * 导出指定ID的仪表板
     * @param ids 仪表板ID列表，为空时导出全部
     * @return 仪表板列表
     */
    List<BackupDashboard> exportDashboards(List<String> ids);
    List<BackupDataview> exportDataviews();
    /**
     * 导出指定ID的数据大屏
     * @param ids 大屏ID列表，为空时导出全部
     * @return 大屏列表
     */
    List<BackupDataview> exportDataviews(List<String> ids);
    List<BackupFolder> collectDashboardFolders();
    /**
     * 收集指定仪表板/大屏的目录链
     * @param ids 仪表板/大屏ID列表
     * @return 目录列表
     */
    List<BackupFolder> collectDashboardFolders(List<String> ids);
    List<BackupChartView> collectCharts(List<BackupDashboard> dashboards);
    List<BackupChartView> collectChartsFromDataviews(List<BackupDataview> dataviews);
    void importDashboards(List<BackupDashboard> dashboards, List<BackupFolder> folders, boolean overwrite);
    void importDashboards(List<BackupDashboard> dashboards, List<BackupFolder> folders, boolean overwrite, Map<String, Long> chartIdMapping);
    void importDashboards(List<BackupDashboard> dashboards, List<BackupFolder> folders, boolean overwrite, Map<String, Long> chartIdMapping, Map<String, Long> dashboardIdMapping);
    void importCharts(List<BackupChartView> charts, Map<String, Long> dashboardIdMapping, Map<String, Long> datasetIdMapping, Map<String, Long> chartIdMapping, boolean overwrite);
    void importDashboard(BackupDashboard dashboard, boolean overwrite);
    void importDataview(BackupDataview dataview, boolean overwrite);
    void importDataview(BackupDataview dataview, boolean overwrite, Map<String, Long> chartIdMapping);
    void importDataview(BackupDataview dataview, boolean overwrite, Map<String, Long> chartIdMapping, Map<String, Long> dataviewIdMapping);
    void updateChartsSceneIds(List<BackupChartView> charts, Map<String, Long> dashboardIdMapping, Map<String, Long> chartIdMapping);
    Map<String, Long> buildDashboardIdMapping(List<BackupDashboard> dashboards);
    Map<String, Long> buildDataviewIdMapping(List<BackupDataview> dataviews);
    /**
     * 从图表列表中提取所有 tableId
     * @param charts 图表列表
     * @return tableId 列表（去重）
     */
    List<Long> collectTableIds(List<BackupChartView> charts);
    /**
     * 导入图表并返回结果（按数据集名称匹配）
     * @param charts 图表列表
     * @param dashboardIdMapping 仪表板ID映射
     * @param overwrite 是否覆盖
     * @return 导入结果（包含成功和缺失信息）
     */
    ChartImportResult importChartsWithResult(List<BackupChartView> charts, Map<String, Long> dashboardIdMapping, boolean overwrite);
}
