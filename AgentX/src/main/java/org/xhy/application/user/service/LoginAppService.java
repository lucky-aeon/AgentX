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

    public LoginAppService(UserDomainService userDomainService, EmailService emailService,
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

    /** 发送注册邮箱验证码 */
    public void sendEmailVerificationCode(String email, String captchaUuid, String captchaCode, String ip) {
        // 检查邮箱是否已存在
        userDomainService.checkAccountExist(email, null);

        // 生成验证码并发送邮件
        String code = verificationCodeService.generateCode(email, captchaUuid, captchaCode, ip);
        emailService.sendVerificationCode(email, code);
    }

    /** 发送重置密码邮箱验证码 */
    public void sendResetPasswordCode(String email, String captchaUuid, String captchaCode, String ip) {
        // 检查邮箱是否存在，不存在则抛出异常
        UserEntity user = userDomainService.findUserByAccount(email);
        if (user == null) {
            throw new BusinessException("该邮箱未注册");
        }

        // 生成验证码并发送邮件
        String code = verificationCodeService.generateCode(email, captchaUuid, captchaCode, ip,
                VerificationCodeService.BUSINESS_TYPE_RESET_PASSWORD);
        emailService.sendVerificationCode(email, code);
    }

    /** 验证邮箱验证码（注册） */
    public boolean verifyEmailCode(String email, String code) {
        return verificationCodeService.verifyCode(email, code);
    }

    /** 验证重置密码邮箱验证码 */
    public boolean verifyResetPasswordCode(String email, String code) {
        return verificationCodeService.verifyCode(email, code, VerificationCodeService.BUSINESS_TYPE_RESET_PASSWORD);
    }

    /** 重置密码 */
    public void resetPassword(String email, String newPassword, String code) {
        // 验证重置密码验证码
        boolean isValid = verifyResetPasswordCode(email, code);
        if (!isValid) {
            throw new BusinessException("验证码无效或已过期");
        }

        // 查找用户并重置密码
        UserEntity user = userDomainService.findUserByAccount(email);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 更新密码
        userDomainService.updatePassword(user.getId(), newPassword);
    }
}
