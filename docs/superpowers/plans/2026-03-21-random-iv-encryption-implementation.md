# 前后端加解密随机IV改造实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将前后端加解密从固定IV改为随机IV，提升安全性

**Architecture:** 新旧函数并存，V2使用随机IV+密文格式，保留V1函数兼容历史数据

**Tech Stack:** Java (Spring Boot), Vue 3, CryptoJS

---

## 文件结构

### 后端
- **Modify**: `sdk/common/src/main/java/io/dataease/utils/RsaUtils.java:207-254` - 新增 V2 加解密函数
- **Create**: `core/core-backend/src/main/java/io/dataease/migration/DataMigrationRunner.java` - 数据迁移脚本
- **Modify**: `core/core-backend/src/main/java/io/dataease/datasource/manage/DataSourceManage.java` - 写入改用 V2
- **Modify**: `core/core-backend/src/main/java/io/dataease/datasource/manage/EngineManage.java` - 写入改用 V2

### 前端
- **Modify**: `core/core-frontend/src/utils/encryption.ts:66-110` - 新增 V2 函数
- **Modify**: `core/core-frontend/src/views/visualized/data/datasource/index.vue` - 改用 V2 解密
- **Modify**: `core/core-frontend/src/views/system/parameter/engine/EngineInfoTemplate.vue` - 改用 V2 解密
- **Modify**: `core/core-frontend/src/views/system/parameter/engine/EngineEdit.vue` - 改用 V2 解密

---

## Task 1: 后端 RsaUtils 新增 V2 函数

**Files:**
- Modify: `sdk/common/src/main/java/io/dataease/utils/RsaUtils.java`

- [ ] **Step 1: 添加 symmetricEncryptV2 函数**

在 `RsaUtils.java` 第254行后添加：

```java
/**
 * 对称加密 V2（随机IV）
 * IV(16字节) + 密文，整体Base64编码
 */
public static String symmetricEncryptV2(String data) {
    try {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            Base64.getDecoder().decode(generateSymmetricKey()), ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] ciphertext = cipher.doFinal(data.getBytes("UTF-8"));
        // 合并 IV + 密文
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

- [ ] **Step 2: 添加 symmetricDecryptV2 函数**

在 `symmetricEncryptV2` 后添加：

```java
/**
 * 对称解密 V2（随机IV）
 * 从密文提取前16字节作为IV
 */
public static String symmetricDecryptV2(String data) {
    try {
        byte[] combined = Base64.getDecoder().decode(data);
        // 提取IV（前16字节）
        byte[] iv = new byte[16];
        System.arraycopy(combined, 0, iv, 0, 16);
        // 提取密文
        byte[] ciphertext = new byte[combined.length - 16];
        System.arraycopy(combined, 16, ciphertext, 0, ciphertext.length);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            Base64.getDecoder().decode(generateSymmetricKey()), ALGORITHM);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] decryptedText = cipher.doFinal(ciphertext);
        return new String(decryptedText, "UTF-8");
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

- [ ] **Step 3: 验证编译**

```bash
cd core/core-backend
mvn compile -pl sdk/common -q
```

- [ ] **Step 4: 提交**

```bash
git add sdk/common/src/main/java/io/dataease/utils/RsaUtils.java
git commit -m "feat(security): 新增随机IV加解密函数 symmetricEncryptV2/symmetricDecryptV2"
```

---

## Task 2: 前端 encryption.ts 复用已有 V2 函数

**Files:**
- Modify: `core/core-frontend/src/utils/encryption.ts`

**说明**：前端已有 `aesEncryptWithRandomIV` (第66行) 和 `aesDecryptWithRandomIV` (第90行)，实现相同功能。Task 3-5 中将直接复用这两个已有函数，无需新增。

**与 spec 对应关系**：
- `aesEncryptWithRandomIV` = spec 中的 `symmetricEncryptV2`
- `aesDecryptWithRandomIV` = spec 中的 `symmetricDecryptV2`

**无需执行任何代码改动，跳过此 Task，直接执行 Task 3。**

---

## Task 3: 前端 datasource/index.vue 改用 V2

**Files:**
- Modify: `core/core-frontend/src/views/visualized/data/datasource/index.vue`

- [ ] **Step 1: 更新 import 语句**

将：
```typescript
import { symmetricDecrypt } from '@/utils/encryption'
```

改为：
```typescript
import { symmetricDecrypt, aesDecryptWithRandomIV } from '@/utils/encryption'
```

