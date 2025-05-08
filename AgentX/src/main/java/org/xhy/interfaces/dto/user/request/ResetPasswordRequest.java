package org.xhy.interfaces.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度应在6-20位之间")
    private String newPassword;

    @NotBlank(message = "验证码不能为空")
    private String code;

    public @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email) {
        this.email = email;
    }

    public @NotBlank(message = "新密码不能为空") @Size(min = 6, max = 20, message = "密码长度应在6-20位之间") String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(
            @NotBlank(message = "新密码不能为空") @Size(min = 6, max = 20, message = "密码长度应在6-20位之间") String newPassword) {
        this.newPassword = newPassword;
    }

    public @NotBlank(message = "验证码不能为空") String getCode() {
        return code;
    }

    public void setCode(@NotBlank(message = "验证码不能为空") String code) {
        this.code = code;
    }
}