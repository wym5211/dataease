package io.dataease.permissions.response;

import io.dataease.result.ResultMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 权限响应工具类
 */
public class PermissionResponse {

    /**
     * 成功响应
     */
    public static ResponseEntity<ResultMessage> success(String message) {
        ResultMessage resultMessage = new ResultMessage(200, message);
        return new ResponseEntity<>(resultMessage, HttpStatus.OK);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> ResponseEntity<ResultMessage<T>> success(T data) {
        ResultMessage<T> resultMessage = new ResultMessage<>(200, "操作成功", data);
        return new ResponseEntity<>(resultMessage, HttpStatus.OK);
    }

    /**
     * 权限不足响应
     */
    public static ResponseEntity<ResultMessage> forbidden(String message) {
        ResultMessage resultMessage = new ResultMessage(403, message);
        return new ResponseEntity<>(resultMessage, HttpStatus.FORBIDDEN);
    }

    /**
     * 未认证响应
     */
    public static ResponseEntity<ResultMessage> unauthorized(String message) {
        ResultMessage resultMessage = new ResultMessage(401, message);
        return new ResponseEntity<>(resultMessage, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 参数错误响应
     */
    public static ResponseEntity<ResultMessage> badRequest(String message) {
        ResultMessage resultMessage = new ResultMessage(400, message);
        return new ResponseEntity<>(resultMessage, HttpStatus.BAD_REQUEST);
    }
}