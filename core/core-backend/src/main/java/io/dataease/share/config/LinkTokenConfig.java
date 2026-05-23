package io.dataease.share.config;

import io.dataease.share.util.LinkTokenUtil;
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
public class LinkTokenConfig {

    private static final Logger logger = LoggerFactory.getLogger(LinkTokenConfig.class);
    private static final String SECRET_FILENAME = "link-secret";
    private static final Set<PosixFilePermission> SECRET_FILE_PERMS = Set.of(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE
    );

    @Value("${dataease.path.ehcache:./cache}")
    private String cachePath;

    @PostConstruct
    public void init() {
        String secret = loadOrPersistSecret();
        LinkTokenUtil.setDefaultPwd(secret);
    }

    private String loadOrPersistSecret() {
        try {
            Path dir = Paths.get(cachePath);
            Path secretFile = dir.resolve(SECRET_FILENAME);
            if (Files.exists(secretFile)) {
                String persisted = Files.readString(secretFile).trim();
                if (StringUtils.isNotBlank(persisted) && persisted.length() >= 16) {
                    setOwnerOnlyPermissions(secretFile);
                    logger.info("Link token secret loaded from persisted file");
                    return persisted;
                }
            }
            String newSecret = "DE_LINK_" + UUID.randomUUID().toString();
            Files.createDirectories(dir);
            Path tmpFile = Files.createTempFile(dir, "link-secret", ".tmp");
            try {
                Files.writeString(tmpFile, newSecret);
                setOwnerOnlyPermissions(tmpFile);
                try {
                    Files.move(tmpFile, secretFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                    Files.move(tmpFile, secretFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                try { Files.deleteIfExists(tmpFile); } catch (Exception ignored) {}
                throw e;
            }
            logger.info("Generated new link token secret and persisted to {}", secretFile);
            return newSecret;
        } catch (Exception e) {
            logger.warn("Failed to persist link token secret, using in-memory: {}", e.getMessage());
            return "DE_LINK_" + UUID.randomUUID().toString();
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
