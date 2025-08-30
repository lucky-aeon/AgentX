package org.xhy.infrastructure.sso;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xhy.domain.sso.model.SsoProvider;
import org.xhy.domain.sso.model.SsoUserInfo;
import org.xhy.domain.sso.service.SsoService;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.HashMap;
import java.util.Map;

@Service
public class CommunitySsoService implements SsoService {


    private final RestTemplate restTemplate;
    private final SsoConfigProvider ssoConfigProvider;

    public CommunitySsoService(RestTemplate restTemplate, SsoConfigProvider ssoConfigProvider) {
        this.restTemplate = restTemplate;
        this.ssoConfigProvider = ssoConfigProvider;
    }

    @Override
    public String getLoginUrl(String redirectUrl) {
        SsoConfigProvider.CommunitySsoConfig config = getEffectiveConfig();
        if (config.getBaseUrl() == null || config.getBaseUrl().isEmpty() || config.getAppKey() == null
                || config.getAppKey().isEmpty()) {
            throw new BusinessException("Community SSO未配置");
        }

        return String.format("%s/sso/login?app_key=%s&redirect_url=%s", config.getBaseUrl(), config.getAppKey(),
                redirectUrl != null ? redirectUrl : config.getCallbackUrl());
    }

    @Override
    public SsoUserInfo getUserInfo(String authCode) {
        SsoConfigProvider.CommunitySsoConfig config = getEffectiveConfig();
        if (config.getBaseUrl() == null || config.getBaseUrl().isEmpty() || config.getAppKey() == null
                || config.getAppKey().isEmpty() || config.getAppSecret() == null || config.getAppSecret().isEmpty()) {
            throw new BusinessException("Community SSO未配置");
        }

        try {
            String url = config.getBaseUrl() + "/sso/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = new HashMap<>();
            request.put("app_key", config.getAppKey());
            request.put("app_secret", config.getAppSecret());
            request.put("auth_code", authCode);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response == null || !Integer.valueOf(200).equals(response.get("code"))) {
                throw new BusinessException("获取Community用户信息失败: " + (response != null ? response.get("msg") : "未知错误"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            return new SsoUserInfo(String.valueOf(data.get("id")), (String) data.get("name"),
                    (String) data.get("email"), (String) data.get("avatar"), (String) data.get("desc"),
                    SsoProvider.COMMUNITY);

        } catch (Exception e) {
            throw new BusinessException("Community SSO登录失败: " + e.getMessage());
        }
    }

    @Override
    public SsoProvider getProvider() {
        return SsoProvider.COMMUNITY;
    }

    /** 获取有效的配置（仅从数据库读取）
     * 
     * @return 有效的Community配置 */
    private SsoConfigProvider.CommunitySsoConfig getEffectiveConfig() {
        SsoConfigProvider.CommunitySsoConfig config = ssoConfigProvider.getCommunityConfig();

        // 检查配置是否完整
        if (config.getBaseUrl() == null || config.getAppKey() == null || config.getAppSecret() == null) {
            throw new BusinessException("Community SSO配置不完整，请在管理后台配置Community OAuth应用信息");
        }

        return config;
    }
}