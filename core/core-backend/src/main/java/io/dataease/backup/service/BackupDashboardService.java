package io.dataease.backup.service;

import io.dataease.model.backup.BackupDashboard;
import io.dataease.model.backup.BackupDataview;

import java.util.List;

public interface BackupDashboardService {
    List<BackupDashboard> exportDashboards();
    List<BackupDataview> exportDataviews();
    void importDashboard(BackupDashboard dashboard, boolean overwrite);
    void importDataview(BackupDataview dataview, boolean overwrite);
}
