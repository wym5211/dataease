package io.dataease.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import io.dataease.auth.vo.TokenVO;
import io.dataease.constant.CacheConstant;
import io.dataease.exception.DEException;
import io.dataease.utils.CacheUtils;
import io.dataease.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 服务
 * 主存储使用 CacheUtils（EhCache disk 持久化 / Redis），重启不丢失，集群一致。
 * userRefreshTokens 二级索引保留在进程内内存（丢失可接受，仅影响按用户批量撤销的便利性）。
 */
@Service
public class TokenRefreshService {

    /**
     * userId -> refreshToken 集合（二级索引，进程内）
     */
    private final ConcurrentHashMap<Long, Set<String>> userRefreshTokens = new ConcurrentHashMap<>();

    /**
     * Refresh Token 有效期：7 天
     */
    private static final long REFRESH_TOKEN_TTL_MS = 7L * 24 * 60 * 60 * 1000;

    /**
     * Access Token 默认过期时间（分钟），与 dataease.login_timeout 对齐
     */
    @Value("${dataease.login_timeout:2880}")
    private long loginTimeoutMinutes;

    /**
     * 可序列化的 Refresh Entry（EhCache disk 持久化和 Redis 都需要）
     */
    public static class RefreshEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long userId;
        private Long oid;
        private long expireAt;

        public RefreshEntry() {}

        public RefreshEntry(Long userId, Long oid, long expireAt) {
            this.userId = userId;
            this.oid = oid;
            this.expireAt = expireAt;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getOid() { return oid; }
        public void setOid(Long oid) { this.oid = oid; }
        public long getExpireAt() { return expireAt; }
        public void setExpireAt(long expireAt) { this.expireAt = expireAt; }
    }

    /**
     * 生成 Refresh Token，并保存映射
     */
    public String generateRefreshToken(Long userId, Long oid) {
        if (userId == null) {
            DEException.throwException("Cannot generate refresh token: userId is null");
        }
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + REFRESH_TOKEN_TTL_MS;
        RefreshEntry entry = new RefreshEntry(userId, oid, expireAt);
        CacheUtils.put(CacheConstant.CommonCacheConstant.REFRESH_TOKENS_CACHE, refreshToken, entry, 7L, TimeUnit.DAYS);
        userRefreshTokens.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(refreshToken);
        return refreshToken;
    }

    /**
     * 使用 Refresh Token 换发新的 Access Token
     */
    public TokenVO refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            DEException.throwException("Refresh token 不能为空");
        }
        Object cached = CacheUtils.get(CacheConstant.CommonCacheConstant.REFRESH_TOKENS_CACHE, refreshToken);
        if (cached == null) {
            DEException.throwException("Refresh token 无效或已过期");
        }
        if (!(cached instanceof RefreshEntry)) {
            DEException.throwException("Refresh token 数据格式错误");
        }
        RefreshEntry entry = (RefreshEntry) cached;
        if (System.currentTimeMillis() > entry.getExpireAt()) {
            revokeRefreshToken(refreshToken);
            DEException.throwException("Refresh token 已过期");
        }

        String secret = TokenUtils.getSecret();
        Algorithm algorithm = Algorithm.HMAC256(secret);
        long expireMillis = loginTimeoutMinutes * 60 * 1000L;
        Date expiresAt = new Date(System.currentTimeMillis() + expireMillis);

        JWTCreator.Builder builder = JWT.create()
                .withClaim("uid", entry.getUserId())
                .withClaim("oid", entry.getOid())
                .withExpiresAt(expiresAt);
        String newAccessToken = builder.sign(algorithm);

        TokenVO vo = new TokenVO(newAccessToken, expiresAt.getTime());
        // 重新颁发 refresh token（滚动续期），并撤销旧的，提升安全性
        Long userId = entry.getUserId();
        Long oid = entry.getOid();
        revokeRefreshToken(refreshToken);
        String newRefreshToken = generateRefreshToken(userId, oid);
        vo.setRefreshToken(newRefreshToken);
        return vo;
    }

    /**
     * 撤销单个 Refresh Token
     */
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            return;
        }
        Object cached = CacheUtils.get(CacheConstant.CommonCacheConstant.REFRESH_TOKENS_CACHE, refreshToken);
        CacheUtils.keyRemove(CacheConstant.CommonCacheConstant.REFRESH_TOKENS_CACHE, refreshToken);
        if (cached instanceof RefreshEntry entry) {
            Set<String> tokens = userRefreshTokens.get(entry.getUserId());
            if (tokens != null) {
                tokens.remove(refreshToken);
            }
        }
    }

    /**
     * 撤销指定用户的所有 Refresh Token
     */
    public void revokeAllByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        Set<String> tokens = userRefreshTokens.remove(userId);
        if (tokens == null) {
            return;
        }
        for (String token : tokens) {
            CacheUtils.keyRemove(CacheConstant.CommonCacheConstant.REFRESH_TOKENS_CACHE, token);
        }
    }
}
