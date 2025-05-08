package org.xhy.application.user.service;

import org.springframework.stereotype.Service;
import org.xhy.application.user.assembler.UserAssembler;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.service.UserDomainService;
import org.xhy.infrastructure.email.EmailService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.utils.JwtUtils;
import org.xhy.infrastructure.verification.CaptchaUtils;
import org.xhy.infrastructure.verification.VerificationCodeService;
import org.xhy.interfaces.dto.user.request.LoginRequest;
import org.xhy.interfaces.dto.user.request.RegisterRequest;

import org.springframework.util.StringUtils;

@Service
public class LoginAppService {

    private final UserDomainService userDomainService;
    private final EmailService emailService;
    private final VerificationCodeService verificationCodeService;

    public LoginAppService(UserDomainService userDomainService, 
                          EmailService emailService,
                          VerificationCodeService verificationCodeService) {
        this.userDomainService = userDomainService;
        this.emailService = emailService;
        this.verificationCodeService = verificationCodeService;
    }

    public String login(LoginRequest loginRequest) {
        UserEntity userEntity = userDomainService.login(loginRequest.getAccount(), loginRequest.getPassword());
        return JwtUtils.generateToken(userEntity.getId());
    }

    public void register(RegisterRequest registerRequest) {
        // 如果是邮箱注册，需要验证码
        if (StringUtils.hasText(registerRequest.getEmail()) && !StringUtils.hasText(registerRequest.getPhone())) {
            if (!StringUtils.hasText(registerRequest.getCode())) {
                throw new BusinessException("邮箱注册需要验证码");
            }
            
            boolean isValid = verificationCodeService.verifyCode(registerRequest.getEmail(), registerRequest.getCode());
            if (!isValid) {
                throw new BusinessException("验证码无效或已过期");
            }
        }
        
        userDomainService.register(registerRequest.getEmail(), registerRequest.getPhone(),
                registerRequest.getPassword());
    }
    
    public void sendEmailVerificationCode(String email, String captchaUuid, String captchaCode, String ip) {
        // 检查邮箱是否已存在
        userDomainService.checkAccountExist(email, null);
        
        // 生成验证码并发送邮件
        String code = verificationCodeService.generateCode(email, captchaUuid, captchaCode, ip);
        emailService.sendVerificationCode(email, code);
    }
    
    public boolean verifyEmailCode(String email, String code) {
        return verificationCodeService.verifyCode(email, code);
    }
}
