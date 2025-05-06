package org.xhy.application.user.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.user.dto.UserDTO;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.interfaces.dto.user.request.RegisterRequest;

public class UserAssembler {

    public static UserDTO toDTO(UserEntity userEntity) {
        UserDTO userDTO = new UserDTO();

        BeanUtils.copyProperties(userEntity, userDTO);
        return userDTO;
    }

    public static UserEntity toEntity(UserDTO userDTO) {
        UserEntity userEntity = new UserEntity();
        BeanUtils.copyProperties(userDTO, userEntity);
        return userEntity;
    }

    public static UserEntity toEntity(RegisterRequest registerRequest) {
        UserEntity userEntity = new UserEntity();
        BeanUtils.copyProperties(registerRequest, userEntity);
        return userEntity;
    }
}
