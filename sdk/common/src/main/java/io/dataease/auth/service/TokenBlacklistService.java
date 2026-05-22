package io.dataease.auth.service;

import io.dataease.auth.bo.TokenUserBO;
import io.dataease.constant.CacheConstant;
import io.dataease.utils.CacheUtils;
import io.dataease.utils.TokenUtils;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务
 * 主存储使用 CacheUtils（EhCache disk 持久化 / Redis），重启不丢失，集群一致。
 * userTokens 二级索引保留在进程内内存（丢失可接受，仅影响按用户批量撤销的便利性）。
 */
@Service
public class TokenBlacklistService {

    /**
     * userId -> 该用户被列入黑名单的 token 集合（二级索引，进程内）
     */
    private final ConcurrentHashMap<Long, Set<String>> userTokens = new ConcurrentHashMap<>();

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
        } catch (Exception ignored) {
            // 解析失败不影响黑名单加入
        }
    }

    /**
     * 检查 token 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        Boolean exists = CacheUtils.keyExist(CacheConstant.CommonCacheConstant.TOKEN_BLACKLIST_CACHE, token);
        return exists != null && exists;
    }

    /**
     * 按用户 ID 使其所有 token 失效
     */
    public void blacklistByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        Set<String> tokens = userTokens.get(userId);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        for (String token : tokens) {
            CacheUtils.put(CacheConstant.CommonCacheConstant.TOKEN_BLACKLIST_CACHE, token, "1", 24L, TimeUnit.HOURS);
        }
        userTokens.remove(userId);
    }
}
