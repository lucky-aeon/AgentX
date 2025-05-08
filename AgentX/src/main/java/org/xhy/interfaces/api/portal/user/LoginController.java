package org.xhy.interfaces.api.portal.user;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.user.service.LoginAppService;
import org.xhy.infrastructure.verification.CaptchaUtils;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.user.request.GetCaptchaRequest;
import org.xhy.interfaces.dto.user.request.LoginRequest;
import org.xhy.interfaces.dto.user.request.RegisterRequest;
import org.xhy.interfaces.dto.user.request.ResetPasswordRequest;
import org.xhy.interfaces.dto.user.request.SendEmailCodeRequest;
import org.xhy.interfaces.dto.user.request.SendResetPasswordCodeRequest;
import org.xhy.interfaces.dto.user.request.VerifyEmailCodeRequest;
import org.xhy.interfaces.dto.user.request.VerifyResetPasswordCodeRequest;
import org.xhy.interfaces.dto.user.response.CaptchaResponse;

import java.util.Map;

/** 登录注册 */
@RestController
@RequestMapping
public class LoginController {

    private final LoginAppService loginAppService;

    public LoginController(LoginAppService loginAppService) {
        this.loginAppService = loginAppService;
    }

    /** 登录
     * @param loginRequest
     * @return */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Validated LoginRequest loginRequest) {
        String token = loginAppService.login(loginRequest);
        return Result.success("登录成功", Map.of("token", token));
    }

    /** 注册
     * @param registerRequest
     * @return */
    @PostMapping("/register")
    public Result<?> register(@RequestBody @Validated RegisterRequest registerRequest) {
        loginAppService.register(registerRequest);
        return Result.success().message("注册成功");
    }

    /** 获取图形验证码
     * @param request
     * @return */
    @PostMapping("/get-captcha")
    public Result<CaptchaResponse> getCaptcha(@RequestBody(required = false) GetCaptchaRequest request) {
        CaptchaUtils.CaptchaResult captchaResult = CaptchaUtils.generateCaptcha();
        CaptchaResponse response = new CaptchaResponse(captchaResult.getUuid(), captchaResult.getImageBase64());
        return Result.success(response);
    }

    /** 发送邮箱验证码接口
     * @param request
     * @param httpRequest
     * @return */
    @PostMapping("/send-email-code")
    public Result<?> sendEmailCode(@RequestBody @Validated SendEmailCodeRequest request,
            HttpServletRequest httpRequest) {
        // 获取客户端IP
        String clientIp = getClientIp(httpRequest);

        loginAppService.sendEmailVerificationCode(request.getEmail(), request.getCaptchaUuid(),
                request.getCaptchaCode(), clientIp);
        return Result.success().message("验证码已发送，请查收邮件");
    }

    /** 发送重置密码邮箱验证码接口
     * @param request
     * @param httpRequest
     * @return */
    @PostMapping("/send-reset-password-code")
    public Result<?> sendResetPasswordCode(@RequestBody @Validated SendResetPasswordCodeRequest request,
            HttpServletRequest httpRequest) {
        // 获取客户端IP
        String clientIp = getClientIp(httpRequest);

        loginAppService.sendResetPasswordCode(request.getEmail(), request.getCaptchaUuid(), request.getCaptchaCode(),
                clientIp);
        return Result.success().message("验证码已发送，请查收邮件");
    }

    /** 验证邮箱验证码接口
     * @param request
     * @return */
    @PostMapping("/verify-email-code")
    public Result<Boolean> verifyEmailCode(@RequestBody @Validated VerifyEmailCodeRequest request) {
        boolean isValid = loginAppService.verifyEmailCode(request.getEmail(), request.getCode());
        if (isValid) {
            return Result.success(true).message("验证码验证成功");
        } else {
            return Result.error(403, "验证码无效或已过期");
        }
    }

    /** 重置密码接口
     * @param request
     * @return */
    @PostMapping("/reset-password")
    public Result<?> resetPassword(@RequestBody @Validated ResetPasswordRequest request) {
        loginAppService.resetPassword(request.getEmail(), request.getNewPassword(), request.getCode());
        return Result.success().message("密码重置成功");
    }

    /** 获取客户端IP
     * @param request
     * @return */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
