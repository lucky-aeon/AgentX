package org.xhy.domain.tool.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.constant.ToolType;
import org.xhy.domain.tool.constant.UploadType;
import org.xhy.domain.tool.model.config.ToolDefinition;
import org.xhy.infrastructure.converter.ListStringConverter;
import org.xhy.infrastructure.converter.MapConverter;
import org.xhy.infrastructure.converter.ToolDefinitionListConverter;
import org.xhy.infrastructure.converter.ToolStatusConverter;
import org.xhy.infrastructure.converter.ToolTypeConverter;
import org.xhy.infrastructure.converter.UploadTypeConverter;
import org.xhy.infrastructure.entity.BaseEntity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 工具实体类 */
@TableName(value = "tools", autoResultMap = true)
public class ToolEntity extends BaseEntity {

    /** 工具唯一ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 工具描述名称 */
    @TableField("name")
    private String name;

    /** 工具图标 */
    @TableField("icon")
    private String icon;

    /** 副标题 */
    @TableField("subtitle")
    private String subtitle;

    /** 工具描述 */
    @TableField("description")
    private String description;

    /** 用户ID */
    @TableField("user_id")
    private String userId;

    /** 标签列表 */
    @TableField(value = "labels", typeHandler = ListStringConverter.class)
    private List<String> labels;

    /** 工具类型：mcp */
    @TableField(value = "tool_type", typeHandler = ToolTypeConverter.class)
    private ToolType toolType = ToolType.MCP;

    /** 上传方式：github, zip */
    @TableField(value = "upload_type", typeHandler = UploadTypeConverter.class)
    private UploadType uploadType = UploadType.GITHUB;

    /** 上传URL */
    @TableField("upload_url")
    private String uploadUrl;

    /** 安装命令 */
    @TableField(value = "install_command", typeHandler = MapConverter.class)
    private Map<String, Object> installCommand;

    /** 工具列表 */
    @TableField(value = "tool_list", typeHandler = ToolDefinitionListConverter.class)
    private List<ToolDefinition> toolList;

    /** 审核状态 */
    @TableField(value = "status", typeHandler = ToolStatusConverter.class)
    private ToolStatus status;

    /** 是否官方工具 */
    @TableField("is_office")
    private Boolean isOffice;

    /** 拒绝原因 */
    @TableField("reject_reason")
    private String rejectReason;

    @TableField(value = "failed_step_status", typeHandler = ToolStatusConverter.class)
    private ToolStatus failedStepStatus;

    @TableField("mcp_server_name")
    private String mcpServerName;

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

    public Boolean getOffice() {
        return isOffice;
    }

    public void setOffice(Boolean office) {
        isOffice = office;
    }

    public String getMcpServerName() {
        return mcpServerName;
    }

    public void setMcpServerName(String mcpServerName) {
        this.mcpServerName = mcpServerName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<tool>\n");
        sb.append("  <name>").append(this.getName()).append("</name>\n");
        sb.append("  <description>").append(this.getDescription()).append("</description>\n");
        if (this.getToolList() != null && !this.getToolList().isEmpty()) {
            sb.append("  <functions>\n");
            for (ToolDefinition def : this.getToolList()) {
                sb.append(def.toString()); // 调用ToolDefinition的toString
            }
            sb.append("  </functions>\n");
        }
        sb.append("</tool>\n");
        return sb.toString();
    }
}