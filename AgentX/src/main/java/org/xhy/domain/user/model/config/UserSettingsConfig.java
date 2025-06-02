package org.xhy.domain.user.model.config;

import java.io.Serializable;

/** 用户设置配置 */
public class UserSettingsConfig implements Serializable {

    /** 默认模型ID */
    private String defaultModel;

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }
}