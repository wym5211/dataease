package io.dataease.system.dao.auto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * <p>
 * 列权限表
 * </p>
 *
 * @author fit2cloud
 * @since 2024-03-12
 */
@Data
@TableName("sys_column_permission")
public class SysColumnPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 数据集ID
     */
    private String datasetId;

    /**
     * 授权目标ID
     */
    private Long authTargetId;

    /**
     * 授权目标类型
     */
    private Integer authTargetType;

    /**
     * 列名
     */
    private String columnName;

    /**
     * 权限类型(0:隐藏 1:脱敏)
     */
    private Integer permissionType;

    /**
     * 脱敏规则
     */
    private String maskRule;
}
