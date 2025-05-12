package org.xhy.interfaces.dto.tool.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * 创建工具的请求对象
 */
public class CreateToolRequest {

    /**
     * 工具名称
     */
    @NotBlank(message = "工具名称不可为空")
    private String name;

    /**
     * 工具图标
     */
    private String icon;

    @NotBlank(message = "副标题不可为空")
    private String subtitle;

    /**
     * 工具描述
     */
    @NotBlank(message = "工具描述不可为空")
    private String description;

    /**
     * 标签
     */
    @NotEmpty(message = "标签不可为空")
    private List<String> labels;

    /**
     * 上传地址
     */
    @NotEmpty(message = "上传地址不可为空")
    private String uploadUrl;

    /**
     * 安装命令
     */
    @NotNull(message = "安装命令不可为空")
    private Map<String, Object> installCommand;

    // 构造方法
    public CreateToolRequest() {
    }

    // Getter和Setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public Map<String, Object> getInstallCommand() {
        return installCommand;
    }

    public void setInstallCommand(Map<String, Object> installCommand) {
        this.installCommand = installCommand;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }
}