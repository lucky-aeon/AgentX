package org.xhy.application.user.service;


import org.xhy.application.user.assembler.UserAssembler;
import org.xhy.application.user.dto.UserDTO;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.service.UserDomainService;

public class UserAppService {


    private final UserDomainService userDomainService;

    public UserAppService(UserDomainService userDomainService) {
        this.userDomainService = userDomainService;
    }

    public UserDTO getUserInfo(String id){
        UserEntity userEntity = userDomainService.getInfo(id);
        return UserAssembler.toDTO(userEntity);
    }
}
