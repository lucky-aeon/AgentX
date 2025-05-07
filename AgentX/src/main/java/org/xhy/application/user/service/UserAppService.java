package org.xhy.application.user.service;

import org.springframework.stereotype.Service;
import org.xhy.application.user.assembler.UserAssembler;
import org.xhy.application.user.dto.UserDTO;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.service.UserDomainService;
import org.xhy.interfaces.dto.user.request.UserUpdateRequest;

@Service
public class UserAppService {

    private final UserDomainService userDomainService;

    public UserAppService(UserDomainService userDomainService) {
        this.userDomainService = userDomainService;
    }

    /** 获取用户信息 */
    public UserDTO getUserInfo(String id) {
        UserEntity userEntity = userDomainService.getUserInfo(id);
        return UserAssembler.toDTO(userEntity);
    }

    /** 修改用户信息 */
    public void updateUserInfo(UserUpdateRequest userUpdateRequest, String userId) {
        UserEntity user = UserAssembler.toEntity(userUpdateRequest, userId);
        userDomainService.updateUserInfo(user);
    }
}
