package io.dataease.system.dao.auto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * <p>
 * 资源权限表
 * </p>
 *
 * @author fit2cloud
 * @since 2024-03-12
 */
@Data
@TableName("sys_resource_permission")
public class SysResourcePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 资源ID
     */
    private String resourceId;

    /**
     * 资源类型(dashboard/dataset/datasource)
     */
    private String resourceType;

    /**
     * 拥有者ID(user_id/role_id/dept_id)
     */
    private Long ownerId;

    /**
     * 拥有者类型(0:user 1:role 2:dept)
     */
    private Integer ownerType;

    /**
     * 权限(1:read 2:write 4:share)
     */
    private Integer permission;
}
