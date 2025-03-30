package org.xhy.interfaces.dto.agent;

import java.util.regex.Pattern;

import jakarta.validation.constraints.NotBlank;
import org.xhy.infrastructure.exception.ParamValidationException;


/**
 * 发布Agent版本请求
 */
public class PublishAgentVersionRequest {
    @NotBlank(message = "版本号不能为空")
    private String versionNumber;
    private String changeLog;

    // 版本号正则表达式，验证x.y.z格式
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    // 构造方法
    public PublishAgentVersionRequest() {
    }

    public PublishAgentVersionRequest(String versionNumber, String changeLog) {
        this.versionNumber = versionNumber;
        this.changeLog = changeLog;
    }

    /**
     * 校验请求参数
     */
    public void validate() {
        // 验证版本号格式
        if (!VERSION_PATTERN.matcher(versionNumber).matches()) {
            throw new ParamValidationException("versionNumber", "版本号必须遵循 x.y.z 格式");
        }

        if (changeLog == null || changeLog.trim().isEmpty()) {
            throw new ParamValidationException("changeLog", "变更日志不能为空");
        }
    }

    // Getter和Setter
    public String getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    /**
     * 比较版本号是否大于给定的版本号
     * 
     * @param lastVersion 上一个版本号
     * @return 如果当前版本号大于lastVersion则返回true，否则返回false
     */
    public boolean isVersionGreaterThan(String lastVersion) {
        if (lastVersion == null || lastVersion.trim().isEmpty()) {
            return true; // 如果没有上一个版本，当前版本肯定更大
        }

        // 确保两个版本号都符合格式
        if (!VERSION_PATTERN.matcher(versionNumber).matches() ||
                !VERSION_PATTERN.matcher(lastVersion).matches()) {
            throw new ParamValidationException("versionNumber", "版本号必须遵循 x.y.z 格式");
        }

        // 分割版本号
        String[] current = versionNumber.split("\\.");
        String[] last = lastVersion.split("\\.");

        // 比较主版本号
        int currentMajor = Integer.parseInt(current[0]);
        int lastMajor = Integer.parseInt(last[0]);
        if (currentMajor > lastMajor)
            return true;
        if (currentMajor < lastMajor)
            return false;

        // 主版本号相同，比较次版本号
        int currentMinor = Integer.parseInt(current[1]);
        int lastMinor = Integer.parseInt(last[1]);
        if (currentMinor > lastMinor)
            return true;
        if (currentMinor < lastMinor)
            return false;

        // 主版本号和次版本号都相同，比较修订版本号
        int currentPatch = Integer.parseInt(current[2]);
        int lastPatch = Integer.parseInt(last[2]);

        return currentPatch > lastPatch;
    }
}