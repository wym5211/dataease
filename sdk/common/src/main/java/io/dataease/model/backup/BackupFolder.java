package io.dataease.model.backup;

import lombok.Data;

/**
 * 目录备份信息
 */
@Data
public class BackupFolder {
    /**
     * 原ID
     */
    private String id;

    /**
     * 目录名称
     */
    private String name;

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
     * 资源类型: dataset | dashboard | datasource
     */
    private String resourceType;

    /**
     * 子类型: dashboard/dataV (仅 dashboard 类型需要)
     */
    private String subType;

    /**
     * 父目录名称（用于跨环境匹配）
     */
    private String parentName;
}
