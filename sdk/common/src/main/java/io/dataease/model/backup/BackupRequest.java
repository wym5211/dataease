package io.dataease.model.backup;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;

@Data
public class BackupRequest {

    @JsonSerialize(using = ToStringSerializer.class)
    private String id;

    /**
     * 导出类型: datasource, dataset, dashboard, dataview, combined
     */
    private String type;

    /**
     * 要导出的资源ID列表
     */
    private List<String> resourceIds;

    /**
     * 导出选项
     */
    private BackupOptions options;

    /**
     * 导入模式: create, update, skip, rename
     */
    private String importMode;

    /**
     * 导入时是否覆盖同名资源
     */
    private boolean overwrite;

    /**
     * 导出文件版本
     */
    private String version;
}
