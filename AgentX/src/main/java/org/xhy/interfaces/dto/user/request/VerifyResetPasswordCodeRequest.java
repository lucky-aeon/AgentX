package org.xhy.interfaces.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class VerifyResetPasswordCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    public @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email) {
        this.email = email;
    }

    public @NotBlank(message = "验证码不能为空") String getCode() {
        return code;
    }

    public void setCode(@NotBlank(message = "验证码不能为空") String code) {
        this.code = code;
    }
}