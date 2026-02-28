package io.dataease.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.exception.DEException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class TokenUtils {


    private static final String SECRET = "DataEase_Secret_Key_2024";

    public static String getSecret() {
        return SECRET;
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
        try {
            com.auth0.jwt.algorithms.Algorithm algorithm = com.auth0.jwt.algorithms.Algorithm.HMAC256(SECRET);
            JWT.require(algorithm).build().verify(token);
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
        Long userId = jwt.getClaim("uid").asLong();
        Long oid = jwt.getClaim("oid").asLong();
        if (ObjectUtils.isEmpty(userId)) {
            DEException.throwException("link token格式错误！");
        }
        return new TokenUserBO(userId, oid);
    }
}
