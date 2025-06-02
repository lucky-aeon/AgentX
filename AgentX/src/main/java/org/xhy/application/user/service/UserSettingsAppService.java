package org.xhy.application.user.service;

import org.springframework.stereotype.Service;
import org.xhy.application.user.assembler.UserSettingsAssembler;
import org.xhy.application.user.dto.UserSettingsDTO;
import org.xhy.domain.user.model.UserSettingsEntity;
import org.xhy.domain.user.service.UserSettingsDomainService;
import org.xhy.interfaces.dto.user.request.UserSettingsUpdateRequest;

/** 用户设置应用服务 */
@Service
public class UserSettingsAppService {

    private final UserSettingsDomainService userSettingsDomainService;

    public UserSettingsAppService(UserSettingsDomainService userSettingsDomainService) {
        this.userSettingsDomainService = userSettingsDomainService;
    }

    /** 获取用户设置
     * @param userId 用户ID
     * @return 用户设置DTO */
    public UserSettingsDTO getUserSettings(String userId) {
        UserSettingsEntity entity = userSettingsDomainService.getUserSettings(userId);
        return UserSettingsAssembler.toDTO(entity);
    }

    /** 更新用户设置
     * @param request 更新请求
     * @param userId 用户ID
     * @return 更新后的用户设置DTO */
    public UserSettingsDTO updateUserSettings(UserSettingsUpdateRequest request, String userId) {
        UserSettingsEntity entity = UserSettingsAssembler.toEntity(request, userId);
        userSettingsDomainService.update(entity);

        return UserSettingsAssembler.toDTO(entity);
    }

    /** 获取用户默认模型ID
     * @param userId 用户ID
     * @return 默认模型ID */
    public String getUserDefaultModelId(String userId) {
        return userSettingsDomainService.getUserDefaultModelId(userId);
    }
}