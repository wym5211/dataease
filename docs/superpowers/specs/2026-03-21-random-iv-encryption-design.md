# 前后端加解密随机IV改造设计方案

## 1. 背景与目标

当前 DataEase 前后端使用固定 IV (`0000000000000000`) 进行 AES 加解密，存在安全风险：
- 相同明文每次加密结果相同，容易被模式分析
- 无法抵御重放攻击

**目标**：将前后端加解密改为随机IV，提升安全性，同时保留原有函数以便回退。

## 2. 设计原则

- 新旧函数并存，通过版本标识切换
- 随机IV存储格式：IV(16字节) + 密文，Base64编码
- 历史数据通过迁移脚本一次性重加密
- 回退方案：数据库备份 + 代码回退

## 3. 数据格式

### V1 固定IV格式
```
密文 (Base64)
```

### V2 随机IV格式
```
IV(16字节) + 密文 → Base64编码
```

## 4. 前端改动

### 4.1 文件：`core/core-frontend/src/utils/encryption.ts`

**新增函数**：

```typescript
/**
 * 对称加密（使用随机IV）
 * IV会被附加到密文前面，格式：IV(16字节) + 密文
 */
export const symmetricEncryptV2 = (data: string, keyStr: string): string => {
  const key = CryptoJS.enc.Utf8.parse(keyStr)
  const iv = CryptoJS.lib.WordArray.random(16)
  const encrypted = CryptoJS.AES.encrypt(data, key, {
    iv: iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  const combined = iv.concat(encrypted.ciphertext)
  return CryptoJS.enc.Base64.stringify(combined)
}

/**
 * 对称解密（支持随机IV V2格式）
 * 从密文中提取IV（前16字节）并解密
 */
export const symmetricDecryptV2 = (ciphertext: string, keyStr: string): string => {
  const key = CryptoJS.enc.Utf8.parse(keyStr)
  const combined = CryptoJS.enc.Base64.parse(ciphertext)
  const iv = CryptoJS.lib.WordArray.create(combined.words.slice(0, 4), 16)
  const encrypted = CryptoJS.lib.WordArray.create(combined.words.slice(4), combined.sigBytes - 16)
  const decrypted = CryptoJS.AES.decrypt(
    { ciphertext: encrypted } as CryptoJS.lib.CipherParamsData,
    key,
    { iv: iv, mode: CryptoJS.mode.CBC, padding: CryptoJS.pad.Pkcs7 }
  )
  return decrypted.toString(CryptoJS.enc.Utf8)
}
```

**保留函数**（兼容用）：
- `aesDecrypt` - 内部用
- `rsaEncryp` - 登录加密
- `symmetricDecrypt` - 兼容V1数据
- `aesEncryptWithRandomIV` - 已有，但未使用
- `aesDecryptWithRandomIV` - 已有，但未使用

### 4.2 调用处改动

| 文件 | 改动 |
|------|------|
| `views/visualized/data/datasource/index.vue` | `symmetricDecrypt` → `symmetricDecryptV2` |
| `views/system/parameter/engine/EngineInfoTemplate.vue` | `symmetricDecrypt` → `symmetricDecryptV2` |
| `views/system/parameter/engine/EngineEdit.vue` | `symmetricDecrypt` → `symmetricDecryptV2` |

## 5. 后端改动

### 5.1 文件：`sdk/common/src/main/java/io/dataease/utils/RsaUtils.java`

**新增函数**：

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

**保留函数**（兼容V1数据）：
- `IV_KEY = "0000000000000000"`
- `symmetricEncrypt`
- `symmetricDecrypt`

### 5.2 数据迁移脚本

**文件**：`core/core-backend/src/main/java/io/dataease/migration/DataMigrationRunner.java`

**迁移范围**：
- 全局扫描使用加密配置的表
- 主要涉及：`datasource`（数据源配置）、`de_engine`（引擎配置）等

**迁移逻辑**：
1. 查询所有使用 V1 格式加密的配置
2. 使用 V2 函数重新加密
3. 更新数据库

**触发方式**：部署前手动执行

## 6. 迁移流程

```
1. 备份数据库
2. 执行迁移脚本（重新加密历史数据）
3. 部署新版本前后端
4. 验证功能正常
```

## 7. 回退方案

- **数据库**：恢复备份
- **代码**：回退到旧版本
- **原因**：旧函数已保留，可直接切换

## 8. 改动文件清单

### 前端
- `core/core-frontend/src/utils/encryption.ts` - 新增 V2 函数
- `core/core-frontend/src/views/visualized/data/datasource/index.vue` - 改用 V2
- `core/core-frontend/src/views/system/parameter/engine/EngineInfoTemplate.vue` - 改用 V2
- `core/core-frontend/src/views/system/parameter/engine/EngineEdit.vue` - 改用 V2

### 后端
- `sdk/common/src/main/java/io/dataease/utils/RsaUtils.java` - 新增 V2 函数
- `core/core-backend/src/main/java/io/dataease/migration/DataMigrationRunner.java` - 新增迁移脚本

## 9. 风险与注意事项

1. **迁移过程需要停机或低峰期执行**
2. **迁移前必须备份数据库**
3. **迁移脚本需要先在测试环境验证**
4. **回退时需同时回退数据库和代码**
