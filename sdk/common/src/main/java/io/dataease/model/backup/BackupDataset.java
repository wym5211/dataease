package io.dataease.model.backup;

import lombok.Data;

import java.util.List;

/**
 * 数据集备份信息
 */
@Data
public class BackupDataset {

    /**
     * 原ID
     */
    private String id;

    /**
     * 数据集名称
     */
    private String name;

    /**
     * 关联的数据源ID
     */
    private String datasourceId;

    /**
     * 数据集类型 (SQL, dataset, etc.)
     */
    private String type;

    /**
     * SQL或数据模型定义
     */
    private String model;

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

    /**
     * 关联的视图ID列表
     */
    private List<String> viewIds;

    /**
     * 关联SQL
     */
    private String unionSql;
}
