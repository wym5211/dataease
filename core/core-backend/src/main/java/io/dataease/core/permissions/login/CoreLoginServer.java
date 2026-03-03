package io.dataease.core.permissions.login;

import io.dataease.api.permissions.login.api.LoginApi;
import io.dataease.api.permissions.login.dto.*;
import io.dataease.api.permissions.login.vo.*;
import io.dataease.api.permissions.user.dto.ModifyPwdRequest;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.auth.vo.TokenVO;
import io.dataease.exception.DEException;
import io.dataease.system.dao.auto.entity.SysUser;
import io.dataease.system.dao.auto.mapper.SysUserMapper;
import io.dataease.utils.RsaUtils;
import io.dataease.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;

@Service("loginServer")
@Primary
@ConditionalOnExpression("'${spring.profiles.active:standalone}'.contains('standalone') || '${spring.profiles.active:standalone}'.contains('distributed')")
@RestController
public class CoreLoginServer implements LoginApi {

    @Autowired
    private SysUserMapper sysUserMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
        
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", name);
        SysUser user = sysUserMapper.selectOne(query);
        
        if (user == null) {
            DEException.throwException("User not found or password incorrect");
        }
        
        boolean matches = false;
        if (user.getPassword() != null && (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$"))) {
             matches = passwordEncoder.matches(pwd, user.getPassword());
        } else {
             if (pwd.equals(user.getPassword())) {
                 matches = true;
                 user.setPassword(passwordEncoder.encode(pwd));
                 sysUserMapper.updateById(user);
             }
        }
        
        if (!matches) {
             DEException.throwException("User not found or password incorrect");
        }
        
        if (user.getStatus() != null && user.getStatus() == 0) {
             DEException.throwException("User is disabled");
        }
        
        TokenUserBO tokenUserBO = new TokenUserBO();
        tokenUserBO.setUserId(user.getId());
        tokenUserBO.setDefaultOid(user.getDeptId() != null ? user.getDeptId() : 0L); 
        
        String secret = TokenUtils.getSecret();
        Algorithm algorithm = Algorithm.HMAC256(secret);
        JWTCreator.Builder builder = JWT.create();
        builder.withClaim("uid", user.getId());
        builder.withClaim("oid", tokenUserBO.getDefaultOid());
        
        String token = builder.sign(algorithm);
        
        return new TokenVO(token, 0L);
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
}
