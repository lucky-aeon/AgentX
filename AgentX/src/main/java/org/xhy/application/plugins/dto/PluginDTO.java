package org.xhy.application.plugins.dto;

import java.util.List;

import org.xhy.domain.plugins.config.PluginConfig;
import org.xhy.domain.plugins.constant.PluginStatus;
import org.xhy.domain.plugins.constant.PluginType;

/**
 * 插件数据传输对象
 */
public class PluginDTO {
    /**
     * ID
     */
    private String id;
    /**
     * 插件名称
     */
    private String name;
    /**
     * 插件描述
     */
    private String description;

    /**
     * 插件tag  
     */
    private List<String> tags;

    /**
     * 状态
     */
    private PluginStatus status;
    /**
     * 仓库地址
     */
    private String repositoryUrl;
    /**
     * 授权协议
     */
    private String license;
    /**
     * 版本
     */
    private String version;
    /**
     * 图标
     */
    private String icon;

    /**
     * 插件类型
     */
    private PluginType type;

    /**
     * 插件配置
     */
    private PluginConfig config;

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

    public PluginType getType() {
        return type;
    }

    public void setType(PluginType type) {
        this.type = type;
    }

    public PluginConfig getConfig() {
        return config;
    }

    public void setConfig(PluginConfig config) {
        this.config = config;
    }
}
