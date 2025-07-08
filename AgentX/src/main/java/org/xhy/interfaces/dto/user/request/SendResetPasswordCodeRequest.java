package org.xhy.interfaces.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SendResetPasswordCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "图形验证码UUID不能为空")
    private String captchaUuid;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;

    public @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email) {
        this.email = email;
    }

    public @NotBlank(message = "图形验证码UUID不能为空") String getCaptchaUuid() {
        return captchaUuid;
    }

    public void setCaptchaUuid(@NotBlank(message = "图形验证码UUID不能为空") String captchaUuid) {
        this.captchaUuid = captchaUuid;
    }

    public @NotBlank(message = "图形验证码不能为空") String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(@NotBlank(message = "图形验证码不能为空") String captchaCode) {
        this.captchaCode = captchaCode;
    }
}