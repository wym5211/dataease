package io.dataease.permissions.notify;

import io.dataease.system.dao.auto.entity.SysUserRole;
import io.dataease.system.dao.auto.mapper.SysUserRoleMapper;
import io.dataease.websocket.WsMessage;
import io.dataease.websocket.WsService;
import io.dataease.websocket.message.PermissionChangeMessage;
import io.dataease.websocket.util.WsUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionChangeNotifier {

    private final WsService wsService;
    private final SysUserRoleMapper sysUserRoleMapper;

    /** WebSocket 权限变更话题 */
    public static final String PERMISSION_CHANGE_TOPIC = "/permission-change-topic";

    /**
     * 通知单个用户权限已变更
     * @param userId     目标用户ID
     * @param changeType 变更类型（MENU/RESOURCE/DATA）
     */
    public void notifyUser(Long userId, String changeType) {
        if (userId == null) return;
        if (!WsUtil.isOnLine(userId)) {
            // 用户不在线，无需推送（下次登录时权限自然刷新）
            return;
        }
        try {
            PermissionChangeMessage message = new PermissionChangeMessage(changeType);
            WsMessage<PermissionChangeMessage> wsMessage = new WsMessage<>(
                userId, PERMISSION_CHANGE_TOPIC, message
            );
            wsService.releaseMessage(wsMessage);
            log.debug("已向用户 {} 推送权限变更通知，类型：{}", userId, changeType);
        } catch (Exception e) {
            log.warn("向用户 {} 推送权限变更通知失败：{}", userId, e.getMessage());
        }
    }

    /**
     * 通知某角色下所有在线用户权限已变更
     * @param roleId     角色ID
     * @param changeType 变更类型（MENU/RESOURCE/DATA）
     */
    public void notifyRole(Long roleId, String changeType) {
        if (roleId == null) return;
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
            new QueryWrapper<SysUserRole>().eq("role_id", roleId)
        );
        if (CollectionUtils.isEmpty(userRoles)) return;
        for (SysUserRole ur : userRoles) {
            if (ur == null || ur.getUserId() == null) continue;
            notifyUser(ur.getUserId(), changeType);
        }
    }

    /**
     * 通知指定用户列表权限已变更（复用已查询的 userId 列表，避免重复 DB 查询）
     */
    public void notifyUsers(List<Long> userIds, String changeType) {
        if (CollectionUtils.isEmpty(userIds)) return;
        for (Long userId : userIds) {
            notifyUser(userId, changeType);
        }
    }
}
