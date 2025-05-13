package org.xhy.interfaces.dto.tool.request;

import jakarta.validation.constraints.NotBlank;
import org.xhy.infrastructure.exception.ParamValidationException;

import java.util.regex.Pattern;

public class MarketToolRequest {

    @NotBlank(message = "工具 id 不可为空")
    private String toolId;

    @NotBlank(message = "版本号不能为空")
    private String version;

    private String changeLog;

    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    public void validate() {
        // 验证版本号格式
        if (!VERSION_PATTERN.matcher(version).matches()) {
            throw new ParamValidationException("versionNumber", "版本号必须遵循 x.y.z 格式");
        }
    }

    /** 比较版本号是否大于给定的版本号
     *
     * @param lastVersion 上一个版本号
     * @return 如果当前版本号大于lastVersion则返回true，否则返回false */
    public boolean isVersionGreaterThan(String lastVersion) {
        if (lastVersion == null || lastVersion.trim().isEmpty()) {
            return true; // 如果没有上一个版本，当前版本肯定更大
        }

        // 确保两个版本号都符合格式
        if (!VERSION_PATTERN.matcher(version).matches() || !VERSION_PATTERN.matcher(lastVersion).matches()) {
            throw new ParamValidationException("versionNumber", "版本号必须遵循 x.y.z 格式");
        }

        // 分割版本号
        String[] current = version.split("\\.");
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

    public String getToolId() {
        return toolId;
    }

    public void setToolId(String toolId) {
        this.toolId = toolId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }
}
