package org.xhy.application.user.service;

import org.springframework.stereotype.Service;
import org.xhy.application.user.assembler.UserAssembler;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.service.UserDomainService;
import org.xhy.infrastructure.utils.JwtUtils;
import org.xhy.interfaces.dto.user.request.LoginRequest;
import org.xhy.interfaces.dto.user.request.RegisterRequest;

@Service
public class LoginAppService {

    private final UserDomainService userDomainService;

    public LoginAppService(UserDomainService userDomainService) {
        this.userDomainService = userDomainService;
    }

    public String login(LoginRequest loginRequest) {
        UserEntity userEntity = userDomainService.login(loginRequest.getAccount(), loginRequest.getPassword());
        return JwtUtils.generateToken(userEntity.getId());
    }

    public void register(RegisterRequest registerRequest) {
        userDomainService.register(registerRequest.getEmail(), registerRequest.getPhone(),
                registerRequest.getPassword());
    }
}
