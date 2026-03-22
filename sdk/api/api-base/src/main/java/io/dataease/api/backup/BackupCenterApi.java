package io.dataease.api.backup;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.dataease.auth.DeApiPath;
import io.dataease.auth.DePermit;
import io.dataease.model.backup.BackupRequest;
import io.dataease.model.backup.BackupResponse;
import io.dataease.model.backup.ExportPackage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static io.dataease.constant.AuthResourceEnum.PANEL;

@Tag(name = "备份中心")
@ApiSupport(order = 975)
@DeApiPath(value = "/backupCenter", rt = PANEL)
public interface BackupCenterApi {

    @Operation(summary = "导出资源")
    @PostMapping("/export")
    BackupResponse export(@RequestBody BackupRequest request);

    @Operation(summary = "导入资源")
    @PostMapping("/import")
    BackupResponse importData(@RequestBody BackupRequest request);

    @Operation(summary = "上传导入文件")
    @PostMapping("/upload")
    BackupResponse upload(@RequestParam("file") MultipartFile file);

    @Operation(summary = "下载导出文件")
    @GetMapping("/download/{id}")
    void download(@PathVariable("id") String id, HttpServletResponse response) throws Exception;

    @Operation(summary = "生成下载链接")
    @GetMapping("/generateDownloadUri/{id}")
    String generateDownloadUri(@PathVariable("id") String id) throws Exception;

    @Operation(summary = "预览导入内容")
    @PostMapping("/preview")
    ExportPackage preview(@RequestParam("file") MultipartFile file);

    @Operation(summary = "验证导入数据")
    @PostMapping("/validate")
    Map<String, Object> validate(@RequestParam("file") MultipartFile file);

    @DePermit("m:read")
    @Operation(summary = "获取导出历史")
    @GetMapping("/history")
    List<BackupResponse> getHistory();

    @Operation(summary = "删除导出记录")
    @DeleteMapping("/{id}")
    void delete(@PathVariable("id") String id);

    @Operation(summary = "获取支持的导出类型")
    @GetMapping("/types")
    Map<String, String> getSupportedTypes();

    @Operation(summary = "获取导出选项")
    @GetMapping("/options")
    Map<String, Object> getOptions(@RequestParam("type") String type);

    @Operation(summary = "检查资源是否存在")
    @PostMapping("/checkExist")
    Map<String, Boolean> checkExist(@RequestBody List<String> names);

    @Operation(summary = "获取导入模式选项")
    @GetMapping("/importModes")
    Map<String, String> getImportModes();
}
