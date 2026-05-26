package io.dataease.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.constant.CacheConstant;
import io.dataease.utils.CacheUtils;
import io.dataease.utils.LogUtil;
import io.dataease.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务
 * 主存储使用 CacheUtils（EhCache disk 持久化 / Redis），重启不丢失，集群一致。
 * userTokens 二级索引保留在进程内内存（丢失可接受，仅影响按用户批量撤销的便利性）。
 * 用户级别撤销标记（user_revoke:{userId}）持久化到 CacheUtils，服务重启后仍可阻止被撤销用户的请求。
 */
@Service
public class TokenBlacklistService {

    private static final String USER_REVOKE_PREFIX = "user_revoke:";

    private final ConcurrentHashMap<Long, Set<String>> userTokens = new ConcurrentHashMap<>();

    @Value("${dataease.login_timeout:2880}")
    private long loginTimeoutMinutes;

    /**
     * 将 token 加入黑名单
     */
    public void blacklist(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        CacheUtils.put(CacheConstant.CommonCacheConstant.TOKEN_BLACKLIST_CACHE, token, "1", 24L, TimeUnit.HOURS);
        try {
            TokenUserBO bo = TokenUtils.userBOByToken(token);
            if (bo != null && bo.getUserId() != null) {
                userTokens.computeIfAbsent(bo.getUserId(), k -> new CopyOnWriteArraySet<>()).add(token);
            }
        } catch (Exception e) {
            LogUtil.warn("JWT decode failed in blacklist add: " + e.getMessage());
        }
    }

    /**
     * 检查 token 是否在黑名单中（含用户级别撤销检查）
     */
    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        // 检查 token 级别黑名单
        Boolean exists = CacheUtils.keyExist(CacheConstant.CommonCacheConstant.TOKEN_BLACKLIST_CACHE, token);
        if (exists != null && exists) {
            return true;
        }
        // 检查用户级别撤销标记（服务重启后仍有效）
        try {
            DecodedJWT jwt = JWT.decode(token);
            Long userId = jwt.getClaim("uid").asLong();
            if (userId != null) {
                Boolean userRevoked = CacheUtils.keyExist(
                    CacheConstant.CommonCacheConstant.TOKEN_BLACKLIST_CACHE, USER_REVOKE_PREFIX + userId);
                if (userRevoked != null && userRevoked) {
                    return true;
                }
            }
        } catch (Exception e) {
            LogUtil.debug("JWT decode failed in blacklist check: " + e.getMessage());
        }
        return false;
    }

    /**
     * 按用户 ID 使其所有 token 失效
     */
    public void blacklistByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        // 存储用户级别撤销标记，TTL 覆盖 access token 最大有效期
        long ttlHours = Math.max((long) Math.ceil(loginTimeoutMinutes / 60.0), 24L);
        CacheUtils.put(CacheConstant.CommonCacheConstant.TOKEN_BLACKLIST_CACHE,
            USER_REVOKE_PREFIX + userId, "1", ttlHours, TimeUnit.HOURS);
        // 尝试撤销内存索引中的个别 token
        Set<String> tokens = userTokens.get(userId);
        if (tokens != null && !tokens.isEmpty()) {
            for (String token : tokens) {
                CacheUtils.put(CacheConstant.CommonCacheConstant.TOKEN_BLACKLIST_CACHE, token, "1", 24L, TimeUnit.HOURS);
            }
        }
        userTokens.remove(userId);
    }
}
