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

/**
 * 用户工具关联实体类
 */
@TableName(value = "user_tools", autoResultMap = true)
public class UserToolEntity extends BaseEntity {

    /**
     * 唯一ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 工具版本ID
     */
    @TableField("tool_version_id")
    private String toolVersionId;

    /**
     * 版本号
     */
    @TableField("version")
    private String version;

    /**
     * 上传方式：1-github, 2-zip
     */
    @TableField(value = "upload_type", typeHandler = UploadTypeConverter.class)
    private UploadType uploadType;

    /**
     * 上传URL
     */
    @TableField("upload_url")
    private String uploadUrl;

    /**
     * 工具列表
     */
    @TableField(value = "tool_list", typeHandler = ToolDefinitionListConverter.class)
    private List<ToolDefinition> toolList;

    /**
     * 标签列表
     */
    @TableField(value = "labels", typeHandler = ListStringConverter.class)
    private List<String> labels;

    /**
     * 是否官方工具
     */
    @TableField("is_office")
    private Boolean isOffice;

    /**
     * 公开状态
     */
    @TableField("public_state")
    private Boolean publicState;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToolVersionId() {
        return toolVersionId;
    }

    public void setToolVersionId(String toolVersionId) {
        this.toolVersionId = toolVersionId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public Boolean getPublicState() {
        return publicState;
    }

    public void setPublicState(Boolean publicState) {
        this.publicState = publicState;
    }
} 