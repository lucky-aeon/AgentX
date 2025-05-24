package org.xhy.domain.tool.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.domain.tool.constant.UploadType;
import org.xhy.domain.tool.model.config.ToolDefinition;
import org.xhy.infrastructure.converter.ListStringConverter;
import org.xhy.infrastructure.converter.ToolDefinitionListConverter;
import org.xhy.infrastructure.converter.UploadTypeConverter;
import org.xhy.infrastructure.entity.BaseEntity;

import java.util.List;

/** 工具版本实体类 */
@TableName(value = "tool_versions", autoResultMap = true)
public class ToolVersionEntity extends BaseEntity {

    /** 版本唯一ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 工具名称 */
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

    /** 版本号 */
    @TableField("version")
    private String version;

    /** 工具ID */
    @TableField("tool_id")
    private String toolId;

    /** 上传方式：github, zip */
    @TableField(value = "upload_type", typeHandler = UploadTypeConverter.class)
    private UploadType uploadType;

    /** 上传URL */
    @TableField("upload_url")
    private String uploadUrl;

    /** 工具列表 */
    @TableField(value = "tool_list", typeHandler = ToolDefinitionListConverter.class)
    private List<ToolDefinition> toolList;

    /** 标签列表 */
    @TableField(value = "labels", typeHandler = ListStringConverter.class)
    private List<String> labels;

    /** 是否官方工具 */
    @TableField("is_office")
    private Boolean isOffice;

    /** 公开状态 */
    @TableField("public_status")
    private Boolean publicStatus;

    /** 变更日志 */
    @TableField("change_log")
    private String changeLog;

    /** MCP服务器名称 */
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getToolId() {
        return toolId;
    }

    public void setToolId(String toolId) {
        this.toolId = toolId;
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

    public List<ToolDefinition> getToolList() {
        return toolList;
    }

    public void setToolList(List<ToolDefinition> toolList) {
        this.toolList = toolList;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public Boolean getIsOffice() {
        return isOffice;
    }

    public void setIsOffice(Boolean isOffice) {
        this.isOffice = isOffice;
    }

    public Boolean getPublicStatus() {
        return publicStatus;
    }

    public void setPublicStatus(Boolean publicStatus) {
        this.publicStatus = publicStatus;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public String getMcpServerName() {
        return mcpServerName;
    }

    public void setMcpServerName(String mcpServerName) {
        this.mcpServerName = mcpServerName;
    }
}
