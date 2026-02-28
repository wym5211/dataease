package io.dataease.permissions.exception;

import io.dataease.exception.DEException;
import io.dataease.result.ResultCode;

/**
 * 权限异常类
 */
public class PermissionException extends DEException {

    public PermissionException(String message) {
        super(ResultCode.PERMISSION_NO_ACCESS.code(), message);
    }

    public PermissionException(ResultCode resultCode) {
        super(resultCode.code(), resultCode.message());
    }

    public PermissionException(Integer code, String message) {
        super(code, message);
    }
}