package io.dataease.auth.config;

import io.dataease.commons.utils.JwtUtils;
import io.dataease.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class JwtConfig {

    @Value("${dataease.jwt.secret:DataEase_Secret_Key_2024}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        TokenUtils.setSecret(jwtSecret);
        JwtUtils.setSecret(jwtSecret);
    }
}
