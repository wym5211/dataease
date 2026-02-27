package io.dataease.system.dao.auto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * <p>
 * 行权限表
 * </p>
 *
 * @author fit2cloud
 * @since 2024-03-12
 */
@Data
@TableName("sys_row_permission")
public class SysRowPermission implements Serializable {

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
     * 授权目标ID(role_id/user_id)
     */
    private Long authTargetId;

    /**
     * 授权目标类型(0:user 1:role)
     */
    private Integer authTargetType;

    /**
     * 过滤条件表达式
     */
    private String filterExpression;

    /**
     * 是否启用
     */
    private Integer enable;
}
