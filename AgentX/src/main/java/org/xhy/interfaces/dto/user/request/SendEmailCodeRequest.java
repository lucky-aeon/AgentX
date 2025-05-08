package org.xhy.interfaces.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SendEmailCodeRequest {
    @Email(message = "不是一个合法的邮箱")
    @NotBlank(message = "邮箱不能为空")
    private String email;
    
    @NotBlank(message = "验证码UUID不能为空")
    private String captchaUuid;
    
    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getCaptchaUuid() {
        return captchaUuid;
    }
    
    public void setCaptchaUuid(String captchaUuid) {
        this.captchaUuid = captchaUuid;
    }
    
    public String getCaptchaCode() {
        return captchaCode;
    }
    
    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
} 