package io.dataease.backup.service;

import io.dataease.model.backup.BackupDataset;
import io.dataease.model.backup.BackupFolder;

import java.util.List;
import java.util.Map;

public interface BackupDatasetService {
    List<BackupDataset> exportDatasets();
    /**
     * 导出指定ID的数据集
     * @param ids 数据集ID列表，为空时导出全部
     * @return 数据集列表
     */
    List<BackupDataset> exportDatasets(List<String> ids);
    /**
     * 收集所有数据集的目录链
     * @return 目录列表
     */
    List<BackupFolder> collectDatasetFolders();
    /**
     * 收集指定数据集的目录链
     * @param ids 数据集ID列表
     * @return 目录列表
     */
    List<BackupFolder> collectDatasetFolders(List<String> ids);
    /**
     * 从图表中提取数据集ID并导出这些数据集
     * @param tableIds 图表的tableId列表
     * @return 数据集列表
     */
    List<BackupDataset> exportDatasetsByTableIds(List<Long> tableIds);
    /**
     * 收集指定数据集的目录链（通过tableId）
     * @param tableIds 图表的tableId列表
     * @return 目录列表
     */
    List<BackupFolder> collectDatasetFoldersByTableIds(List<Long> tableIds);
    /**
     * 从数据集中提取数据源ID
     * @param datasets 数据集列表
     * @return 数据源ID列表（去重）
     */
    List<String> collectDatasourceIds(List<BackupDataset> datasets);
    /**
     * 导入数据集
     * @param dataset 数据集信息
     * @param overwrite 是否覆盖
     * @param idMapping ID映射表（旧ID -> 新ID）
     * @return 新创建的ID（如果已存在并覆盖则返回原有ID）
     */
    String importDataset(BackupDataset dataset, boolean overwrite, Map<String, String> idMapping);
    /**
     * 导入数据集及其关联的物理表
     * @param dataset 数据集信息
     * @param overwrite 是否覆盖
     * @param datasourceIdMapping 数据源ID映射表（旧ID -> 新ID）
     * @return 新创建的ID（如果已存在并覆盖则返回原有ID）
     */
    String importDatasetWithTables(BackupDataset dataset, boolean overwrite, Map<String, String> datasourceIdMapping);
    /**
     * 导入数据集列表（包含目录创建）
     * @param datasets 数据集列表
     * @param folders 目录列表
     * @param overwrite 是否覆盖
     * @param idMapping ID映射表（用于存储数据集ID映射）
     * @param datasourceIdMapping 数据源ID映射表（旧数据源ID -> 新数据源ID）
     */
    void importDatasets(List<BackupDataset> datasets, List<BackupFolder> folders, boolean overwrite, Map<String, String> idMapping, Map<String, String> datasourceIdMapping);
}
