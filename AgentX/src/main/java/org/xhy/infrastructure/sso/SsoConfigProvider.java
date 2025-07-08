package org.xhy.infrastructure.sso;

import org.springframework.stereotype.Component;
import org.xhy.domain.auth.constant.AuthFeatureKey;
import org.xhy.domain.auth.model.AuthSettingEntity;
import org.xhy.domain.auth.service.AuthSettingDomainService;

import java.util.Map;

/** SSO配置提供者 */
@Component
public class SsoConfigProvider {

    private final AuthSettingDomainService authSettingDomainService;

    public SsoConfigProvider(AuthSettingDomainService authSettingDomainService) {
        this.authSettingDomainService = authSettingDomainService;
    }

    /** 获取GitHub SSO配置
     * 
     * @return GitHub配置 */
    public GitHubSsoConfig getGitHubConfig() {
        AuthSettingEntity entity = authSettingDomainService.getByFeatureKey(AuthFeatureKey.GITHUB_LOGIN);
        if (entity == null || entity.getConfigData() == null) {
            return new GitHubSsoConfig();
        }

        try {
            Map<String, Object> configMap = entity.getConfigData();

            GitHubSsoConfig config = new GitHubSsoConfig();
            config.setClientId((String) configMap.get("clientId"));
            config.setClientSecret((String) configMap.get("clientSecret"));
            config.setRedirectUri((String) configMap.get("redirectUri"));
            // GitHub API端点使用固定值，不需要配置
            config.setAuthorizeUrl("https://github.com/login/oauth/authorize");
            config.setTokenUrl("https://github.com/login/oauth/access_token");
            config.setUserInfoUrl("https://api.github.com/user");
            config.setUserEmailUrl("https://api.github.com/user/emails");

            return config;
        } catch (Exception e) {
            return new GitHubSsoConfig();
        }
    }

    /** 获取Community SSO配置
     * 
     * @return Community配置 */
    public CommunitySsoConfig getCommunityConfig() {
        AuthSettingEntity entity = authSettingDomainService.getByFeatureKey(AuthFeatureKey.COMMUNITY_LOGIN);
        if (entity == null || entity.getConfigData() == null) {
            return new CommunitySsoConfig();
        }

        try {
            Map<String, Object> configMap = entity.getConfigData();

            CommunitySsoConfig config = new CommunitySsoConfig();
            config.setBaseUrl((String) configMap.get("baseUrl"));
            config.setAppKey((String) configMap.get("appKey"));
            config.setAppSecret((String) configMap.get("appSecret"));
            config.setCallbackUrl((String) configMap.get("callbackUrl"));

            return config;
        } catch (Exception e) {
            return new CommunitySsoConfig();
        }
    }

    /** GitHub SSO配置类 */
    public static class GitHubSsoConfig {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String authorizeUrl = "https://github.com/login/oauth/authorize";
        private String tokenUrl = "https://github.com/login/oauth/access_token";
        private String userInfoUrl = "https://api.github.com/user";
        private String userEmailUrl = "https://api.github.com/user/emails";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getAuthorizeUrl() {
            return authorizeUrl;
        }

        public void setAuthorizeUrl(String authorizeUrl) {
            this.authorizeUrl = authorizeUrl;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }

        public String getUserInfoUrl() {
            return userInfoUrl;
        }

        public void setUserInfoUrl(String userInfoUrl) {
            this.userInfoUrl = userInfoUrl;
        }

        public String getUserEmailUrl() {
            return userEmailUrl;
        }

        public void setUserEmailUrl(String userEmailUrl) {
            this.userEmailUrl = userEmailUrl;
        }
    }

    /** Community SSO配置类 */
    public static class CommunitySsoConfig {
        private String baseUrl;
        private String appKey;
        private String appSecret;
        private String callbackUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public String getCallbackUrl() {
            return callbackUrl;
        }

        public void setCallbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
        }
    }
}