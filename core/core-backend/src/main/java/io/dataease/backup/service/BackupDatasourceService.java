package io.dataease.backup.service;

import io.dataease.model.backup.BackupDatasource;

import java.util.List;

public interface BackupDatasourceService {
    List<BackupDatasource> exportDatasources();
    /**
     * 导入数据源
     * @param datasource 数据源信息
     * @param overwrite 是否覆盖
     * @return 新创建的ID（如果已存在并覆盖则返回原有ID）
     */
    String importDatasource(BackupDatasource datasource, boolean overwrite);
}
