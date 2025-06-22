package org.xhy.application.tool.dto;

import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.constant.ToolType;
import org.xhy.domain.tool.constant.UploadType;
import org.xhy.domain.tool.model.config.ToolDefinition;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 包含用户信息的工具DTO，用于管理员界面 */
public class ToolWithUserDTO {

    private String id;
    private String name;
    private String icon;
    private String subtitle;
    private String description;
    private String userId;
    private List<String> labels;
    private ToolType toolType;
    private UploadType uploadType;
    private String uploadUrl;
    private Map<String, Object> installCommand;
    private List<ToolDefinition> toolList;
    private ToolStatus status;
    private Boolean isOffice;
    private Integer installCount;
    private String currentVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String rejectReason;
    private ToolStatus failedStepStatus;
    private String mcpServerName;

    // 用户信息字段
    private String userNickname;
    private String userEmail;
    private String userAvatarUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public ToolType getToolType() {
        return toolType;
    }

    public void setToolType(ToolType toolType) {
        this.toolType = toolType;
    }

    public UploadType getUploadType() {
        return uploadType;
    }

    public void setUploadType(UploadType uploadType) {
        this.uploadType = uploadType;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public Map<String, Object> getInstallCommand() {
        return installCommand;
    }

    public void setInstallCommand(Map<String, Object> installCommand) {
        this.installCommand = installCommand;
    }

    public List<ToolDefinition> getToolList() {
        return toolList;
    }

    public void setToolList(List<ToolDefinition> toolList) {
        this.toolList = toolList;
    }

    public ToolStatus getStatus() {
        return status;
    }

    public void setStatus(ToolStatus status) {
        this.status = status;
    }

    public Boolean getIsOffice() {
        return isOffice;
    }

    public void setIsOffice(Boolean isOffice) {
        this.isOffice = isOffice;
    }

    public Integer getInstallCount() {
        return installCount;
    }

    public void setInstallCount(Integer installCount) {
        this.installCount = installCount;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public ToolStatus getFailedStepStatus() {
        return failedStepStatus;
    }

    public void setFailedStepStatus(ToolStatus failedStepStatus) {
        this.failedStepStatus = failedStepStatus;
    }

    public String getMcpServerName() {
        return mcpServerName;
    }

    public void setMcpServerName(String mcpServerName) {
        this.mcpServerName = mcpServerName;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }
}