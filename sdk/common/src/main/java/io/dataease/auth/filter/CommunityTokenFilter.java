package io.dataease.auth.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.auth.config.SubstituleLoginConfig;
import io.dataease.license.utils.LicenseUtil;
import io.dataease.utils.*;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class CommunityTokenFilter implements Filter {

    private static final String headName = "DE-GATEWAY-FLAG";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // Skip all validation in desktop mode or community edition
        // TokenFilter (order=0) already handles the main token validation
        if (ModelUtils.isDesktop() || !LicenseUtil.licenseValid()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Enterprise edition token validation logic here
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void sendResponseEntity(HttpServletResponse httpResponse, ResponseEntity<String> responseEntity) throws IOException {
        HttpHeaders headers = responseEntity.getHeaders();
        HttpStatusCode statusCode = responseEntity.getStatusCode();
        httpResponse.setStatus(statusCode.value());
        for (String name : headers.keySet()) {
            httpResponse.setHeader(name, headers.getFirst(name));
        }
        httpResponse.getWriter().write(Objects.requireNonNull(responseEntity.getBody()));
    }
}
