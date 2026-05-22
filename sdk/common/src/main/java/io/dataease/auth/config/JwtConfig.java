package io.dataease.auth.config;

import io.dataease.utils.TokenUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtConfig {

    private static final Logger logger = LoggerFactory.getLogger(JwtConfig.class);
    private static final String SECRET_FILENAME = "jwt-secret";
    private static final Set<PosixFilePermission> SECRET_FILE_PERMS = Set.of(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE
    );

    @Value("${dataease.jwt.secret:}")
    private String jwtSecret;

    @Value("${dataease.path.ehcache:./cache}")
    private String cachePath;

    @PostConstruct
    public void init() {
        if (StringUtils.isBlank(jwtSecret) || jwtSecret.length() < 16) {
            jwtSecret = loadOrPersistSecret();
        }
        TokenUtils.setSecret(jwtSecret);
    }

    private String loadOrPersistSecret() {
        try {
            Path dir = Paths.get(cachePath);
            Path secretFile = dir.resolve(SECRET_FILENAME);
            if (Files.exists(secretFile)) {
                String persisted = Files.readString(secretFile).trim();
                if (StringUtils.isNotBlank(persisted) && persisted.length() >= 16) {
                    setOwnerOnlyPermissions(secretFile);
                    logger.info("JWT secret loaded from persisted file");
                    return persisted;
                }
            }
            String newSecret = UUID.randomUUID().toString();
            Files.createDirectories(dir);
            // 原子写入：先写临时文件再移动
            Path tmpFile = Files.createTempFile(dir, "jwt-secret", ".tmp");
            try {
                Files.writeString(tmpFile, newSecret);
                setOwnerOnlyPermissions(tmpFile);
                Files.move(tmpFile, secretFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                try { Files.deleteIfExists(tmpFile); } catch (Exception ignored) {}
                throw e;
            }
            logger.warn("Generated new JWT secret and persisted to {}. Recommend setting DE_JWT_SECRET environment variable for production.", secretFile);
            return newSecret;
        } catch (Exception e) {
            logger.warn("Failed to persist JWT secret to file, using in-memory random secret: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }

    private void setOwnerOnlyPermissions(Path file) {
        try {
            Files.setPosixFilePermissions(file, SECRET_FILE_PERMS);
        } catch (UnsupportedOperationException ignored) {
            // Windows 不支持 POSIX 权限
        } catch (Exception e) {
            logger.debug("Could not set file permissions: {}", e.getMessage());
        }
    }
}
