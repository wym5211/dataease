package io.dataease.backup.service;

import io.dataease.model.backup.BackupDataset;

import java.util.List;
import java.util.Map;

public interface BackupDatasetService {
    List<BackupDataset> exportDatasets();
    /**
     * 导入数据集
     * @param dataset 数据集信息
     * @param overwrite 是否覆盖
     * @param idMapping ID映射表（旧ID -> 新ID）
     * @return 新创建的ID（如果已存在并覆盖则返回原有ID）
     */
    String importDataset(BackupDataset dataset, boolean overwrite, Map<String, String> idMapping);
}
