package io.dataease.permissions.handler;

import io.dataease.exception.DEException;
import io.dataease.permissions.exception.PermissionException;
import io.dataease.result.ResultMessage;
import io.dataease.utils.LogUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 权限异常统一处理器
 */
@RestControllerAdvice
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

    /**
     * 处理DEException中的权限相关异常
     */
    @ExceptionHandler(DEException.class)
    public ResponseEntity<ResultMessage> handleDEException(DEException e) {
        // 如果是权限相关的错误码，统一处理
        if (isPermissionError(e.getCode())) {
            LogUtil.error("权限异常: " + e.getMessage(), e);
            ResultMessage resultMessage = new ResultMessage(e.getCode(), e.getMessage());
            return new ResponseEntity<>(resultMessage, HttpStatus.FORBIDDEN);
        }
        
        // 其他DEException不处理，继续抛出
        throw e;
    }

    /**
     * 判断是否为权限相关错误码
     */
    private boolean isPermissionError(Integer code) {
        // 这里可以根据实际的错误码来判断
        // 假设权限相关的错误码是403系列
        return code != null && (code == 403 || code.toString().startsWith("403"));
    }
}