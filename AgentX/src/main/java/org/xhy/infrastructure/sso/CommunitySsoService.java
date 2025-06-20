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

    @Value("${sso.community.base-url:}")
    private String baseUrl;

    @Value("${sso.community.app-key:}")
    private String appKey;

    @Value("${sso.community.app-secret:}")
    private String appSecret;

    @Value("${sso.community.callback-url:}")
    private String callbackUrl;

    private final RestTemplate restTemplate;

    public CommunitySsoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getLoginUrl(String redirectUrl) {
        if (baseUrl.isEmpty() || appKey.isEmpty()) {
            throw new BusinessException("Community SSO未配置");
        }

        return String.format("%s/sso/login?app_key=%s&redirect_url=%s", baseUrl, appKey,
                redirectUrl != null ? redirectUrl : callbackUrl);
    }

    @Override
    public SsoUserInfo getUserInfo(String authCode) {
        if (baseUrl.isEmpty() || appKey.isEmpty() || appSecret.isEmpty()) {
            throw new BusinessException("Community SSO未配置");
        }

        try {
            String url = baseUrl + "/sso/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = new HashMap<>();
            request.put("app_key", appKey);
            request.put("app_secret", appSecret);
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
}