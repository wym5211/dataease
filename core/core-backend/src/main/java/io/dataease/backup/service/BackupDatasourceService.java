package io.dataease.backup.service;

import io.dataease.model.backup.BackupDatasource;

import java.util.List;

public interface BackupDatasourceService {
    List<BackupDatasource> exportDatasources();
    void importDatasource(BackupDatasource datasource, boolean overwrite);
}
