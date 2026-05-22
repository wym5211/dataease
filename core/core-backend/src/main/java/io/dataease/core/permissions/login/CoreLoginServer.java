package io.dataease.core.permissions.login;

import io.dataease.api.permissions.login.api.LoginApi;
import io.dataease.api.permissions.login.dto.*;
import io.dataease.api.permissions.login.vo.*;
import io.dataease.api.permissions.user.dto.ModifyPwdRequest;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.auth.service.TokenBlacklistService;
import io.dataease.auth.service.TokenRefreshService;
import io.dataease.auth.vo.TokenVO;
import io.dataease.exception.DEException;
import io.dataease.system.dao.auto.entity.SysUser;
import io.dataease.system.dao.auto.mapper.SysUserMapper;
import io.dataease.utils.AuthUtils;
import io.dataease.utils.RsaUtils;
import io.dataease.utils.ServletUtils;
import io.dataease.utils.TokenUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service("loginServer")
@Primary
@ConditionalOnExpression("'${spring.profiles.active:standalone}'.contains('standalone') || '${spring.profiles.active:standalone}'.contains('distributed')")
@RestController
public class CoreLoginServer implements LoginApi {

    private static final Logger log = LoggerFactory.getLogger(CoreLoginServer.class);

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private TokenRefreshService tokenRefreshService;

    @Value("${dataease.login_timeout:2880}")
    private long loginTimeout;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final ConcurrentHashMap<String, AtomicInteger> loginFailCount = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lockTime = new ConcurrentHashMap<>();
    private static final int MAX_FAIL_COUNT = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    private void checkLoginRate(String name) {
        Long lockedAt = lockTime.get(name);
        if (lockedAt != null) {
            if (System.currentTimeMillis() - lockedAt < LOCK_DURATION_MS) {
                DEException.throwException("Account temporarily locked, please try again later");
            } else {
                lockTime.remove(name);
                loginFailCount.remove(name);
            }
        }
    }

    private void recordLoginFail(String name) {
        AtomicInteger count = loginFailCount.computeIfAbsent(name, k -> new AtomicInteger(0));
        if (count.incrementAndGet() >= MAX_FAIL_COUNT) {
            lockTime.put(name, System.currentTimeMillis());
        }
    }

    private void clearLoginFail(String name) {
        loginFailCount.remove(name);
        lockTime.remove(name);
    }

    @Override
    public TokenVO localLogin(PwdLoginDTO dto) {
        String name = dto.getName();
        String pwd = dto.getPwd();
        
        try {
             if (name != null && name.length() > 20) name = RsaUtils.decryptStr(name);
        } catch(Exception e) {
        }
        
        try {
             if (pwd != null && pwd.length() > 20) pwd = RsaUtils.decryptStr(pwd);
        } catch(Exception e) {
        }

        checkLoginRate(name);

        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", name);
        SysUser user = sysUserMapper.selectOne(query);

        if (user == null) {
            recordLoginFail(name);
            DEException.throwException("User not found or password incorrect");
        }
        
        boolean matches = false;
        if (user.getPassword() != null && (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$"))) {
             matches = passwordEncoder.matches(pwd, user.getPassword());
        } else {
             DEException.throwException("User not found or password incorrect");
        }

        if (!matches) {
            recordLoginFail(name);
            DEException.throwException("User not found or password incorrect");
        }

        clearLoginFail(name);
        
        if (user.getStatus() != null && user.getStatus() == 0) {
             DEException.throwException("User is disabled");
        }
        
        TokenUserBO tokenUserBO = new TokenUserBO();
        tokenUserBO.setUserId(user.getId());
        tokenUserBO.setDefaultOid(user.getDeptId() != null ? user.getDeptId() : 0L); 
        
        String secret = TokenUtils.getSecret();
        Algorithm algorithm = Algorithm.HMAC256(secret);
        Date expiresAt = new Date(System.currentTimeMillis() + loginTimeout * 60 * 1000L);
        JWTCreator.Builder builder = JWT.create();
        builder.withClaim("uid", user.getId());
        builder.withClaim("oid", tokenUserBO.getDefaultOid());
        builder.withExpiresAt(expiresAt);

        String token = builder.sign(algorithm);

        String refreshToken = tokenRefreshService.generateRefreshToken(user.getId(), tokenUserBO.getDefaultOid());
        setRefreshTokenHeader(ServletUtils.response(), refreshToken);

        return new TokenVO(token, expiresAt.getTime(), refreshToken);
    }

    @Override
    public TokenVO refreshAccess(@RequestParam("refreshToken") String refreshToken) {
        TokenVO vo = tokenRefreshService.refreshAccessToken(refreshToken);
        if (vo != null) {
            setRefreshTokenHeader(ServletUtils.response(), vo.getRefreshToken());
        }
        return vo;
    }

    @Override
    public TokenVO refresh() {
        return null;
    }

    @Override
    public TokenVO platformLogin(Integer origin) {
        return null;
    }

    @Override
    public void logout() {
        // 1. 将当前 access token 加入黑名单
        String token = ServletUtils.getToken();
        if (token != null && !token.isBlank()) {
            tokenBlacklistService.blacklist(token);
        }
        // 2. 撤销该用户的所有 refresh token
        try {
            TokenUserBO user = AuthUtils.getUser();
            if (user != null && user.getUserId() != null) {
                tokenRefreshService.revokeAllByUserId(user.getUserId());
            }
        } catch (Exception e) {
            log.warn("Failed to revoke refresh tokens on logout: {}", e.getMessage());
        }
    }

    @Override
    public MfaQrVO mfaQr(Long id) {
        return null;
    }

    @Override
    public TokenVO mfaLogin(MfaLoginDTO dto) {
        return null;
    }

    @Override
    public void modifyInvalidPwd(ModifyPwdRequest request) {

    }

    private void setRefreshTokenHeader(HttpServletResponse response, String refreshToken) {
        if (response != null && refreshToken != null) {
            response.setHeader("X-Refresh-Token", refreshToken);
            response.addHeader("Access-Control-Expose-Headers", "X-Refresh-Token");
        }
    }
}
