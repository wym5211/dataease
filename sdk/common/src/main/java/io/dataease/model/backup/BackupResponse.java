package io.dataease.model.backup;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class BackupResponse {

    private int code = 0;

    @JsonSerialize(using = ToStringSerializer.class)
    private String id;

    /**
     * 导出/导入状态: success, failed, processing
     */
    private String status;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件下载地址
     */
    private String downloadUrl;

    /**
     * 导出/导入的消息
     */
    private String message;

    /**
     * 导出项数量
     */
    private Integer itemCount;

    /**
     * 导出时间
     */
    private Long exportTime;
}
