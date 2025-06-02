package org.xhy.application.user.dto;

import org.xhy.domain.user.model.config.UserSettingsConfig;

/** 用户设置数据传输对象 */
public class UserSettingsDTO {

    /** 主键ID */
    private String id;

    /** 用户ID */
    private String userId;

    /** 配置 */
    private UserSettingsConfig settingConfig;

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

    public UserSettingsConfig getSettingConfig() {
        return settingConfig;
    }

    public void setSettingConfig(UserSettingsConfig settingConfig) {
        this.settingConfig = settingConfig;
    }
}