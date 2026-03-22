package io.dataease.backup.service;

import io.dataease.model.backup.BackupDataset;

import java.util.List;

public interface BackupDatasetService {
    List<BackupDataset> exportDatasets();
    void importDataset(BackupDataset dataset, boolean overwrite);
}