- [ ] **Step 2: 搜索所有解密调用处并替换**

将 `symmetricDecrypt` 替换为 `aesDecryptWithRandomIV`：
- 第712行附近
- 第715行附近
- 第718行附近
- 第850行附近
- 第853行附近
- 第856行附近
- 第923行附近
- 第926行附近
- 第929行附近

使用 `replace_all` 将所有 `symmetricDecrypt(` 替换为 `aesDecryptWithRandomIV(`。

- [ ] **Step 3: 验证编译**

```bash
cd core/core-frontend
npm run build:base 2>&1 | head -50
```

- [ ] **Step 4: 提交**

```bash
git add core/core-frontend/src/views/visualized/data/datasource/index.vue
git commit -m "feat(security): 数据源页面改用随机IV解密"
```

---

## Task 4: 前端 EngineInfoTemplate.vue 改用 V2

**Files:**
- Modify: `core/core-frontend/src/views/system/parameter/engine/EngineInfoTemplate.vue`

- [ ] **Step 1: 更新 import 语句**

将：
```typescript
import { symmetricDecrypt } from '@/utils/encryption'
```

改为：
```typescript
import { symmetricDecrypt, aesDecryptWithRandomIV } from '@/utils/encryption'
```

- [ ] **Step 2: 更新调用处**

第74行附近：
将 `JSON.parse(symmetricDecrypt(configuration, response.data))` 改为 `JSON.parse(aesDecryptWithRandomIV(configuration, response.data))`

- [ ] **Step 3: 验证编译**

```bash
cd core/core-frontend
npm run build:base 2>&1 | head -50
```

- [ ] **Step 4: 提交**

```bash
git add core/core-frontend/src/views/system/parameter/engine/EngineInfoTemplate.vue
git commit -m "feat(security): 引擎模板页面改用随机IV解密"
```

---

## Task 5: 前端 EngineEdit.vue 改用 V2

**Files:**
- Modify: `core/core-frontend/src/views/system/parameter/engine/EngineEdit.vue`

- [ ] **Step 1: 更新 import 语句**

将：
```typescript
import { symmetricDecrypt } from '@/utils/encryption'
```

改为：
```typescript
import { symmetricDecrypt, aesDecryptWithRandomIV } from '@/utils/encryption'
```

- [ ] **Step 2: 更新调用处**

第183行附近：
将 `JSON.parse(symmetricDecrypt(configuration, response.data))` 改为 `JSON.parse(aesDecryptWithRandomIV(configuration, response.data))`

- [ ] **Step 3: 验证编译**

```bash
cd core/core-frontend
npm run build:base 2>&1 | head -50
```

- [ ] **Step 4: 提交**

```bash
git add core/core-frontend/src/views/system/parameter/engine/EngineEdit.vue
git commit -m "feat(security): 引擎编辑页面改用随机IV解密"
```

---

## Task 6: 后端数据迁移脚本

**Files:**
- Create: `core/core-backend/src/main/java/io/dataease/migration/DataMigrationRunner.java`

- [ ] **Step 1: 创建迁移脚本**

```java
package io.dataease.migration;

import io.dataease.utils.RsaUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 数据迁移脚本：将固定IV加密的数据迁移为随机IV加密
 * 使用方式：手动执行，执行后需重启应用
 */
@Component
public class DataMigrationRunner implements CommandLineRunner {

    @Resource
    private JdbcTemplate jdbcTemplate;

    // 需要迁移的表和字段配置
    // 格式：TableName -> [encryptedField1, encryptedField2, ...]
    private static final Map<String, List<String>> MIGRATION_TARGETS = Map.of(
        "datasource", List.of("configuration", "api_configuration", "params"),
        "de_engine", List.of("configuration")
    );

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== 开始加密数据迁移 (V1 -> V2) ===");
        System.out.println("警告：请确保已备份数据库！");

        int totalMigrated = 0;
        int totalSkipped = 0;
        for (Map.Entry<String, List<String>> entry : MIGRATION_TARGETS.entrySet()) {
            String tableName = entry.getKey();
            List<String> fields = entry.getValue();
            for (String field : fields) {
                int[] result = migrateTable(tableName, field);
                totalMigrated += result[0];
                totalSkipped += result[1];
                System.out.println(String.format("表 %s.%s: 迁移 %d 条, 跳过 %d 条", tableName, field, result[0], result[1]));
            }
        }
        System.out.println(String.format("=== 加密数据迁移完成，共迁移 %d 条，跳过 %d 条 ===", totalMigrated, totalSkipped));
    }

    private int[] migrateTable(String tableName, String field) {
        String selectSql = String.format("SELECT id, %s FROM %s WHERE %s IS NOT NULL AND %s != ''", field, tableName, field, field);
        String updateSql = String.format("UPDATE %s SET %s = ? WHERE id = ?", tableName, field);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql);
        int migrated = 0;
        int skipped = 0;
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String encryptedValue = (String) row.get(field);
            try {
                // V1 解密
                String decrypted = RsaUtils.symmetricDecrypt(encryptedValue);
                // V2 重新加密
                String reEncrypted = RsaUtils.symmetricEncryptV2(decrypted);
                jdbcTemplate.update(updateSql, reEncrypted, id);
                migrated++;
            } catch (Exception e) {
                // 如果 V1 解密失败，说明可能已经是 V2 格式或其他情况
                skipped++;
            }
        }
        return new int[]{migrated, skipped};
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd core/core-backend
mvn compile -q
```

