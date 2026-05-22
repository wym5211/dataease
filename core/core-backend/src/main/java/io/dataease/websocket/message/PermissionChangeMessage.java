package io.dataease.websocket.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 权限变更通知消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionChangeMessage implements Serializable {
    /** 消息类型固定值 */
    private String type = "PERMISSION_CHANGE";
    /** 变更类型：MENU / RESOURCE / DATA */
    private String changeType;
    /** 时间戳 */
    private Long timestamp = System.currentTimeMillis();

    public PermissionChangeMessage(String changeType) {
        this.type = "PERMISSION_CHANGE";
        this.changeType = changeType;
        this.timestamp = System.currentTimeMillis();
    }
}
