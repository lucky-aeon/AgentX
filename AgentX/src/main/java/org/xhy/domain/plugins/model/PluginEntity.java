package org.xhy.domain.plugins.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.List;

import org.xhy.domain.plugins.config.PluginConfig;
import org.xhy.domain.plugins.constant.PluginStatus;
import org.xhy.domain.plugins.constant.PluginType;
import org.xhy.domain.plugins.typehandler.PluginConfigTypeHandler;
import org.xhy.domain.plugins.typehandler.PluginStatusTypeHandler;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.infrastructure.entity.BaseEntity;
import org.xhy.infrastructure.exception.BusinessException;

/**
 * 插件聚合根
 */
@TableName("plugins")
public class PluginEntity extends BaseEntity {
    /**
     * 插件ID
     */
    @TableId
    private String id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 插件名称
     */
    @TableField("name")
    private String name;

    /**
     * 插件描述
     */
    @TableField("description")
    private String description;

    /**
     * 插件状态
     */
    @TableField(value = "status", typeHandler = PluginStatusTypeHandler.class)
    private PluginStatus status;

    /**
     * 仓库地址
     */
    @TableField("repository_url")
    private String repositoryUrl;

    /**
     * 授权协议
     */
    @TableField("license")
    private String license;

    /**
     * 版本号
     */
    @TableField("version")
    private String version;

    /**
     * 图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 插件tag
     */
    @TableField("tags")
    private List<String> tags;  

    /**
     * 插件配置
     */
    @TableField(value = "config", typeHandler = PluginConfigTypeHandler.class)
    private PluginConfig config;

    /**
     * 创建新的插件
     */
    public static PluginEntity create(String name, String description, GitRepository repository, License license) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("插件名称不能为空");
        }

        PluginEntity plugin = new PluginEntity();
        plugin.name = name.trim();
        plugin.description = description != null ? description.trim() : "";
        plugin.repositoryUrl = repository.getUrl();
        plugin.license = license.getValue();
        plugin.status = PluginStatus.ENABLED;
        plugin.userId = UserContext.getCurrentUserId();
        return plugin;
    }

    /**
     * 更新插件
     */
    public void update(String name, String description, GitRepository repository, License license) {
        this.name = name.trim();
        this.description = description != null ? description.trim() : "";
        this.repositoryUrl = repository.getUrl();
        this.license = license.getValue();
    }

    /**
     * 更新插件状态
     */
    public void updateStatus(PluginStatus newStatus) {
        if (newStatus == null) {
            throw new BusinessException("状态不能为空");
        }
        this.status = newStatus;
    }

    // Getters
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

    public PluginStatus getStatus() {
        return status;
    }

    public void setStatus(PluginStatus status) {
        this.status = status;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public PluginConfig getConfig() {
        return config;
    }

    public void setConfig(PluginConfig config) {
        this.config = config;
    }

}
