package org.xhy.domain.user.model.config;

import java.io.Serializable;

/** 用户设置配置 */
public class UserSettingsConfig implements Serializable {

    /** 默认模型ID */
    private String defaultModel;

    /** 降级配置 */
    private FallbackConfig fallbackConfig;

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public FallbackConfig getFallbackConfig() {
        return fallbackConfig;
    }

    public void setFallbackConfig(FallbackConfig fallbackConfig) {
        this.fallbackConfig = fallbackConfig;
    }

    /** 获取降级配置，如果为空则返回默认配置 */
    public FallbackConfig getFallbackConfigOrDefault() {
        if (fallbackConfig == null) {
            fallbackConfig = new FallbackConfig();
        }
        return fallbackConfig;
    }
}