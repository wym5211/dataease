package io.dataease.backup.service.impl;

import io.dataease.backup.service.BackupDashboardService;
import io.dataease.model.backup.BackupDashboard;
import io.dataease.model.backup.BackupDataview;
import io.dataease.utils.LogUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BackupDashboardServiceImpl implements BackupDashboardService {

    @Override
    public List<BackupDashboard> exportDashboards() {
        List<BackupDashboard> result = new ArrayList<>();
        try {
            // TODO: Implement dashboard export when panel module is available
            // For now, return empty list
        } catch (Exception e) {
            LogUtil.getLogger().error("Export dashboards failed", e);
        }
        return result;
    }

    @Override
    public List<BackupDataview> exportDataviews() {
        List<BackupDataview> result = new ArrayList<>();
        try {
            // TODO: Implement dataview export when panel module is available
            // For now, return empty list
        } catch (Exception e) {
            LogUtil.getLogger().error("Export dataviews failed", e);
        }
        return result;
    }

    @Override
    public void importDashboard(BackupDashboard dashboard, boolean overwrite) {
        try {
            // TODO: Implement dashboard import when panel module is available
        } catch (Exception e) {
            LogUtil.getLogger().error("Import dashboard failed: " + dashboard.getName(), e);
            throw e;
        }
    }

    @Override
    public void importDataview(BackupDataview dataview, boolean overwrite) {
        try {
            // TODO: Implement dataview import when panel module is available
        } catch (Exception e) {
            LogUtil.getLogger().error("Import dataview failed: " + dataview.getName(), e);
            throw e;
        }
    }
}
