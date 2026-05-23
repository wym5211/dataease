package io.dataease.permissions.handler;

import io.dataease.permissions.exception.PermissionException;
import io.dataease.result.ResultMessage;
import io.dataease.utils.LogUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 权限异常统一处理器
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PermissionExceptionHandler {

    /**
     * 处理权限异常
     */
    @ExceptionHandler(PermissionException.class)
    public ResponseEntity<ResultMessage> handlePermissionException(PermissionException e) {
        LogUtil.error("权限异常: " + e.getMessage(), e);
        ResultMessage resultMessage = new ResultMessage(e.getCode(), e.getMessage());
        return new ResponseEntity<>(resultMessage, HttpStatus.FORBIDDEN);
    }

}