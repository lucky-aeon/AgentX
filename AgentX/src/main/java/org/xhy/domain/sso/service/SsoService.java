package org.xhy.domain.sso.service;

import org.xhy.domain.sso.model.SsoProvider;
import org.xhy.domain.sso.model.SsoUserInfo;

public interface SsoService {
    
    /**
     * 获取SSO登录重定向URL
     * @param redirectUrl 登录成功后的回调地址
     * @return 重定向URL
     */
    String getLoginUrl(String redirectUrl);
    
    /**
     * 通过授权码获取用户信息
     * @param authCode 授权码
     * @return 用户信息
     */
    SsoUserInfo getUserInfo(String authCode);
    
    /**
     * 获取支持的SSO提供商类型
     * @return SSO提供商类型
     */
    SsoProvider getProvider();
}