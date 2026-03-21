import CryptoJS from 'crypto-js/crypto-js'
import JSEncrypt from 'jsencrypt/bin/jsencrypt.min'
import { Base64 } from 'js-base64'
import { useCache } from '@/hooks/web/useCache'
import { useAppStoreWithOut } from '@/store/modules/app'

const appStore = useAppStoreWithOut()

const { wsCache } = useCache()

const rsaKey = '-pk_separator-'
const crypt = new JSEncrypt()

/**
 * AES解密（使用固定IV，用于解密后端传来的数据）
 * 注意：此函数保持使用固定IV以兼容现有后端实现
 */
const aesDecrypt = (word, keyStr) => {
  const keyHex = CryptoJS.enc.Utf8.parse(keyStr)
  // 保持固定IV以兼容后端
  const ivHex = CryptoJS.enc.Utf8.parse('0000000000000000')
  const decrypt = CryptoJS.AES.decrypt(word, keyHex, {
    iv: ivHex,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  return decrypt.toString(CryptoJS.enc.Utf8)
}

export const rsaEncryp = word => {
  const separator = Base64.encodeURI(rsaKey) + '='
  const dekey = wsCache.get(appStore.getDekey)
  const keyArray = dekey.split(separator)
  const k1 = keyArray[0]
  const k2 = keyArray[1]
  const pk = aesDecrypt(k1, k2)
  crypt.setKey(pk)
  return crypt.encrypt(word)
}

/**
 * 对称解密（使用固定IV，用于解密后端传来的数据）
 * 注意：此函数保持使用固定IV以兼容现有后端实现
 */
export const symmetricDecrypt = (data, keyStr) => {
  // 保持固定IV以兼容后端
  const iv = CryptoJS.enc.Utf8.parse('0000000000000000')
  const key = CryptoJS.enc.Base64.parse(keyStr)
  const decodedCiphertext = CryptoJS.enc.Base64.parse(data)
  const decrypted = CryptoJS.AES.decrypt({ ciphertext: decodedCiphertext }, key, {
    iv: iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  return decrypted.toString(CryptoJS.enc.Utf8)
}

/**
 * AES加密（使用随机IV，用于前端加密发送到后端的数据）
 * IV会被附加到密文前面，格式：IV(16字节) + 密文
 *
 * @param plaintext 明文
 * @param keyStr 密钥字符串
 * @returns Base64编码的 IV+密文
 */
export const aesEncryptWithRandomIV = (plaintext: string, keyStr: string): string => {
  const key = CryptoJS.enc.Utf8.parse(keyStr)
  // 生成随机IV（16字节）
  const iv = CryptoJS.lib.WordArray.random(16)

  const encrypted = CryptoJS.AES.encrypt(plaintext, key, {
    iv: iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })

  // 将IV和密文拼接：IV + 密文
  const combined = iv.concat(encrypted.ciphertext)
  return CryptoJS.enc.Base64.stringify(combined)
}

/**
 * AES解密（支持随机IV）
 * 从密文中提取IV并解密
 *
 * @param ciphertext Base64编码的 IV+密文
 * @param keyStr 密钥字符串
 * @returns 明文
 */
export const aesDecryptWithRandomIV = (ciphertext: string, keyStr: string): string => {
  const key = CryptoJS.enc.Utf8.parse(keyStr)
  const combined = CryptoJS.enc.Base64.parse(ciphertext)

  // 提取IV（前16字节）
  const iv = CryptoJS.lib.WordArray.create(combined.words.slice(0, 4), 16)
  // 提取密文（剩余部分）
  const encrypted = CryptoJS.lib.WordArray.create(combined.words.slice(4), combined.sigBytes - 16)

  const decrypted = CryptoJS.AES.decrypt(
    { ciphertext: encrypted } as CryptoJS.lib.CipherParamsData,
    key,
    {
      iv: iv,
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    }
  )

  return decrypted.toString(CryptoJS.enc.Utf8)
}
