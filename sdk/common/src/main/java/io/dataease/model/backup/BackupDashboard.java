package io.dataease.model.backup;

import lombok.Data;

import java.util.List;

/**
 * 仪表板备份信息
 */
@Data
public class BackupDashboard {

    /**
     * 原ID
     */
    private String id;

    /**
     * 仪表板名称
     */
    private String name;

    /**
     * 仪表板类型
     */
    private String type;

    /**
     * 仪表板配置 (JSON格式)
     */
    private String config;

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
     * 仪表板样式配置
     */
    private String style;

    /**
     * 所属组织ID
     */
    private Long orgId;

    /**
     * 画布样式数据
     */
    private String canvasStyleData;

    /**
     * 组件数据
     */
    private String componentData;

    /**
     * 内容标识
     */
    private String contentId;
}
