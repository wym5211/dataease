package io.dataease.model.backup;

import lombok.Data;

/**
 * 数据源备份信息
 */
@Data
public class BackupDatasource {

    /**
     * 原ID
     */
    private String id;

    /**
     * 数据源名称
     */
    private String name;

    /**
     * 数据源类型 (mysql, postgres, etc.)
     */
    private String type;

    /**
     * 数据源配置 (JSON格式，包含连接信息)
     */
    private String configuration;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 修改人
     */
    private String updateBy;

    /**
     * 修改时间
     */
    private Long updateTime;
}
