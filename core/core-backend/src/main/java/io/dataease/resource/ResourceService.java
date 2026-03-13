package io.dataease.resource;

import io.dataease.api.permissions.auth.dto.BusiPerCheckDTO;
import io.dataease.constant.AuthEnum;
import io.dataease.system.manage.CorePermissionManage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * @Author Junjun
 */
@Component
public class ResourceService {
    @Resource
    private CorePermissionManage corePermissionManage;

    public boolean checkPermission(Long id) {
        return checkPermissionByAuth(id, AuthEnum.READ);
    }

    public boolean checkManagePermission(Long id) {
        return checkPermissionByAuth(id, AuthEnum.MANAGE);
    }

    private boolean checkPermissionByAuth(Long id, AuthEnum authEnum) {
        BusiPerCheckDTO dto = new BusiPerCheckDTO();
        dto.setId(id);
        dto.setAuthEnum(authEnum);
        boolean b;
        try {
            b = corePermissionManage.checkAuth(dto);
        } catch (Exception e) {
            b = false;
        }
        return b;
    }
}
