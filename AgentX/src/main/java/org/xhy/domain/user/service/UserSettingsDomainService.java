package org.xhy.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;

import java.util.ArrayList;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.xhy.domain.user.model.UserSettingsEntity;
import org.xhy.domain.user.model.config.FallbackConfig;
import org.xhy.domain.user.repository.UserSettingsRepository;

/** 用户设置领域服务 */
@Service
public class UserSettingsDomainService {

    private final UserSettingsRepository userSettingsRepository;

    public UserSettingsDomainService(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    /** 获取用户设置
     * @param userId 用户ID
     * @return 用户设置实体 */
    public UserSettingsEntity getUserSettings(String userId) {
        LambdaQueryWrapper<UserSettingsEntity> wrapper = Wrappers.<UserSettingsEntity>lambdaQuery()
                .eq(UserSettingsEntity::getUserId, userId);
        return userSettingsRepository.selectOne(wrapper);
    }

    /** 更新用户设置
     * @param userSettings 用户设置实体 */
    public void update(UserSettingsEntity userSettings) {
        Wrapper<UserSettingsEntity> wrapper = Wrappers.<UserSettingsEntity>lambdaQuery()
                .eq(UserSettingsEntity::getUserId, userSettings.getUserId());
        userSettingsRepository.checkedUpdate(userSettings, wrapper);
    }

    /** 获取用户默认模型ID
     * @param userId 用户ID
     * @return 默认模型ID */
    public String getUserDefaultModelId(String userId) {
        UserSettingsEntity settings = getUserSettings(userId);
        return settings != null ? settings.getDefaultModelId() : null;
    }

    /** 获取用户降级链配置
     * @param userId 用户ID
     * @return 降级模型ID列表，如果未启用降级则返回null */
    public List<String> getUserFallbackChain(String userId) {
        UserSettingsEntity settings = getUserSettings(userId);
        if (settings == null || settings.getSettingConfig() == null) {
            return new ArrayList<>();
        }

        FallbackConfig fallbackConfig = settings.getSettingConfig().getFallbackConfig();
        if (fallbackConfig == null || !fallbackConfig.isEnabled() || fallbackConfig.getFallbackChain().isEmpty()) {
            return new ArrayList<>();
        }

        return fallbackConfig.getFallbackChain();
    }

    /** 设置用户默认模型ID
     * @param userId 用户ID
     * @param modelId 模型ID */
    public void setUserDefaultModelId(String userId, String modelId) {
        UserSettingsEntity settings = getUserSettings(userId);
        if (settings == null) {
            // 创建新的用户设置
            settings = new UserSettingsEntity();
            settings.setUserId(userId);
            settings.setDefaultModelId(modelId);
            userSettingsRepository.insert(settings);
        } else {
            // 更新现有设置
            settings.setDefaultModelId(modelId);
            Wrapper<UserSettingsEntity> wrapper = Wrappers.<UserSettingsEntity>lambdaQuery()
                    .eq(UserSettingsEntity::getUserId, userId);
            userSettingsRepository.checkedUpdate(settings, wrapper);
        }
    }
}