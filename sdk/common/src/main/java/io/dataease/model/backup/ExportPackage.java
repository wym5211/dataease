package io.dataease.model.backup;

import lombok.Data;

import java.util.List;

/**
 * 导出包结构 - 用于打包导出的小工具资源
 */
@Data
public class ExportPackage {

    /**
     * 导出包版本
     */
    private String version = "2.0";

    /**
     * 导出类型: datasource, dataset, dashboard, dataview, combined
     */
    private String type;

    /**
     * 导出时间
     */
    private Long exportTime;

    /**
     * 导出人ID
     */
    private Long exportBy;

    /**
     * 数据源列表
     */
    private List<BackupDatasource> datasources;

    /**
     * 数据集列表
     */
    private List<BackupDataset> datasets;

    /**
     * 仪表板列表
     */
    private List<BackupDashboard> dashboards;

    /**
     * 大屏列表
     */
    private List<BackupDataview> dataviews;

    /**
     * 图表列表
     */
    private List<BackupChartView> charts;

    /**
     * 目录列表
     */
    private List<BackupFolder> folders;

    /**
     * ID映射关系 (旧ID -> 新ID)
     */
    private IdMapping idMapping;

    @Data
    public static class IdMapping {
        /**
         * 数据源ID映射
         */
        private List<IdPair> datasourceIds;

        /**
         * 数据集ID映射
         */
        private List<IdPair> datasetIds;

        /**
         * 仪表板ID映射
         */
        private List<IdPair> dashboardIds;

        /**
         * 大屏ID映射
         */
        private List<IdPair> dataviewIds;
    }

    @Data
    public static class IdPair {
        private String oldId;
        private String newId;
    }
}
