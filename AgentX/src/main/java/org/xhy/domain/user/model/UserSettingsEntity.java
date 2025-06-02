package org.xhy.domain.user.model;

import com.baomidou.mybatisplus.annotation.*;
import org.apache.ibatis.type.JdbcType;
import org.xhy.infrastructure.entity.BaseEntity;
import org.xhy.infrastructure.converter.UserSettingsConfigConverter;
import org.xhy.domain.user.model.config.UserSettingsConfig;

/** 用户设置领域模型 */
@TableName("user_settings")
public class UserSettingsEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户ID */
    private String userId;

    /** 设置配置 */
    @TableField(typeHandler = UserSettingsConfigConverter.class, jdbcType = JdbcType.OTHER)
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

    /** 获取默认模型ID */
    public String getDefaultModelId() {
        if (settingConfig == null) {
            return null;
        }
        return settingConfig.getDefaultModel();
    }

    /** 设置默认模型ID */
    public void setDefaultModelId(String modelId) {
        if (settingConfig == null) {
            settingConfig = new UserSettingsConfig();
        }
        settingConfig.setDefaultModel(modelId);
    }
}