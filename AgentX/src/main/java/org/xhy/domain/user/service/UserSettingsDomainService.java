package org.xhy.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.xhy.domain.user.model.UserSettingsEntity;
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
        userSettingsRepository.checkedUpdate(userSettings,wrapper);
    }

    /** 获取用户默认模型ID
     * @param userId 用户ID
     * @return 默认模型ID */
    public String getUserDefaultModelId(String userId) {
        UserSettingsEntity settings = getUserSettings(userId);
        return settings != null ? settings.getDefaultModelId() : null;
    }
}