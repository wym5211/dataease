package io.dataease.backup.service;

import io.dataease.model.backup.BackupDataset;
import io.dataease.model.backup.BackupFolder;

import java.util.List;
import java.util.Map;

public interface BackupDatasetService {
    List<BackupDataset> exportDatasets();
    /**
     * 收集所有数据集的目录链
     * @return 目录列表
     */
    List<BackupFolder> collectDatasetFolders();
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
     * @param idMapping ID映射表
     */
    void importDatasets(List<BackupDataset> datasets, List<BackupFolder> folders, boolean overwrite, Map<String, String> idMapping);
}
