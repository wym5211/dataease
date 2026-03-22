package io.dataease.model.backup;

import lombok.Data;

import java.util.List;

/**
 * 数据大屏备份信息
 */
@Data
public class BackupDataview {

    /**
     * 原ID
     */
    private String id;

    /**
     * 大屏名称
     */
    private String name;

    /**
     * 大屏类型
     */
    private String type;

    /**
     * 大屏配置 (JSON格式)
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
     * 关联的视图ID列表
     */
    private List<String> viewIds;

    /**
     * 大屏样式配置
     */
    private String style;

    /**
     * 大屏设置
     */
    private String settings;
}
