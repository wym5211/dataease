package io.dataease.backup.service;

import io.dataease.model.backup.BackupChartView;
import io.dataease.model.backup.BackupDashboard;
import io.dataease.model.backup.BackupDataview;
import io.dataease.model.backup.BackupFolder;

import java.util.List;
import java.util.Map;

public interface BackupDashboardService {
    List<BackupDashboard> exportDashboards();
    List<BackupDataview> exportDataviews();
    List<BackupFolder> collectDashboardFolders();
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
}
