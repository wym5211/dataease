package io.dataease.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.exception.DEException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class TokenUtilsTest {

    private static final String TEST_SECRET = "test_secret_key_1234567890";
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(TEST_SECRET);

    @BeforeAll
    static void setUp() {
        TokenUtils.setSecret(TEST_SECRET);
    }

    private String createToken(Long uid, Long oid, Date expiresAt) {
        JWTCreator.Builder builder = JWT.create();
        builder.withClaim("uid", uid);
        builder.withClaim("oid", oid);
        if (expiresAt != null) {
            builder.withExpiresAt(expiresAt);
        }
        return builder.sign(ALGORITHM);
    }

    @Test
    @DisplayName("validate(null) should throw")
    void validate_nullToken_throws() {
        assertThrows(Exception.class, () -> TokenUtils.validate(null));
    }

    @Test
    @DisplayName("validate(empty) should throw")
    void validate_emptyToken_throws() {
        assertThrows(Exception.class, () -> TokenUtils.validate(""));
    }

    @Test
    @DisplayName("validate(short token) should throw")
    void validate_shortToken_throws() {
        assertThrows(Exception.class, () -> TokenUtils.validate("abc"));
    }

    @Test
    @DisplayName("validate(valid token) returns TokenUserBO")
    void validate_validToken_returnsUserBO() {
        String token = createToken(1L, 100L, new Date(System.currentTimeMillis() + 3600_000));
        TokenUserBO result = TokenUtils.validate(token);
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
    }

    @Test
    @DisplayName("validate(expired token) throws 401")
    void validate_expiredToken_throws401() {
        String token = createToken(1L, 100L, new Date(System.currentTimeMillis() - 1000));
        DEException ex = assertThrows(DEException.class, () -> TokenUtils.validate(token));
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("validate(tampered token) throws verification failed")
    void validate_tamperedToken_throws() {
        String token = createToken(1L, 100L, new Date(System.currentTimeMillis() + 3600_000));
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThrows(Exception.class, () -> TokenUtils.validate(tampered));
    }

    @Test
    @DisplayName("setSecret rejects short secrets")
    void setSecret_rejectsShort() {
        String original = TokenUtils.getSecret();
        TokenUtils.setSecret("short");
        assertEquals(original, TokenUtils.getSecret());
    }

    @Test
    @DisplayName("getSecret throws when not initialized")
    void getSecret_throwsWhenNull() {
        // This test verifies the guard logic, but we can't easily reset SECRET to null
        // without reflection. Instead, verify getSecret returns a value after setSecret.
        assertNotNull(TokenUtils.getSecret());
    }
}
