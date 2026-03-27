package io.dataease.backup.manage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dataease.backup.service.BackupDatasourceService;
import io.dataease.backup.service.BackupDatasetService;
import io.dataease.backup.service.BackupDashboardService;
import io.dataease.commons.UUIDUtils;
import io.dataease.model.backup.*;
import io.dataease.utils.LogUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Component
public class BackupCenterManage {

    @Value("${dataease.backup.path:./backup-data}")
    private String backupPath;

    @Autowired
    private BackupDatasourceService backupDatasourceService;

    @Autowired
    private BackupDatasetService backupDatasetService;

    @Autowired
    private BackupDashboardService backupDashboardService;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private Map<String, ExportPackage> exportPackages = new HashMap<>();

    public BackupResponse export(BackupRequest request) {
        BackupResponse response = new BackupResponse();
        try {
            String exportId = UUIDUtils.getUUID();
            ExportPackage exportPackage = new ExportPackage();
            exportPackage.setVersion("1.0");
            exportPackage.setExportTime(System.currentTimeMillis());
            exportPackage.setType(request.getType());

            List<String> resourceIds = request.getResourceIds();
            boolean hasResourceIds = resourceIds != null && !resourceIds.isEmpty();

            List<BackupDatasource> datasources = new ArrayList<>();
            List<BackupDataset> datasets = new ArrayList<>();
            List<BackupDashboard> dashboards = new ArrayList<>();
            List<BackupDataview> dataviews = new ArrayList<>();
            List<BackupFolder> folders = new ArrayList<>();
            List<BackupChartView> charts = new ArrayList<>();

            if ("datasource".equals(request.getType()) || "combined".equals(request.getType())) {
                if (hasResourceIds && "datasource".equals(request.getType())) {
                    datasources = backupDatasourceService.exportDatasources(resourceIds);
                    folders.addAll(backupDatasourceService.collectDatasourceFolders(resourceIds));
                } else {
                    datasources = backupDatasourceService.exportDatasources();
                    folders.addAll(backupDatasourceService.collectDatasourceFolders());
                }
            }
            if ("dataset".equals(request.getType()) || "combined".equals(request.getType())) {
                if (hasResourceIds && "dataset".equals(request.getType())) {
                    datasets = backupDatasetService.exportDatasets(resourceIds);
                    folders.addAll(backupDatasetService.collectDatasetFolders(resourceIds));
                } else {
                    datasets = backupDatasetService.exportDatasets();
                    folders.addAll(backupDatasetService.collectDatasetFolders());
                }
            }
            if ("dashboard".equals(request.getType()) || "combined".equals(request.getType())) {
                if (hasResourceIds && "dashboard".equals(request.getType())) {
                    dashboards = backupDashboardService.exportDashboards(resourceIds);
                    folders.addAll(backupDashboardService.collectDashboardFolders(resourceIds));
                    charts.addAll(backupDashboardService.collectCharts(dashboards));
                } else {
                    dashboards = backupDashboardService.exportDashboards();
                    folders.addAll(backupDashboardService.collectDashboardFolders());
                    charts.addAll(backupDashboardService.collectCharts(dashboards));
                }
            }
            if ("dataview".equals(request.getType()) || "combined".equals(request.getType())) {
                if (hasResourceIds && "dataview".equals(request.getType())) {
                    dataviews = backupDashboardService.exportDataviews(resourceIds);
                    charts.addAll(backupDashboardService.collectChartsFromDataviews(dataviews));
                } else {
                    dataviews = backupDashboardService.exportDataviews();
                    charts.addAll(backupDashboardService.collectChartsFromDataviews(dataviews));
                }
            }

            // 当导出仪表板/大屏时，自动导出关联的数据集和数据源（如果还没有导出）
            if (!charts.isEmpty() && datasets.isEmpty()) {
                List<Long> tableIds = backupDashboardService.collectTableIds(charts);
                if (!tableIds.isEmpty()) {
                    LogUtil.getLogger().info("导出仪表板/大屏时自动导出关联的数据集, tableIds 数量: {}", tableIds.size());
                    datasets = backupDatasetService.exportDatasetsByTableIds(tableIds);
                    folders.addAll(backupDatasetService.collectDatasetFoldersByTableIds(tableIds));
                }
            }
            // 当导出了数据集但还没有导出数据源时，自动导出关联的数据源
            if (!datasets.isEmpty() && datasources.isEmpty()) {
                List<String> datasourceIds = backupDatasetService.collectDatasourceIds(datasets);
                if (!datasourceIds.isEmpty()) {
                    LogUtil.getLogger().info("导出仪表板/大屏时自动导出关联的数据源, datasourceIds 数量: {}", datasourceIds.size());
                    datasources = backupDatasourceService.exportDatasources(datasourceIds);
                    folders.addAll(backupDatasourceService.collectDatasourceFolders(datasourceIds));
                }
            }

            exportPackage.setDatasources(datasources);
            exportPackage.setDatasets(datasets);
            exportPackage.setDashboards(dashboards);
            exportPackage.setDataviews(dataviews);
            exportPackage.setFolders(folders);
            exportPackage.setCharts(charts);

            // Save to file
            File backupDir = new File(backupPath);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            String fileName = "export_" + exportId + ".json";
            File jsonFile = new File(backupDir, fileName);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(jsonFile, exportPackage);

            // Compress if needed
            if (request.getOptions() != null && Boolean.TRUE.equals(request.getOptions().isCompress())) {
                String zipFileName = "export_" + exportId + ".zip";
                File zipFile = new File(backupDir, zipFileName);
                try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
                    ZipEntry zipEntry = new ZipEntry(fileName);
                    zipOut.putNextEntry(zipEntry);
                    Files.copy(jsonFile.toPath(), zipOut);
                    zipOut.closeEntry();
                }
                jsonFile.delete();
                response.setId(exportId);
                response.setFileName(zipFileName);
                response.setDownloadUrl("/backupCenter/download/" + exportId);
            } else {
                response.setId(exportId);
                response.setFileName(fileName);
                response.setDownloadUrl("/backupCenter/download/" + exportId);
            }

            response.setStatus("success");
            response.setMessage("导出成功");
            exportPackages.put(exportId, exportPackage);

        } catch (Exception e) {
            LogUtil.getLogger().error("Export failed", e);
            response.setStatus("failed");
            response.setMessage("导出失败: " + e.getMessage());
        }
        return response;
    }

    public BackupResponse importData(BackupRequest request) {
        BackupResponse response = new BackupResponse();
        try {
            ExportPackage exportPackage = exportPackages.get(request.getId());
            if (exportPackage == null) {
                response.setStatus("failed");
                response.setMessage("导入包不存在或已过期");
                return response;
            }

            int successCount = 0;
            int failCount = 0;

            // 数据源 ID 映射表：用于存储旧数据源ID到新数据源ID的映射关系
            Map<String, String> datasourceIdMapping = new HashMap<>();
            // 数据集 ID 映射表：用于存储旧数据集ID到新数据集ID的映射关系
            Map<String, String> datasetIdMapping = new HashMap<>();
            // 图表 ID 映射表：用于存储旧图表ID到新图表ID的映射关系
            Map<String, Long> chartIdMapping = new HashMap<>();
            // 仪表板 ID 映射表：用于存储旧仪表板ID到新仪表板ID的映射关系
            Map<String, Long> dashboardIdMapping = new HashMap<>();
            // 大屏 ID 映射表：用于存储旧大屏ID到新大屏ID的映射关系
            Map<String, Long> dataviewIdMapping = new HashMap<>();

            // Get folders from export package
            List<BackupFolder> folders = exportPackage.getFolders();
            LogUtil.getLogger().info("=== importData: type={}, overwrite={}, datasources={}, datasets={}, dashboards={}, folders={} ===",
                exportPackage.getType(),
                request.isOverwrite(),
                exportPackage.getDatasources() != null ? exportPackage.getDatasources().size() : 0,
                exportPackage.getDatasets() != null ? exportPackage.getDatasets().size() : 0,
                exportPackage.getDashboards() != null ? exportPackage.getDashboards().size() : 0,
                folders != null ? folders.size() : 0);

            // Import datasources
            if (exportPackage.getDatasources() != null && !exportPackage.getDatasources().isEmpty()) {
                try {
                    backupDatasourceService.importDatasources(exportPackage.getDatasources(), folders, request.isOverwrite(), datasourceIdMapping);
                    successCount += exportPackage.getDatasources().size();
                    LogUtil.getLogger().info("=== Datasource ID mapping size: {} ===", datasourceIdMapping.size());
                } catch (Exception e) {
                    failCount += exportPackage.getDatasources().size();
                    LogUtil.getLogger().error("Import datasources failed", e);
                }
            }

            // Import datasets
            if (exportPackage.getDatasets() != null && !exportPackage.getDatasets().isEmpty()) {
                try {
                    backupDatasetService.importDatasets(exportPackage.getDatasets(), folders, request.isOverwrite(), datasetIdMapping, datasourceIdMapping);
                    successCount += exportPackage.getDatasets().size();
                } catch (Exception e) {
                    failCount += exportPackage.getDatasets().size();
                    LogUtil.getLogger().error("Import datasets failed", e);
                }
            }

            // 从 datasetIdMapping 构建 datasetIdMappingForChart（用于图表导入时替换 tableId）
            Map<String, Long> datasetIdMappingForChart = new HashMap<>();
            for (Map.Entry<String, String> entry : datasetIdMapping.entrySet()) {
                datasetIdMappingForChart.put(entry.getKey(), Long.parseLong(entry.getValue()));
            }
            LogUtil.getLogger().info("构建 datasetIdMappingForChart 完成, 大小: {}", datasetIdMappingForChart.size());

            // Import charts (before dashboards to build chartIdMapping) - 使用按名称匹配的方式
            if (exportPackage.getCharts() != null && !exportPackage.getCharts().isEmpty()) {
                LogUtil.getLogger().info("开始导入图表, charts 数量: {}", exportPackage.getCharts().size());
                // dashboardIdMapping 从 exportPackage.getIdMapping() 获取（仅在导出时有值）
                if (exportPackage.getIdMapping() != null && exportPackage.getIdMapping().getDashboardIds() != null) {
                    for (ExportPackage.IdPair pair : exportPackage.getIdMapping().getDashboardIds()) {
                        dashboardIdMapping.put(pair.getOldId(), Long.parseLong(pair.getNewId()));
                    }
                }
                LogUtil.getLogger().info("调用 importChartsWithResult 前, chartIdMapping 大小: {}, dashboardIdMapping 大小: {}", chartIdMapping.size(), dashboardIdMapping.size());
                ChartImportResult chartImportResult = backupDashboardService.importChartsWithResult(
                    exportPackage.getCharts(), dashboardIdMapping, request.isOverwrite());

                // 将成功导入的图表添加到 chartIdMapping
                for (ChartImportResult.ChartInfo chartInfo : chartImportResult.getImportedCharts()) {
                    chartIdMapping.put(chartInfo.getOldId(), chartInfo.getNewChartId());
                }

                // 设置缺失数据集信息到响应
                response.setMissingDatasets(chartImportResult.getMissingDatasets());
                response.setMissingChartCount(chartImportResult.getMissingCount());

                LogUtil.getLogger().info("调用 importChartsWithResult 后, 成功: {}, 缺失: {}, chartIdMapping 大小: {}",
                    chartImportResult.getSuccessCount(), chartImportResult.getMissingCount(), chartIdMapping.size());
            } else {
                LogUtil.getLogger().info("没有图表需要导入, charts 为空或 null");
            }

            // Import dashboards (using chartIdMapping to replace chart IDs in componentData)
            if (exportPackage.getDashboards() != null && !exportPackage.getDashboards().isEmpty()) {
                try {
                    backupDashboardService.importDashboards(exportPackage.getDashboards(), folders, request.isOverwrite(), chartIdMapping, dashboardIdMapping);
                    successCount += exportPackage.getDashboards().size();

                    // 更新导入图表的 sceneId，指向新导入的仪表板
                    if (exportPackage.getCharts() != null && !exportPackage.getCharts().isEmpty()) {
                        LogUtil.getLogger().info("更新图表 sceneId, dashboardIdMapping 大小: {}", dashboardIdMapping.size());
                        backupDashboardService.updateChartsSceneIds(exportPackage.getCharts(), dashboardIdMapping, chartIdMapping);
                    }
                } catch (Exception e) {
                    failCount += exportPackage.getDashboards().size();
                    LogUtil.getLogger().error("Import dashboards failed", e);
                }
            }

            // Import dataviews (using chartIdMapping to replace chart IDs in componentData)
            if (exportPackage.getDataviews() != null && !exportPackage.getDataviews().isEmpty()) {
                for (BackupDataview dataview : exportPackage.getDataviews()) {
                    if ("folder".equals(dataview.getNodeType())) {
                        // 跳过文件夹类型，它们已在 folders 中处理
                        continue;
                    }
                    try {
                        backupDashboardService.importDataview(dataview, request.isOverwrite(), chartIdMapping, dataviewIdMapping);
                        successCount++;
                    } catch (Exception e) {
                        failCount++;
                        LogUtil.getLogger().error("Import dataview failed: " + dataview.getName(), e);
                    }
                }

                // 更新导入图表的 sceneId，指向新导入的大屏
                if (exportPackage.getCharts() != null && !exportPackage.getCharts().isEmpty()) {
                    LogUtil.getLogger().info("更新图表 sceneId, dataviewIdMapping 大小: {}", dataviewIdMapping.size());
                    backupDashboardService.updateChartsSceneIds(exportPackage.getCharts(), dataviewIdMapping, chartIdMapping);
                }
            }

            exportPackages.remove(request.getId());

            response.setStatus("success");
            response.setMessage(String.format("导入完成，成功: %d，失败: %d", successCount, failCount));

        } catch (Exception e) {
            LogUtil.getLogger().error("Import failed", e);
            response.setStatus("failed");
            response.setMessage("导入失败: " + e.getMessage());
        }
        return response;
    }

    public BackupResponse upload(MultipartFile file) {
        BackupResponse response = new BackupResponse();
        try {
            String importId = UUIDUtils.getUUID();
            File backupDir = new File(backupPath);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            // 保留原始文件扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            File tempFile = new File(backupDir, "upload_" + importId + extension);
            file.transferTo(tempFile);

            ExportPackage exportPackage = parseExportPackage(tempFile);
            if (exportPackage == null) {
                tempFile.delete();
                response.setStatus("failed");
                response.setMessage("文件格式错误，无法解析");
                return response;
            }

            exportPackages.put(importId, exportPackage);
            response.setId(importId);
            response.setStatus("success");
            response.setMessage("上传成功");

        } catch (Exception e) {
            LogUtil.getLogger().error("Upload failed", e);
            response.setStatus("failed");
            response.setMessage("上传失败: " + e.getMessage());
        }
        return response;
    }

    public void download(String id, HttpServletResponse response) throws Exception {
        File backupDir = new File(backupPath);
        File jsonFile = new File(backupDir, "export_" + id + ".json");
        File zipFile = new File(backupDir, "export_" + id + ".zip");

        File downloadFile;
        String fileName;
        if (zipFile.exists()) {
            downloadFile = zipFile;
            fileName = zipFile.getName();
        } else if (jsonFile.exists()) {
            downloadFile = jsonFile;
            fileName = jsonFile.getName();
        } else {
            // 回退到内存缓存
            ExportPackage exportPackage = exportPackages.get(id);
            if (exportPackage != null) {
                String jsonStr = new ObjectMapper().writeValueAsString(exportPackage);
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment;filename=export_" + id + ".json");
                response.getWriter().write(jsonStr);
                return;
            }
            throw new FileNotFoundException("导出文件不存在");
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        Files.copy(downloadFile.toPath(), response.getOutputStream());
    }

    public String generateDownloadUri(String id) throws Exception {
        return "/backupCenter/download/" + id;
    }

    public ExportPackage preview(MultipartFile file) {
        try {
            File tempFile = File.createTempFile("preview_", ".tmp");
            file.transferTo(tempFile);
            ExportPackage exportPackage = parseExportPackage(tempFile);
            tempFile.delete();
            return exportPackage;
        } catch (Exception e) {
            LogUtil.getLogger().error("Preview failed", e);
            return null;
        }
    }

    public Map<String, Object> validate(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            File tempFile = File.createTempFile("validate_", ".tmp");
            file.transferTo(tempFile);
            ExportPackage exportPackage = parseExportPackage(tempFile);
            tempFile.delete();

            if (exportPackage == null) {
                result.put("valid", false);
                result.put("message", "文件格式错误");
            } else {
                result.put("valid", true);
                result.put("message", "文件有效");
                result.put("type", exportPackage.getType());
            }
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "验证失败: " + e.getMessage());
        }
        return result;
    }

    public List<BackupResponse> getHistory() {
        List<BackupResponse> history = new ArrayList<>();
        try {
            File backupDir = new File(backupPath);
            if (backupDir.exists()) {
                File[] files = backupDir.listFiles((dir, name) -> name.startsWith("export_") && (name.endsWith(".json") || name.endsWith(".zip")));
                if (files != null) {
                    for (File file : files) {
                        BackupResponse item = new BackupResponse();
                        String id = file.getName().replace("export_", "").replace(".json", "").replace(".zip", "");
                        item.setId(id);
                        item.setFileName(file.getName());
                        item.setExportTime(file.lastModified());
                        item.setStatus("success");

                        ExportPackage pkg = parseExportPackage(file);
                        if (pkg != null) {
                            int count = 0;
                            if (pkg.getDatasources() != null) count += pkg.getDatasources().size();
                            if (pkg.getDatasets() != null) count += pkg.getDatasets().size();
                            if (pkg.getDashboards() != null) count += pkg.getDashboards().size();
                            if (pkg.getDataviews() != null) count += pkg.getDataviews().size();
                            item.setItemCount(count);
                            item.setMessage(pkg.getType());
                        }

                        history.add(item);
                    }
                }
            }
            // 按导出时间从新到旧排序
            history.sort((a, b) -> Long.compare(b.getExportTime(), a.getExportTime()));
        } catch (Exception e) {
            LogUtil.getLogger().error("Get history failed", e);
        }
        return history;
    }

    public void delete(String id) {
        try {
            File backupDir = new File(backupPath);
            File jsonFile = new File(backupDir, "export_" + id + ".json");
            File zipFile = new File(backupDir, "export_" + id + ".zip");
            if (jsonFile.exists()) jsonFile.delete();
            if (zipFile.exists()) zipFile.delete();
            exportPackages.remove(id);
        } catch (Exception e) {
            LogUtil.getLogger().error("Delete failed", e);
        }
    }

    public Map<String, String> getSupportedTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("datasource", "数据源");
        types.put("dataset", "数据集");
        types.put("dashboard", "仪表板");
        types.put("dataview", "数据大屏");
        types.put("combined", "完整导出");
        return types;
    }

    public Map<String, Object> getOptions(String type) {
        Map<String, Object> options = new HashMap<>();
        options.put("compress", true);
        return options;
    }

    public Map<String, Boolean> checkExist(List<String> names) {
        Map<String, Boolean> result = new HashMap<>();
        for (String name : names) {
            result.put(name, false);
        }
        return result;
    }

    public Map<String, String> getImportModes() {
        Map<String, String> modes = new LinkedHashMap<>();
        modes.put("create", "创建新资源（重名自动重命名）");
        modes.put("overwrite", "覆盖同名资源");
        return modes;
    }

    public String getBackupPath() {
        return backupPath;
    }

    private ExportPackage parseExportPackage(File file) {
        try {
            if (file.getName().endsWith(".zip") || file.getName().endsWith(".debk")) {
                File tempDir = Files.createTempDirectory("backup_preview").toFile();
                try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(file))) {
                    ZipEntry zipEntry = zipIn.getNextEntry();
                    if (zipEntry != null) {
                        File outputFile = new File(tempDir, zipEntry.getName());
                        try (OutputStream os = new FileOutputStream(outputFile)) {
                            zipIn.transferTo(os);
                        }
                        return new ObjectMapper().readValue(outputFile, ExportPackage.class);
                    }
                }
                return null;
            } else {
                return new ObjectMapper().readValue(file, ExportPackage.class);
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Parse export package failed", e);
            return null;
        }
    }

    public DependencyInfo checkDependencies(String type, List<String> resourceIds) {
        DependencyInfo result = new DependencyInfo();
        DependencyInfo.Dependencies dependencies = result.getDependencies();

        if (resourceIds == null || resourceIds.isEmpty()) {
            result.setHasDependencies(false);
            return result;
        }

        switch (type) {
            case "dataset":
                List<DependencyInfo.ResourceItem> datasources = queryDatasetDatasources(resourceIds);
                dependencies.getDatasources().addAll(datasources);
                break;
            case "dashboard":
            case "dataview":
                List<DependencyInfo.ResourceItem> datasets = queryDashboardDatasets(resourceIds);
                dependencies.getDatasets().addAll(datasets);
                if (!datasets.isEmpty()) {
                    List<String> datasetIds = datasets.stream()
                        .map(DependencyInfo.ResourceItem::getId)
                        .distinct()
                        .collect(Collectors.toList());
                    List<DependencyInfo.ResourceItem> ds = queryDatasetDatasources(datasetIds);
                    dependencies.getDatasources().addAll(ds);
                }
                break;
            default:
                break;
        }

        dependencies.setDatasources(new ArrayList<>(new HashSet<>(dependencies.getDatasources())));
        dependencies.setDatasets(new ArrayList<>(new HashSet<>(dependencies.getDatasets())));

        result.setHasDependencies(
            !dependencies.getDatasources().isEmpty() || !dependencies.getDatasets().isEmpty()
        );
        return result;
    }

    private List<DependencyInfo.ResourceItem> queryDatasetDatasources(List<String> datasetIds) {
        if (datasetIds == null || datasetIds.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String sql = "SELECT DISTINCT d.id, d.name FROM core_datasource d " +
                         "INNER JOIN core_dataset_table dt ON d.id = dt.datasource_id " +
                         "WHERE dt.id IN (:ids)";
            return namedParameterJdbcTemplate.query(
                sql,
                Map.of("ids", datasetIds),
                (rs, rowNum) -> new DependencyInfo.ResourceItem(rs.getString("id"), rs.getString("name"))
            );
        } catch (Exception e) {
            LogUtil.getLogger().error("Query dataset datasources failed", e);
            return Collections.emptyList();
        }
    }

    private List<DependencyInfo.ResourceItem> queryDashboardDatasets(List<String> dashboardIds) {
        if (dashboardIds == null || dashboardIds.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String sql = "SELECT DISTINCT dt.id, dt.name FROM core_dataset_table dt " +
                         "INNER JOIN core_chart_view cv ON dt.id = cv.table_id " +
                         "WHERE cv.scene_id IN (:ids)";
            return namedParameterJdbcTemplate.query(
                sql,
                Map.of("ids", dashboardIds),
                (rs, rowNum) -> new DependencyInfo.ResourceItem(rs.getString("id"), rs.getString("name"))
            );
        } catch (Exception e) {
            LogUtil.getLogger().error("Query dashboard datasets failed", e);
            return Collections.emptyList();
        }
    }
}
