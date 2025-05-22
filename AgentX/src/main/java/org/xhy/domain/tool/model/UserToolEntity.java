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

/** 用户工具关联实体类 */
@TableName(value = "user_tools", autoResultMap = true)
public class UserToolEntity extends BaseEntity {

    /** 唯一ID */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户ID */
    @TableField("user_id")
    private String userId;

    /** 工具名称 */
    @TableField("name")
    private String name;

    /** 工具描述 */
    @TableField("description")
    private String description;

    /** 工具图标 */
    @TableField("icon")
    private String icon;

    /** 副标题 */
    @TableField("subtitle")
    private String subtitle;

    /** 工具版本ID */
    @TableField("tool_id")
    private String toolId;

    /** 版本号 */
    @TableField("version")
    private String version;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Boolean getOffice() {
        return isOffice;
    }

    public void setOffice(Boolean office) {
        isOffice = office;
    }
}