- [ ] **Step 3: 提交**

```bash
git add core/core-backend/src/main/java/io/dataease/migration/DataMigrationRunner.java
git commit -m "feat(security): 添加数据迁移脚本 DataMigrationRunner"
```

---

## Task 6b: 后端加密调用点改动

**说明**：迁移后，新的写入操作应使用 V2 函数加密。重点关注 `DataSourceManage.java` 和 `EngineManage.java` 中的保存/更新方法。

**Files:**
- Modify: `core/core-backend/src/main/java/io/dataease/datasource/manage/DataSourceManage.java`
- Modify: `core/core-backend/src/main/java/io/dataease/datasource/manage/EngineManage.java`

- [ ] **Step 1: 搜索加密调用点**

在后端项目中搜索写入相关的加密调用：
```bash
grep -rn "RsaUtils.symmetricEncrypt\|EncryptUtils.aesEncrypt" --include="*.java" core/core-backend/src/main/java/io/dataease/datasource/manage/
```

预期结果：
- `DataSourceManage.java` 中的 `createDataSource` 或 `updateDataSource` 方法
- `EngineManage.java` 中的 `createEngine` 或 `updateEngine` 方法

- [ ] **Step 2: 修改 DataSourceManage.java**

找到保存数据源时加密配置的代码（约在 `save` 或 `create` 方法内），将：
```java
String encrypted = RsaUtils.symmetricEncrypt(configStr);
```
改为：
```java
String encrypted = RsaUtils.symmetricEncryptV2(configStr);
```

- [ ] **Step 3: 修改 EngineManage.java**

类似地修改 `EngineManage.java` 中保存引擎配置时的加密调用。

- [ ] **Step 4: 验证编译**

```bash
cd core/core-backend
mvn clean compile -DskipTests -q
```

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "feat(security): 后端写入改用随机IV加密"
```

---

## Task 7: 前端加密调用点改动

**说明**：根据调研，前端暂无直接对称加密数据源/引擎配置的需求（前端只解密后端返回的数据）。如果后续有写入需求，应使用 `symmetricEncryptV2`。

当前 Task 3-5 已完成解密调用点的改动。

---

## Task 8: 整体验证

- [ ] **Step 1: 后端编译**

```bash
cd core/core-backend
mvn clean compile -DskipTests -q
```

- [ ] **Step 2: 前端编译**

```bash
cd core/core-frontend
npm run build:base 2>&1 | tail -20
```

- [ ] **Step 3: 最终提交**

```bash
git add -A
git commit -m "feat(security): 完成前后端随机IV加解密改造"
```

---

## 迁移执行流程

**重要**：Task 6b 必须先于迁移脚本部署，以确保迁移后新写入的数据格式一致。

```
1. 备份数据库
2. 重新编译并部署后端（包含 Task 6b 改动 - 写入改用 V2）
3. 执行迁移脚本（重新加密历史数据）
4. 重新编译前端
5. 部署验证前端
```

**执行顺序说明**：
- Step 2 (Task 6b): 后端写入改用 V2，确保新数据使用随机IV
- Step 3: 迁移脚本重加密历史数据为 V2
- Step 4-5: 前端使用 V2 解密

---

## 回退方案

如需回退：
1. 恢复数据库备份
2. git revert 最后一次提交
3. 重新部署
