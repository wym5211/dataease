package io.dataease.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.exception.DEException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class TokenUtils {


    private static volatile String SECRET = null;
    private static volatile String linkTokenSecret = null;

    public static String getSecret() {
        if (SECRET == null) {
            throw new IllegalStateException("TokenUtils not initialized - secret not set");
        }
        return SECRET;
    }

    public static void setSecret(String secret) {
        if (StringUtils.isNotBlank(secret) && secret.length() >= 16) {
            SECRET = secret;
        }
    }

    public static void setLinkTokenSecret(String secret) {
        if (StringUtils.isNotBlank(secret) && secret.length() >= 16) {
            linkTokenSecret = secret;
        }
    }

    public static TokenUserBO userBOByToken(String token) {
        DecodedJWT jwt = JWT.decode(token);
        Long userId = jwt.getClaim("uid").asLong();
        Long oid = jwt.getClaim("oid").asLong();
        if (ObjectUtils.isEmpty(userId)) {
            DEException.throwException("token格式错误！");
        }
        return new TokenUserBO(userId, oid);
    }

    public static TokenUserBO validate(String token) {
        if (StringUtils.isBlank(token)) {
            String uri = ServletUtils.request().getRequestURI();
            DEException.throwException("token is empty for uri {" + uri + "}");
        }
        if (StringUtils.length(token) < 100) {
            DEException.throwException("token is invalid");
        }
        // 预检查过期时间，以便返回与“签名失败”区分的错误信息
        try {
            DecodedJWT decoded = JWT.decode(token);
            if (decoded.getExpiresAt() != null && decoded.getExpiresAt().before(new java.util.Date())) {
                DEException.throwException(401, "token expired");
            }
        } catch (DEException e) {
            throw e;
        } catch (Exception ignored) {
            // 解码异常交给下面的 verify 处理
        }
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            JWT.require(algorithm).build().verify(token);
        } catch (TokenExpiredException e) {
            DEException.throwException(401, "token expired");
        } catch (Exception e) {
             DEException.throwException("token verification failed");
        }
        return userBOByToken(token);
    }


    public static TokenUserBO validateLinkToken(String linkToken) {
        if (StringUtils.isBlank(linkToken)) {
            String uri = ServletUtils.request().getRequestURI();
            DEException.throwException("link token is empty for uri {" + uri + "}");
        }
        if (StringUtils.length(linkToken) < 100) {
            DEException.throwException("token is invalid");
        }
        DecodedJWT jwt = JWT.decode(linkToken);
        if (jwt == null) {
            DEException.throwException("link token 格式无效");
        }
        // 签名验证：使用默认密码尝试，资源特定密码由 DeLinkAop 后续验证
        String secret = linkTokenSecret;
        if (secret != null) {
            try {
                Algorithm algorithm = Algorithm.HMAC256(secret);
                JWT.require(algorithm).build().verify(linkToken);
            } catch (TokenExpiredException e) {
                DEException.throwException("link token 已过期");
            } catch (Exception ignored) {
                // 签名不匹配默认密码，可能使用资源特定密码，由 DeLinkAop 完整验证
            }
        }
        // 过期检查
        if (jwt.getExpiresAt() != null && jwt.getExpiresAt().before(new java.util.Date())) {
            DEException.throwException("link token 已过期");
        }
        Long userId = jwt.getClaim("uid").asLong();
        Long oid = jwt.getClaim("oid").asLong();
        if (ObjectUtils.isEmpty(userId)) {
            DEException.throwException("link token格式错误！");
        }
        return new TokenUserBO(userId, oid);
    }
}
