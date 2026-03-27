package io.dataease.model.backup;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 图表导入结果
 */
@Data
public class ChartImportResult {
    /**
     * 成功导入的图表
     */
    private List<ChartInfo> importedCharts = new ArrayList<>();

    /**
     * 缺失数据集的图表
     */
    private List<ChartMissingDataset> missingDatasets = new ArrayList<>();

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
        private Long newChartId;
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
