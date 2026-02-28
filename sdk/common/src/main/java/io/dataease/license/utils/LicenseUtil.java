package io.dataease.license.utils;

/**
 * 社区版许可证工具类
 * 绕过企业版许可证验证，始终返回有效
 */
public class LicenseUtil {

    /**
     * 许可证始终有效
     * @return true
     */
    public static boolean licenseValid() {
        return true;
    }

    /**
     * 获取许可证功能状态
     * @param feature 功能名称
     * @return true (所有功能都可用)
     */
    public static boolean functionEnabled(String feature) {
        return true;
    }

    /**
     * 获取许可证类型
     * @return "community" 或其他标识
     */
    public static String getLicenseType() {
        return "community";
    }
}
