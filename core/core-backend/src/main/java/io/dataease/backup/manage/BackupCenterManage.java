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
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
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

    private Map<String, ExportPackage> exportPackages = new HashMap<>();

    public BackupResponse export(BackupRequest request) {
        BackupResponse response = new BackupResponse();
        try {
            String exportId = UUIDUtils.getUUID();
            ExportPackage exportPackage = new ExportPackage();
            exportPackage.setVersion("1.0");
            exportPackage.setExportTime(System.currentTimeMillis());
            exportPackage.setType(request.getType());

            List<BackupDatasource> datasources = new ArrayList<>();
            List<BackupDataset> datasets = new ArrayList<>();
            List<BackupDashboard> dashboards = new ArrayList<>();
            List<BackupDataview> dataviews = new ArrayList<>();

            if ("datasource".equals(request.getType()) || "combined".equals(request.getType())) {
                datasources = backupDatasourceService.exportDatasources();
            }
            if ("dataset".equals(request.getType()) || "combined".equals(request.getType())) {
                datasets = backupDatasetService.exportDatasets();
            }
            if ("dashboard".equals(request.getType()) || "combined".equals(request.getType())) {
                dashboards = backupDashboardService.exportDashboards();
            }
            if ("dataview".equals(request.getType()) || "combined".equals(request.getType())) {
                dataviews = backupDashboardService.exportDataviews();
            }

            exportPackage.setDatasources(datasources);
            exportPackage.setDatasets(datasets);
            exportPackage.setDashboards(dashboards);
            exportPackage.setDataviews(dataviews);

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

            // ID映射表：用于存储旧ID到新ID的映射关系
            Map<String, String> idMapping = new HashMap<>();

            // Import datasources
            if (exportPackage.getDatasources() != null) {
                for (BackupDatasource ds : exportPackage.getDatasources()) {
                    try {
                        String originalId = ds.getId();
                        String newId = backupDatasourceService.importDatasource(ds, request.isOverwrite());
                        idMapping.put(originalId, newId);
                        successCount++;
                    } catch (Exception e) {
                        failCount++;
                        LogUtil.getLogger().error("Import datasource failed: " + ds.getName(), e);
                    }
                }
            }

            // Import datasets
            if (exportPackage.getDatasets() != null) {
                for (BackupDataset ds : exportPackage.getDatasets()) {
                    try {
                        backupDatasetService.importDatasetWithTables(ds, request.isOverwrite(), idMapping);
                        successCount++;
                    } catch (Exception e) {
                        failCount++;
                        LogUtil.getLogger().error("Import dataset failed: " + ds.getName(), e);
                    }
                }
            }

            // Import dashboards
            if (exportPackage.getDashboards() != null) {
                for (BackupDashboard dashboard : exportPackage.getDashboards()) {
                    try {
                        backupDashboardService.importDashboard(dashboard, request.isOverwrite());
                        successCount++;
                    } catch (Exception e) {
                        failCount++;
                        LogUtil.getLogger().error("Import dashboard failed: " + dashboard.getName(), e);
                    }
                }
            }

            // Import dataviews
            if (exportPackage.getDataviews() != null) {
                for (BackupDataview dataview : exportPackage.getDataviews()) {
                    try {
                        backupDashboardService.importDataview(dataview, request.isOverwrite());
                        successCount++;
                    } catch (Exception e) {
                        failCount++;
                        LogUtil.getLogger().error("Import dataview failed: " + dataview.getName(), e);
                    }
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
}
