package io.dataease.license.utils;

import io.dataease.license.bo.F2CLicResult;

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

    /**
     * 验证许可证
     * @return true (社区版许可证始终有效)
     */
    public static boolean validate() {
        return true;
    }

    /**
     * 获取许可证对象
     * @return F2CLicResult (社区版返回有效许可证)
     */
    public static F2CLicResult get() {
        F2CLicResult result = new F2CLicResult();
        result.setStatus(F2CLicResult.Status.valid);
        result.setMessage("社区版许可证");
        return result;
    }
}
