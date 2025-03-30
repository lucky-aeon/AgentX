package org.xhy.application.plugins.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.xhy.domain.plugins.config.PluginConfig;
import org.xhy.domain.plugins.constant.PluginType;

/**
 * 插件提交命令
 */
public class PluginSubmissionCommand {
    /**
     * 插件名称
     */
    @NotBlank(message = "插件名称不能为空")
    private String name;

    /**
     * 插件描述
     */
    private String description;

    /**
     * 仓库地址
     */
    @NotBlank(message = "仓库地址不能为空")
    private String repositoryUrl;

    /**
     * 授权协议
     */
    @NotBlank(message = "授权协议不能为空")
    private String license;

    /**
     * 插件配置
     */
    @NotNull(message = "插件配置不能为空")
    private PluginConfig config;

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

    public PluginConfig getConfig() {
        return config;
    }

    public void setConfig(PluginConfig config) {
        this.config = config;
    }
}
