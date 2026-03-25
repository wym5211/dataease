package io.dataease.model.backup;

import lombok.Data;

import java.util.List;

import io.dataease.model.backup.BackupDatasetTable;

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
     * 目录路径（用于日志追踪）
     */
    private String folderPath;

    /**
     * 父目录ID
     */
    private Long pid;

    /**
     * 层级深度
     */
    private Integer level;

    /**
     * 节点类型: folder
     */
    private String nodeType;

    /**
     * 关联的视图ID列表
     */
    private List<String> viewIds;

    /**
     * 关联SQL
     */
    private String unionSql;

    /**
     * 是否跨数据集
     */
    private Boolean isCross;

    /**
     * 嵌套的数据表列表
     */
    private List<BackupDatasetTable> tables;
}
