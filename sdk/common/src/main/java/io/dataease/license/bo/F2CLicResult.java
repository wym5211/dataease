package io.dataease.license.bo;

/**
 * 社区版许可证结果类
 * 简化实现，始终返回有效状态
 */
public class F2CLicResult {

    /**
     * 许可证状态
     */
    public enum Status {
        valid,      // 有效
        expired,    // 已过期
        invalid     // 无效
    }

    private Status status = Status.valid;
    private String message = "社区版许可证";

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 无许可证记录
     * @return 返回无效状态的许可证结果
     */
    public static F2CLicResult noRecord() {
        F2CLicResult result = new F2CLicResult();
        result.setStatus(Status.invalid);
        result.setMessage("无许可证记录");
        return result;
    }

    /**
     * 获取许可证对象
     * @return F2CLicResult (社区版返回有效许可证)
     */
    public static F2CLicResult get() {
        F2CLicResult result = new F2CLicResult();
        result.setStatus(Status.valid);
        result.setMessage("社区版许可证");
        return result;
    }
}
