package io.dataease.backup.service;

import io.dataease.model.backup.BackupDatasource;
import io.dataease.model.backup.BackupFolder;

import java.util.List;
import java.util.Map;

public interface BackupDatasourceService {
    List<BackupDatasource> exportDatasources();
    /**
     * 收集数据源的目录链
     * @return 目录列表
     */
    List<BackupFolder> collectDatasourceFolders();
    /**
     * 导入数据源
     * @param datasource 数据源信息
     * @param overwrite 是否覆盖
     * @return 新创建的ID（如果已存在并覆盖则返回原有ID）
     */
    String importDatasource(BackupDatasource datasource, boolean overwrite);
    /**
     * 导入数据源列表（包含目录创建）
     * @param datasources 数据源列表
     * @param folders 目录列表
     * @param overwrite 是否覆盖
     * @param idMapping ID映射表
     */
    void importDatasources(List<BackupDatasource> datasources, List<BackupFolder> folders, boolean overwrite, Map<String, String> idMapping);
}
