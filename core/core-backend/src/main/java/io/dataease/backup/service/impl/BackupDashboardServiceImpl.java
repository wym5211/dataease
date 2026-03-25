package io.dataease.backup.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.dataease.backup.service.BackupDashboardService;
import io.dataease.model.backup.BackupDashboard;
import io.dataease.model.backup.BackupDataview;
import io.dataease.model.backup.BackupFolder;
import io.dataease.utils.LogUtil;
import io.dataease.chart.dao.auto.entity.CoreChartView;
import io.dataease.chart.dao.auto.mapper.CoreChartViewMapper;
import io.dataease.model.backup.BackupChartView;
import io.dataease.visualization.dao.auto.entity.DataVisualizationInfo;
import io.dataease.visualization.dao.auto.mapper.DataVisualizationInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

import java.util.*;

@Service
public class BackupDashboardServiceImpl implements BackupDashboardService {

    @Autowired
    private DataVisualizationInfoMapper dataVisualizationInfoMapper;

    @Autowired
    private CoreChartViewMapper coreChartViewMapper;

    @Override
    public List<BackupDashboard> exportDashboards() {
        List<BackupDashboard> result = new ArrayList<>();
        try {
            QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type", "dashboard");
            List<DataVisualizationInfo> dashboards = dataVisualizationInfoMapper.selectList(queryWrapper);

            for (DataVisualizationInfo dashboard : dashboards) {
                BackupDashboard backup = new BackupDashboard();
                backup.setId(String.valueOf(dashboard.getId()));
                backup.setName(dashboard.getName());
                backup.setPid(dashboard.getPid());
                backup.setLevel(dashboard.getLevel());
                backup.setNodeType(dashboard.getNodeType());
                backup.setCreateBy(dashboard.getCreateBy());
                backup.setCreateTime(dashboard.getCreateTime());
                backup.setUpdateTime(dashboard.getUpdateTime());
                backup.setOrgId(dashboard.getOrgId());
                backup.setCanvasStyleData(dashboard.getCanvasStyleData());
                backup.setComponentData(dashboard.getComponentData());
                backup.setContentId(dashboard.getContentId());
                result.add(backup);
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Export dashboards failed", e);
        }
        return result;
    }

    /**
     * Collect all dashboard folders and build folder chain for exported dashboards
     */
    public List<BackupFolder> collectDashboardFolders() {
        List<BackupFolder> result = new ArrayList<>();
        Map<String, BackupFolder> folderMap = new HashMap<>();
        try {
            // Query all dashboards to collect their parent folders
            QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type", "dashboard").or().eq("type", "dataV");
            List<DataVisualizationInfo> allDashboards = dataVisualizationInfoMapper.selectList(queryWrapper);

            for (DataVisualizationInfo dashboard : allDashboards) {
                if (dashboard.getPid() != null && dashboard.getPid() != 0L) {
                    result.addAll(collectDashboardParentFolders(dashboard.getId(), folderMap, 1, "dashboard".equals(dashboard.getType()) ? "dashboard" : "dataV"));
                }
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Collect dashboard folders failed", e);
        }
        return result;
    }

    /**
     * Recursively collect parent folders for a dashboard
     */
    private List<BackupFolder> collectDashboardParentFolders(Long dashboardId, Map<String, BackupFolder> folderMap, int currentLevel, String subType) {
        List<BackupFolder> result = new ArrayList<>();
        DataVisualizationInfo dashboard = dataVisualizationInfoMapper.selectById(dashboardId);
        if (dashboard == null || dashboard.getPid() == null || dashboard.getPid() == 0L) {
            return result;
        }
        DataVisualizationInfo parent = dataVisualizationInfoMapper.selectById(dashboard.getPid());
        if (parent == null) {
            return result;
        }
        if ("folder".equals(parent.getNodeType())) {
            String key = parent.getName() + "_" + (parent.getPid() != null && parent.getPid() != 0L ?
                dataVisualizationInfoMapper.selectById(parent.getPid()).getName() : "root");
            if (folderMap.containsKey(key)) {
                return result;
            }
            BackupFolder folder = new BackupFolder();
            folder.setId(String.valueOf(parent.getId()));
            folder.setName(parent.getName());
            folder.setPid(parent.getPid());
            folder.setLevel(currentLevel);
            folder.setNodeType("folder");
            folder.setResourceType("dashboard");
            folder.setSubType(subType);
            // 设置父目录名称用于跨环境匹配
            if (parent.getPid() != null && parent.getPid() != 0L) {
                DataVisualizationInfo grandParent = dataVisualizationInfoMapper.selectById(parent.getPid());
                if (grandParent != null) {
                    folder.setParentName(grandParent.getName());
                }
            }
            folderMap.put(key, folder);
            result.add(folder);
            result.addAll(collectDashboardParentFolders(parent.getId(), folderMap, currentLevel + 1, subType));
        }
        return result;
    }

    @Override
    public List<BackupDataview> exportDataviews() {
        List<BackupDataview> result = new ArrayList<>();
        try {
            QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type", "dataV");
            List<DataVisualizationInfo> dataviews = dataVisualizationInfoMapper.selectList(queryWrapper);

            for (DataVisualizationInfo dataview : dataviews) {
                BackupDataview backup = new BackupDataview();
                backup.setId(String.valueOf(dataview.getId()));
                backup.setName(dataview.getName());
                backup.setPid(dataview.getPid());
                backup.setLevel(dataview.getLevel());
                backup.setNodeType(dataview.getNodeType());
                backup.setCreateBy(dataview.getCreateBy());
                backup.setCreateTime(dataview.getCreateTime());
                backup.setUpdateTime(dataview.getUpdateTime());
                backup.setOrgId(dataview.getOrgId());
                backup.setCanvasStyleData(dataview.getCanvasStyleData());
                backup.setComponentData(dataview.getComponentData());
                backup.setContentId(dataview.getContentId());
                result.add(backup);
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Export dataviews failed", e);
        }
        return result;
    }

    @Override
    public void importDashboard(BackupDashboard dashboard, boolean overwrite) {
        try {
            QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", dashboard.getName());
            DataVisualizationInfo existing = dataVisualizationInfoMapper.selectOne(queryWrapper);

            if (existing != null && overwrite) {
                existing.setPid(dashboard.getPid());
                existing.setLevel(dashboard.getLevel());
                dataVisualizationInfoMapper.updateById(existing);
            } else {
                String newName = dashboard.getName();
                if (existing != null) {
                    newName = generateUniqueName(dashboard.getName());
                }
                DataVisualizationInfo newDashboard = new DataVisualizationInfo();
                newDashboard.setName(newName);
                newDashboard.setPid(dashboard.getPid() != null ? dashboard.getPid() : 0L);
                newDashboard.setLevel(dashboard.getLevel() != null ? dashboard.getLevel() : 0);
                newDashboard.setNodeType(dashboard.getNodeType() != null ? dashboard.getNodeType() : "dashboard");
                newDashboard.setType("dashboard");
                newDashboard.setCreateBy("1");
                newDashboard.setCreateTime(System.currentTimeMillis());
                newDashboard.setUpdateTime(System.currentTimeMillis());
                dataVisualizationInfoMapper.insert(newDashboard);
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Import dashboard failed: " + dashboard.getName(), e);
            throw e;
        }
    }

    /**
     * Import dashboards with folder support
     */
    public void importDashboards(List<BackupDashboard> dashboards, List<BackupFolder> folders, boolean overwrite) {
        if (dashboards == null || dashboards.isEmpty()) {
            return;
        }

        // 按level排序，先创建低级目录
        Map<String, Long> folderMapping = new HashMap<>();
        if (folders != null && !folders.isEmpty()) {
            List<BackupFolder> sortedFolders = folders.stream()
                .sorted(Comparator.comparingInt(f -> f.getLevel() != null ? f.getLevel() : 0))
                .toList();

            for (BackupFolder folder : sortedFolders) {
                createOrFindDashboardFolder(folder, folderMapping);
            }
        }

        // 导入仪表板
        for (BackupDashboard dashboard : dashboards) {
            try {
                // 计算新pid：使用文件夹映射将原pid转换为新pid
                Long newPid = 0L;
                if (dashboard.getPid() != null && dashboard.getPid() != 0L) {
                    newPid = folderMapping.getOrDefault("id_" + dashboard.getPid(), 0L);
                }

                importDashboardWithPid(dashboard, overwrite, newPid);
            } catch (Exception e) {
                LogUtil.getLogger().error("Import dashboard failed: " + dashboard.getName(), e);
            }
        }
    }

    private void importDashboardWithPid(BackupDashboard dashboard, boolean overwrite, Long newPid) {
        QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", dashboard.getName());
        DataVisualizationInfo existing = dataVisualizationInfoMapper.selectOne(queryWrapper);

        if (existing != null && overwrite) {
            existing.setPid(newPid);
            existing.setLevel(dashboard.getLevel());
            existing.setOrgId(dashboard.getOrgId());
            existing.setCanvasStyleData(dashboard.getCanvasStyleData());
            existing.setComponentData(dashboard.getComponentData());
            existing.setContentId(dashboard.getContentId());
            dataVisualizationInfoMapper.updateById(existing);
        } else {
            String newName = dashboard.getName();
            if (existing != null) {
                newName = generateUniqueName(dashboard.getName());
            }
            DataVisualizationInfo newDashboard = new DataVisualizationInfo();
            newDashboard.setName(newName);
            newDashboard.setPid(newPid);
            newDashboard.setLevel(dashboard.getLevel() != null ? dashboard.getLevel() : 0);
            newDashboard.setNodeType("dashboard");
            newDashboard.setType("dashboard");
            newDashboard.setOrgId(dashboard.getOrgId());
            newDashboard.setCanvasStyleData(dashboard.getCanvasStyleData());
            newDashboard.setComponentData(dashboard.getComponentData());
            newDashboard.setContentId(dashboard.getContentId());
            newDashboard.setCreateBy("1");
            newDashboard.setCreateTime(System.currentTimeMillis());
            newDashboard.setUpdateTime(System.currentTimeMillis());
            dataVisualizationInfoMapper.insert(newDashboard);
        }
    }

    /**
     * 按 name + parentId 查找目录
     */
    private DataVisualizationInfo findDashboardFolderByNameAndParent(String name, Long parentId) {
        QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name).eq("node_type", "folder");
        if (parentId != null && parentId != 0L) {
            queryWrapper.eq("pid", parentId);
        } else {
            queryWrapper.and(w -> w.eq("pid", 0L).or().isNull("pid"));
        }
        return dataVisualizationInfoMapper.selectOne(queryWrapper);
    }

    /**
     * 创建或查找目录，返回目录ID
     */
    public Long createOrFindDashboardFolder(BackupFolder folder, Map<String, Long> folderMapping) {
        String key = folder.getName() + "_" + (folder.getParentName() != null ? folder.getParentName() : "root");
        if (folderMapping.containsKey(key)) {
            return folderMapping.get(key);
        }

        // 先查找父目录ID
        Long parentId = 0L;
        if (folder.getParentName() != null) {
            String parentKey = folder.getParentName() + "_root";
            parentId = folderMapping.getOrDefault(parentKey, 0L);
        }

        // 查找是否已存在
        DataVisualizationInfo existing = findDashboardFolderByNameAndParent(folder.getName(), parentId);
        if (existing != null) {
            folderMapping.put(key, existing.getId());
            if (folder.getId() != null) {
                folderMapping.put("id_" + folder.getId(), existing.getId());
            }
            return existing.getId();
        }

        // 创建新目录
        DataVisualizationInfo newFolder = new DataVisualizationInfo();
        newFolder.setName(folder.getName());
        newFolder.setPid(parentId);
        newFolder.setLevel(folder.getLevel() != null ? folder.getLevel() : 0);
        newFolder.setNodeType("folder");
        newFolder.setCreateBy("1");
        newFolder.setCreateTime(System.currentTimeMillis());
        newFolder.setUpdateTime(System.currentTimeMillis());
        dataVisualizationInfoMapper.insert(newFolder);

        // 重新查询获取ID
        newFolder = dataVisualizationInfoMapper.selectOne(new QueryWrapper<DataVisualizationInfo>()
            .eq("name", folder.getName())
            .eq("node_type", "folder")
            .eq("pid", parentId));

        folderMapping.put(key, newFolder.getId());
        if (folder.getId() != null) {
            folderMapping.put("id_" + folder.getId(), newFolder.getId());
        }
        return newFolder.getId();
    }

    private String generateUniqueName(String baseName) {
        for (int i = 1; i <= 1000; i++) {
            String newName = baseName + "_" + i;
            QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", newName);
            if (dataVisualizationInfoMapper.selectCount(queryWrapper) == 0) {
                return newName;
            }
        }
        return baseName + "_" + System.currentTimeMillis();
    }

    @Override
    public void importDataview(BackupDataview dataview, boolean overwrite) {
        importDataview(dataview, overwrite, null);
    }

    @Override
    public void importDataview(BackupDataview dataview, boolean overwrite, Map<String, Long> chartIdMapping) {
        try {
            // 替换 componentData 中的图表 ID
            String componentData = dataview.getComponentData();
            if (componentData != null && chartIdMapping != null && !chartIdMapping.isEmpty()) {
                for (Map.Entry<String, Long> entry : chartIdMapping.entrySet()) {
                    componentData = componentData.replaceAll(
                        "\"id\"\\s*:\\s*\"" + Pattern.quote(entry.getKey()) + "\"",
                        "\"id\":\"" + entry.getValue() + "\""
                    );
                }
            }

            QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", dataview.getName());
            DataVisualizationInfo existing = dataVisualizationInfoMapper.selectOne(queryWrapper);

            if (existing != null && overwrite) {
                existing.setPid(dataview.getPid());
                existing.setLevel(dataview.getLevel());
                existing.setOrgId(dataview.getOrgId());
                existing.setCanvasStyleData(dataview.getCanvasStyleData());
                existing.setComponentData(componentData);
                existing.setContentId(dataview.getContentId());
                dataVisualizationInfoMapper.updateById(existing);
            } else {
                String newName = dataview.getName();
                if (existing != null) {
                    newName = generateUniqueName(dataview.getName());
                }
                DataVisualizationInfo newDataview = new DataVisualizationInfo();
                newDataview.setName(newName);
                newDataview.setPid(dataview.getPid() != null ? dataview.getPid() : 0L);
                newDataview.setLevel(dataview.getLevel() != null ? dataview.getLevel() : 0);
                newDataview.setNodeType(dataview.getNodeType() != null ? dataview.getNodeType() : "dashboard");
                newDataview.setType("dataV");
                newDataview.setOrgId(dataview.getOrgId());
                newDataview.setCanvasStyleData(dataview.getCanvasStyleData());
                newDataview.setComponentData(componentData);
                newDataview.setContentId(dataview.getContentId());
                newDataview.setCreateBy("1");
                newDataview.setCreateTime(System.currentTimeMillis());
                newDataview.setUpdateTime(System.currentTimeMillis());
                dataVisualizationInfoMapper.insert(newDataview);
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Import dataview failed: " + dataview.getName(), e);
            throw e;
        }
    }

    @Override
    public void importDataview(BackupDataview dataview, boolean overwrite, Map<String, Long> chartIdMapping, Map<String, Long> dataviewIdMapping) {
        try {
            // 替换 componentData 中的图表 ID
            String componentData = dataview.getComponentData();
            if (componentData != null && chartIdMapping != null && !chartIdMapping.isEmpty()) {
                for (Map.Entry<String, Long> entry : chartIdMapping.entrySet()) {
                    componentData = componentData.replaceAll(
                        "\"id\"\\s*:\\s*\"" + Pattern.quote(entry.getKey()) + "\"",
                        "\"id\":\"" + entry.getValue() + "\""
                    );
                }
            }

            QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", dataview.getName());
            DataVisualizationInfo existing = dataVisualizationInfoMapper.selectOne(queryWrapper);

            if (existing != null && overwrite) {
                existing.setPid(dataview.getPid());
                existing.setLevel(dataview.getLevel());
                existing.setOrgId(dataview.getOrgId());
                existing.setCanvasStyleData(dataview.getCanvasStyleData());
                existing.setComponentData(componentData);
                existing.setContentId(dataview.getContentId());
                dataVisualizationInfoMapper.updateById(existing);
                // 记录 ID 映射：原 ID -> 原 ID（因为是覆盖）
                dataviewIdMapping.put(dataview.getId(), existing.getId());
                LogUtil.getLogger().info("importDataview (覆盖): {} -> {}", dataview.getId(), existing.getId());
            } else {
                String newName = dataview.getName();
                if (existing != null) {
                    newName = generateUniqueName(dataview.getName());
                }
                DataVisualizationInfo newDataview = new DataVisualizationInfo();
                newDataview.setName(newName);
                newDataview.setPid(dataview.getPid() != null ? dataview.getPid() : 0L);
                newDataview.setLevel(dataview.getLevel() != null ? dataview.getLevel() : 0);
                newDataview.setNodeType(dataview.getNodeType() != null ? dataview.getNodeType() : "dashboard");
                newDataview.setType("dataV");
                newDataview.setOrgId(dataview.getOrgId());
                newDataview.setCanvasStyleData(dataview.getCanvasStyleData());
                newDataview.setComponentData(componentData);
                newDataview.setContentId(dataview.getContentId());
                newDataview.setCreateBy("1");
                newDataview.setCreateTime(System.currentTimeMillis());
                newDataview.setUpdateTime(System.currentTimeMillis());
                dataVisualizationInfoMapper.insert(newDataview);
                // 记录 ID 映射：原 ID -> 新 ID
                dataviewIdMapping.put(dataview.getId(), newDataview.getId());
                LogUtil.getLogger().info("importDataview (新建): {} -> {}", dataview.getId(), newDataview.getId());
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Import dataview failed: " + dataview.getName(), e);
            throw e;
        }
    }

    @Override
    public List<BackupChartView> collectCharts(List<BackupDashboard> dashboards) {
        List<BackupChartView> result = new ArrayList<>();
        if (dashboards == null || dashboards.isEmpty()) {
            LogUtil.getLogger().info("collectCharts: dashboards 为空");
            return result;
        }
        try {
            // 从所有仪表板的 componentData 中提取图表 ID
            Set<Long> chartIds = new HashSet<>();
            for (BackupDashboard dashboard : dashboards) {
                String componentData = dashboard.getComponentData();
                LogUtil.getLogger().info("处理仪表板: {}, componentData 长度: {}", dashboard.getName(), componentData != null ? componentData.length() : 0);
                if (componentData != null && !componentData.isEmpty()) {
                    extractChartIdsFromComponentData(componentData, chartIds);
                }
            }

            LogUtil.getLogger().info("collectCharts: 共提取到 {} 个图表 ID", chartIds.size());

            if (chartIds.isEmpty()) {
                return result;
            }

            // 根据 ID 批量查询图表
            List<CoreChartView> charts = coreChartViewMapper.selectBatchIds(chartIds);
            LogUtil.getLogger().info("collectCharts: 查询到 {} 个图表", charts.size());

            for (CoreChartView chart : charts) {
                BackupChartView backup = new BackupChartView();
                backup.setId(String.valueOf(chart.getId()));
                backup.setTitle(chart.getTitle());
                backup.setSceneId(chart.getSceneId());
                backup.setTableId(chart.getTableId());
                backup.setType(chart.getType());
                backup.setRender(chart.getRender());
                backup.setResultCount(chart.getResultCount());
                backup.setResultMode(chart.getResultMode());
                backup.setxAxis(chart.getxAxis());
                backup.setxAxisExt(chart.getxAxisExt());
                backup.setyAxis(chart.getyAxis());
                backup.setyAxisExt(chart.getyAxisExt());
                backup.setExtStack(chart.getExtStack());
                backup.setExtBubble(chart.getExtBubble());
                backup.setExtLabel(chart.getExtLabel());
                backup.setExtTooltip(chart.getExtTooltip());
                backup.setCustomAttr(chart.getCustomAttr());
                backup.setCustomStyle(chart.getCustomStyle());
                backup.setCustomFilter(chart.getCustomFilter());
                backup.setDrillFields(chart.getDrillFields());
                backup.setSenior(chart.getSenior());
                backup.setCreateBy(chart.getCreateBy());
                backup.setCreateTime(chart.getCreateTime());
                backup.setUpdateTime(chart.getUpdateTime());
                backup.setSnapshot(chart.getSnapshot());
                backup.setStylePriority(chart.getStylePriority());
                backup.setChartType(chart.getChartType());
                backup.setIsPlugin(chart.getIsPlugin());
                backup.setDataFrom(chart.getDataFrom());
                backup.setViewFields(chart.getViewFields());
                backup.setRefreshViewEnable(chart.getRefreshViewEnable());
                backup.setRefreshUnit(chart.getRefreshUnit());
                backup.setRefreshTime(chart.getRefreshTime());
                backup.setLinkageActive(chart.getLinkageActive());
                backup.setJumpActive(chart.getJumpActive());
                backup.setCopyFrom(chart.getCopyFrom());
                backup.setCopyId(chart.getCopyId());
                backup.setAggregate(chart.getAggregate());
                backup.setFlowMapStartName(chart.getFlowMapStartName());
                backup.setFlowMapEndName(chart.getFlowMapEndName());
                backup.setExtColor(chart.getExtColor());
                backup.setCustomAttrMobile(chart.getCustomAttrMobile());
                backup.setCustomStyleMobile(chart.getCustomStyleMobile());
                backup.setSortPriority(chart.getSortPriority());
                result.add(backup);
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Collect charts failed", e);
        }
        return result;
    }

    @Override
    public List<BackupChartView> collectChartsFromDataviews(List<BackupDataview> dataviews) {
        List<BackupChartView> result = new ArrayList<>();
        if (dataviews == null || dataviews.isEmpty()) {
            LogUtil.getLogger().info("collectChartsFromDataviews: dataviews 为空");
            return result;
        }
        try {
            // 从所有大屏的 componentData 中提取图表 ID
            Set<Long> chartIds = new HashSet<>();
            for (BackupDataview dataview : dataviews) {
                String componentData = dataview.getComponentData();
                LogUtil.getLogger().info("处理大屏: {}, componentData 长度: {}", dataview.getName(), componentData != null ? componentData.length() : 0);
                if (componentData != null && !componentData.isEmpty()) {
                    // 从 componentData 中解析图表 ID
                    extractChartIdsFromComponentData(componentData, chartIds);
                }
            }

            LogUtil.getLogger().info("collectChartsFromDataviews: 共提取到 {} 个图表 ID", chartIds.size());

            if (chartIds.isEmpty()) {
                return result;
            }

            // 根据 ID 批量查询图表
            List<CoreChartView> charts = coreChartViewMapper.selectBatchIds(chartIds);
            LogUtil.getLogger().info("collectChartsFromDataviews: 查询到 {} 个图表", charts.size());

            for (CoreChartView chart : charts) {
                BackupChartView backup = new BackupChartView();
                backup.setId(String.valueOf(chart.getId()));
                backup.setTitle(chart.getTitle());
                backup.setSceneId(chart.getSceneId());
                backup.setTableId(chart.getTableId());
                backup.setType(chart.getType());
                backup.setRender(chart.getRender());
                backup.setResultCount(chart.getResultCount());
                backup.setResultMode(chart.getResultMode());
                backup.setxAxis(chart.getxAxis());
                backup.setxAxisExt(chart.getxAxisExt());
                backup.setyAxis(chart.getyAxis());
                backup.setyAxisExt(chart.getyAxisExt());
                backup.setExtStack(chart.getExtStack());
                backup.setExtBubble(chart.getExtBubble());
                backup.setExtLabel(chart.getExtLabel());
                backup.setExtTooltip(chart.getExtTooltip());
                backup.setCustomAttr(chart.getCustomAttr());
                backup.setCustomStyle(chart.getCustomStyle());
                backup.setCustomFilter(chart.getCustomFilter());
                backup.setDrillFields(chart.getDrillFields());
                backup.setSenior(chart.getSenior());
                backup.setCreateBy(chart.getCreateBy());
                backup.setCreateTime(chart.getCreateTime());
                backup.setUpdateTime(chart.getUpdateTime());
                backup.setSnapshot(chart.getSnapshot());
                backup.setStylePriority(chart.getStylePriority());
                backup.setChartType(chart.getChartType());
                backup.setIsPlugin(chart.getIsPlugin());
                backup.setDataFrom(chart.getDataFrom());
                backup.setViewFields(chart.getViewFields());
                backup.setRefreshViewEnable(chart.getRefreshViewEnable());
                backup.setRefreshUnit(chart.getRefreshUnit());
                backup.setRefreshTime(chart.getRefreshTime());
                backup.setLinkageActive(chart.getLinkageActive());
                backup.setJumpActive(chart.getJumpActive());
                backup.setCopyFrom(chart.getCopyFrom());
                backup.setCopyId(chart.getCopyId());
                backup.setAggregate(chart.getAggregate());
                backup.setFlowMapStartName(chart.getFlowMapStartName());
                backup.setFlowMapEndName(chart.getFlowMapEndName());
                backup.setExtColor(chart.getExtColor());
                backup.setCustomAttrMobile(chart.getCustomAttrMobile());
                backup.setCustomStyleMobile(chart.getCustomStyleMobile());
                backup.setSortPriority(chart.getSortPriority());
                result.add(backup);
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Collect charts from dataviews failed", e);
        }
        return result;
    }

    @Override
    public void importCharts(List<BackupChartView> charts,
                             Map<String, Long> dashboardIdMapping,
                             Map<String, Long> datasetIdMapping,
                             Map<String, Long> chartIdMapping) {
        if (charts == null || charts.isEmpty()) {
            LogUtil.getLogger().info("importCharts: charts 为空");
            return;
        }
        LogUtil.getLogger().info("importCharts: 开始导入 {} 个图表", charts.size());
        for (BackupChartView chart : charts) {
            try {
                LogUtil.getLogger().info("importCharts: 处理图表 title={}, id={}, sceneId={}", chart.getTitle(), chart.getId(), chart.getSceneId());
                // 不检查原ID是否存在，直接创建新图表
                LogUtil.getLogger().info("importCharts: 创建新图表 title={}", chart.getTitle());
                CoreChartView newChart = new CoreChartView();
                newChart.setTitle(chart.getTitle());
                // 替换 sceneId
                Long oldSceneId = chart.getSceneId();
                Long newSceneId = dashboardIdMapping.get(String.valueOf(oldSceneId));
                newChart.setSceneId(newSceneId != null ? newSceneId : oldSceneId);
                // 替换 tableId
                Long oldTableId = chart.getTableId();
                Long newTableId = datasetIdMapping.get(String.valueOf(oldTableId));
                newChart.setTableId(newTableId != null ? newTableId : oldTableId);
                // 设置其他字段
                newChart.setType(chart.getType());
                newChart.setRender(chart.getRender());
                newChart.setResultCount(chart.getResultCount());
                newChart.setResultMode(chart.getResultMode());
                newChart.setxAxis(chart.getxAxis());
                newChart.setxAxisExt(chart.getxAxisExt());
                newChart.setyAxis(chart.getyAxis());
                newChart.setyAxisExt(chart.getyAxisExt());
                newChart.setExtStack(chart.getExtStack());
                newChart.setExtBubble(chart.getExtBubble());
                newChart.setExtLabel(chart.getExtLabel());
                newChart.setExtTooltip(chart.getExtTooltip());
                newChart.setCustomAttr(chart.getCustomAttr());
                newChart.setCustomStyle(chart.getCustomStyle());
                newChart.setCustomFilter(chart.getCustomFilter());
                newChart.setDrillFields(chart.getDrillFields());
                newChart.setSenior(chart.getSenior());
                newChart.setCreateBy("1");
                newChart.setCreateTime(System.currentTimeMillis());
                newChart.setUpdateTime(System.currentTimeMillis());
                newChart.setSnapshot(chart.getSnapshot());
                newChart.setStylePriority(chart.getStylePriority());
                newChart.setChartType(chart.getChartType());
                newChart.setIsPlugin(chart.getIsPlugin());
                newChart.setDataFrom(chart.getDataFrom());
                newChart.setViewFields(chart.getViewFields());
                newChart.setRefreshViewEnable(chart.getRefreshViewEnable());
                newChart.setRefreshUnit(chart.getRefreshUnit());
                newChart.setRefreshTime(chart.getRefreshTime());
                newChart.setLinkageActive(chart.getLinkageActive());
                newChart.setJumpActive(chart.getJumpActive());
                newChart.setCopyFrom(chart.getCopyFrom());
                newChart.setCopyId(chart.getCopyId());
                newChart.setAggregate(chart.getAggregate());
                newChart.setFlowMapStartName(chart.getFlowMapStartName());
                newChart.setFlowMapEndName(chart.getFlowMapEndName());
                newChart.setExtColor(chart.getExtColor());
                newChart.setCustomAttrMobile(chart.getCustomAttrMobile());
                newChart.setCustomStyleMobile(chart.getCustomStyleMobile());
                newChart.setSortPriority(chart.getSortPriority());

                coreChartViewMapper.insert(newChart);
                Long newId = newChart.getId();
                LogUtil.getLogger().info("importCharts: 图表插入成功 newId={}", newId);

                // 记录 ID 映射
                chartIdMapping.put(chart.getId(), newId);
                LogUtil.getLogger().info("importCharts: chartIdMapping 添加 {} -> {}", chart.getId(), newId);
            } catch (Exception e) {
                LogUtil.getLogger().error("Import chart failed: " + chart.getTitle(), e);
            }
        }
        LogUtil.getLogger().info("importCharts: 完成, chartIdMapping 大小={}", chartIdMapping.size());
    }

    @Override
    public void importDashboards(List<BackupDashboard> dashboards,
                                List<BackupFolder> folders,
                                boolean overwrite,
                                Map<String, Long> chartIdMapping) {
        // 使用现有逻辑导入仪表板，但 componentData 中的图表 ID 需要替换
        for (BackupDashboard dashboard : dashboards) {
            try {
                // 替换 componentData 中的图表 ID
                String componentData = dashboard.getComponentData();
                if (componentData != null && chartIdMapping != null && !chartIdMapping.isEmpty()) {
                    for (Map.Entry<String, Long> entry : chartIdMapping.entrySet()) {
                        componentData = componentData.replaceAll(
                            "\"id\"\\s*:\\s*\"" + Pattern.quote(entry.getKey()) + "\"",
                            "\"id\":\"" + entry.getValue() + "\""
                        );
                    }
                    dashboard.setComponentData(componentData);
                }

                // 调用原有导入逻辑
                importDashboard(dashboard, overwrite);
            } catch (Exception e) {
                LogUtil.getLogger().error("Import dashboard with chart mapping failed: " + dashboard.getName(), e);
            }
        }
    }

    @Override
    public void importDashboards(List<BackupDashboard> dashboards,
                                List<BackupFolder> folders,
                                boolean overwrite,
                                Map<String, Long> chartIdMapping,
                                Map<String, Long> dashboardIdMapping) {
        // 导入仪表板，并记录 ID 映射
        for (BackupDashboard dashboard : dashboards) {
            try {
                // 替换 componentData 中的图表 ID
                String componentData = dashboard.getComponentData();
                if (componentData != null && chartIdMapping != null && !chartIdMapping.isEmpty()) {
                    for (Map.Entry<String, Long> entry : chartIdMapping.entrySet()) {
                        componentData = componentData.replaceAll(
                            "\"id\"\\s*:\\s*\"" + Pattern.quote(entry.getKey()) + "\"",
                            "\"id\":\"" + entry.getValue() + "\""
                        );
                    }
                    dashboard.setComponentData(componentData);
                }

                // 执行导入，获取新 ID
                String originalId = dashboard.getId();
                String newId = importDashboardWithMapping(dashboard, overwrite);
                dashboardIdMapping.put(originalId, Long.parseLong(newId));
                LogUtil.getLogger().info("importDashboards (新): {} -> {}", originalId, newId);
            } catch (Exception e) {
                LogUtil.getLogger().error("Import dashboard with mapping failed: " + dashboard.getName(), e);
            }
        }
    }

    /**
     * 导入仪表板并返回新 ID
     */
    private String importDashboardWithMapping(BackupDashboard dashboard, boolean overwrite) {
        QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", dashboard.getName());
        DataVisualizationInfo existing = dataVisualizationInfoMapper.selectOne(queryWrapper);

        if (existing != null && overwrite) {
            existing.setPid(dashboard.getPid());
            existing.setLevel(dashboard.getLevel());
            existing.setNodeType(dashboard.getNodeType() != null ? dashboard.getNodeType() : "dashboard");
            existing.setType("dashboard");
            existing.setOrgId(dashboard.getOrgId());
            existing.setCanvasStyleData(dashboard.getCanvasStyleData());
            existing.setComponentData(dashboard.getComponentData());
            existing.setContentId(dashboard.getContentId());
            dataVisualizationInfoMapper.updateById(existing);
            LogUtil.getLogger().info("importDashboard (覆盖): id={}", existing.getId());
            return String.valueOf(existing.getId());
        } else {
            String newName = dashboard.getName();
            if (existing != null) {
                newName = generateUniqueName(dashboard.getName());
            }
            DataVisualizationInfo newDashboard = new DataVisualizationInfo();
            newDashboard.setName(newName);
            newDashboard.setPid(dashboard.getPid() != null ? dashboard.getPid() : 0L);
            newDashboard.setLevel(dashboard.getLevel() != null ? dashboard.getLevel() : 0);
            newDashboard.setNodeType(dashboard.getNodeType() != null ? dashboard.getNodeType() : "dashboard");
            newDashboard.setType("dashboard");
            newDashboard.setOrgId(dashboard.getOrgId());
            newDashboard.setCanvasStyleData(dashboard.getCanvasStyleData());
            newDashboard.setComponentData(dashboard.getComponentData());
            newDashboard.setContentId(dashboard.getContentId());
            newDashboard.setCreateBy("1");
            newDashboard.setCreateTime(System.currentTimeMillis());
            newDashboard.setUpdateTime(System.currentTimeMillis());
            dataVisualizationInfoMapper.insert(newDashboard);
            LogUtil.getLogger().info("importDashboard (新建): id={}", newDashboard.getId());
            return String.valueOf(newDashboard.getId());
        }
    }

    /**
     * 从 componentData JSON 中提取图表 ID
     * componentData 中的图表 ID 格式为 "id":"7442426199994798080"
     */
    private void extractChartIdsFromComponentData(String componentData, Set<Long> chartIds) {
        try {
            // 匹配 "id":"数字" 的模式
            Pattern pattern = Pattern.compile("\"id\"\\s*:\\s*\"(\\d+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(componentData);
            while (matcher.find()) {
                String idStr = matcher.group(1);
                try {
                    Long chartId = Long.parseLong(idStr);
                    chartIds.add(chartId);
                    LogUtil.getLogger().info("提取到图表 ID: {}", chartId);
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Extract chart IDs from componentData failed", e);
        }
    }

    @Override
    public void updateChartsSceneIds(List<BackupChartView> charts, Map<String, Long> dashboardIdMapping, Map<String, Long> chartIdMapping) {
        if (charts == null || charts.isEmpty() || chartIdMapping == null || chartIdMapping.isEmpty()) {
            return;
        }
        LogUtil.getLogger().info("updateChartsSceneIds: 开始更新 {} 个图表的 sceneId", charts.size());
        for (BackupChartView chart : charts) {
            try {
                Long newChartId = chartIdMapping.get(chart.getId());
                if (newChartId == null) {
                    continue;
                }
                Long oldSceneId = chart.getSceneId();
                Long newSceneId = dashboardIdMapping != null ? dashboardIdMapping.get(String.valueOf(oldSceneId)) : null;
                if (newSceneId == null) {
                    LogUtil.getLogger().warn("updateChartsSceneIds: 找不到图表 {} 的新仪表板 ID, 原 sceneId={}", chart.getId(), oldSceneId);
                    continue;
                }
                CoreChartView updateChart = new CoreChartView();
                updateChart.setId(newChartId);
                updateChart.setSceneId(newSceneId);
                coreChartViewMapper.updateById(updateChart);
                LogUtil.getLogger().info("updateChartsSceneIds: 更新图表 {} 的 sceneId: {} -> {}", newChartId, oldSceneId, newSceneId);
            } catch (Exception e) {
                LogUtil.getLogger().error("updateChartsSceneIds failed for chart: " + chart.getId(), e);
            }
        }
        LogUtil.getLogger().info("updateChartsSceneIds: 完成");
    }

    @Override
    public Map<String, Long> buildDashboardIdMapping(List<BackupDashboard> dashboards) {
        Map<String, Long> mapping = new HashMap<>();
        if (dashboards == null || dashboards.isEmpty()) {
            return mapping;
        }
        LogUtil.getLogger().info("buildDashboardIdMapping: 开始构建仪表板 ID 映射, 仪表板数量: {}", dashboards.size());
        for (BackupDashboard dashboard : dashboards) {
            try {
                QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("name", dashboard.getName());
                DataVisualizationInfo existing = dataVisualizationInfoMapper.selectOne(queryWrapper);
                if (existing != null) {
                    mapping.put(dashboard.getId(), existing.getId());
                    LogUtil.getLogger().info("buildDashboardIdMapping: 仪表板 {} -> {}: {} -> {}", dashboard.getName(), existing.getId(), dashboard.getId(), existing.getId());
                } else {
                    LogUtil.getLogger().warn("buildDashboardIdMapping: 找不到仪表板: {}", dashboard.getName());
                }
            } catch (Exception e) {
                LogUtil.getLogger().error("buildDashboardIdMapping failed for dashboard: " + dashboard.getName(), e);
            }
        }
        LogUtil.getLogger().info("buildDashboardIdMapping: 完成, 映射大小: {}", mapping.size());
        return mapping;
    }

    @Override
    public Map<String, Long> buildDataviewIdMapping(List<BackupDataview> dataviews) {
        Map<String, Long> mapping = new HashMap<>();
        if (dataviews == null || dataviews.isEmpty()) {
            return mapping;
        }
        LogUtil.getLogger().info("buildDataviewIdMapping: 开始构建大屏 ID 映射, 大屏数量: {}", dataviews.size());
        for (BackupDataview dataview : dataviews) {
            try {
                QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("name", dataview.getName());
                DataVisualizationInfo existing = dataVisualizationInfoMapper.selectOne(queryWrapper);
                if (existing != null) {
                    mapping.put(dataview.getId(), existing.getId());
                    LogUtil.getLogger().info("buildDataviewIdMapping: 大屏 {} -> {}: {} -> {}", dataview.getName(), existing.getId(), dataview.getId(), existing.getId());
                } else {
                    LogUtil.getLogger().warn("buildDataviewIdMapping: 找不到大屏: {}", dataview.getName());
                }
            } catch (Exception e) {
                LogUtil.getLogger().error("buildDataviewIdMapping failed for dataview: " + dataview.getName(), e);
            }
        }
        LogUtil.getLogger().info("buildDataviewIdMapping: 完成, 映射大小: {}", mapping.size());
        return mapping;
    }
}
