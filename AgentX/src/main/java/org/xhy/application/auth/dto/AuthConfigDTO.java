package org.xhy.application.auth.dto;

import java.util.Map;

/** 认证配置响应DTO */
public class AuthConfigDTO {

    private Map<String, LoginMethodDTO> loginMethods;
    private Boolean registerEnabled;

    public Map<String, LoginMethodDTO> getLoginMethods() {
        return loginMethods;
    }

    public void setLoginMethods(Map<String, LoginMethodDTO> loginMethods) {
        this.loginMethods = loginMethods;
    }

    public Boolean getRegisterEnabled() {
        return registerEnabled;
    }

    public void setRegisterEnabled(Boolean registerEnabled) {
        this.registerEnabled = registerEnabled;
    }
}