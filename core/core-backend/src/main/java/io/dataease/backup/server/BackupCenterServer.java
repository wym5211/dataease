package io.dataease.backup.server;

import io.dataease.api.backup.BackupCenterApi;
import io.dataease.backup.manage.BackupCenterManage;
import io.dataease.model.backup.BackupRequest;
import io.dataease.model.backup.BackupResponse;
import io.dataease.model.backup.ExportPackage;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/backupCenter")
public class BackupCenterServer implements BackupCenterApi {

    @Autowired
    private BackupCenterManage backupCenterManage;

    @Override
    @Operation(summary = "导出资源")
    @PostMapping("/export")
    public BackupResponse export(@RequestBody BackupRequest request) {
        return backupCenterManage.export(request);
    }

    @Override
    @Operation(summary = "导入资源")
    @PostMapping("/import")
    public BackupResponse importData(@RequestBody BackupRequest request) {
        return backupCenterManage.importData(request);
    }

    @Override
    @Operation(summary = "上传导入文件")
    @PostMapping("/upload")
    public BackupResponse upload(@RequestParam("file") MultipartFile file) {
        return backupCenterManage.upload(file);
    }

    @Override
    @Operation(summary = "下载导出文件")
    @GetMapping("/download/{id}")
    public void download(@PathVariable("id") String id, HttpServletResponse response) throws Exception {
        backupCenterManage.download(id, response);
    }

    @Override
    @Operation(summary = "生成下载链接")
    @GetMapping("/generateDownloadUri/{id}")
    public String generateDownloadUri(@PathVariable("id") String id) throws Exception {
        return backupCenterManage.generateDownloadUri(id);
    }

    @Override
    @Operation(summary = "预览导入内容")
    @PostMapping("/preview")
    public ExportPackage preview(@RequestParam("file") MultipartFile file) {
        return backupCenterManage.preview(file);
    }

    @Override
    @Operation(summary = "验证导入数据")
    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestParam("file") MultipartFile file) {
        return backupCenterManage.validate(file);
    }

    @Override
    @Operation(summary = "获取导出历史")
    @GetMapping("/history")
    public List<BackupResponse> getHistory() {
        return backupCenterManage.getHistory();
    }

    @Override
    @Operation(summary = "删除导出记录")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) {
        backupCenterManage.delete(id);
    }

    @Override
    @Operation(summary = "获取支持的导出类型")
    @GetMapping("/types")
    public Map<String, String> getSupportedTypes() {
        return backupCenterManage.getSupportedTypes();
    }

    @Override
    @Operation(summary = "获取导出选项")
    @GetMapping("/options")
    public Map<String, Object> getOptions(@RequestParam("type") String type) {
        return backupCenterManage.getOptions(type);
    }

    @Override
    @Operation(summary = "检查资源是否存在")
    @PostMapping("/checkExist")
    public Map<String, Boolean> checkExist(@RequestBody List<String> names) {
        return backupCenterManage.checkExist(names);
    }

    @Override
    @Operation(summary = "获取导入模式选项")
    @GetMapping("/importModes")
    public Map<String, String> getImportModes() {
        return backupCenterManage.getImportModes();
    }
}
