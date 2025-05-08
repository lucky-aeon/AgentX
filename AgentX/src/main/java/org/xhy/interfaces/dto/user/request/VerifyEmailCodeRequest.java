package org.xhy.interfaces.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class VerifyEmailCodeRequest {
    @Email(message = "不是一个合法的邮箱")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}