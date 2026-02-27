package io.dataease.commons.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import java.util.Date;
import java.util.Map;

public class JwtUtils {

    private static final String SECRET = "DataEase_Secret_Key_2024";
    private static final long EXPIRATION = 3600 * 1000 * 24; // 24小时

    public static String createToken(String username, Map<String, Object> claims) {
        return JWT.create()
                .withSubject(username)
                .withPayload(claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION))
                .sign(Algorithm.HMAC256(SECRET));
    }

    public static DecodedJWT verifyToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    public static String getUsername(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt != null ? jwt.getSubject() : null;
    }
}
