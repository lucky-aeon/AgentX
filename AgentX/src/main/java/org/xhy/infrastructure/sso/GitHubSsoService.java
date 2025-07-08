package org.xhy.infrastructure.sso;

import com.alibaba.fastjson.JSON;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xhy.domain.sso.model.SsoProvider;
import org.xhy.domain.sso.model.SsoUserInfo;
import org.xhy.domain.sso.service.SsoService;
import org.xhy.infrastructure.config.GitHubOAuthProperties;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.dto.user.GitHubTokenResponse;
import org.xhy.interfaces.dto.user.GitHubUserInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class GitHubSsoService implements SsoService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubSsoService.class);

    private final GitHubOAuthProperties githubProperties;
    private final SsoConfigProvider ssoConfigProvider;

    public GitHubSsoService(GitHubOAuthProperties githubProperties, SsoConfigProvider ssoConfigProvider) {
        this.githubProperties = githubProperties;
        this.ssoConfigProvider = ssoConfigProvider;
    }

    @Override
    public String getLoginUrl(String redirectUrl) {
        SsoConfigProvider.GitHubSsoConfig config = getEffectiveConfig();
        String callbackUrl = redirectUrl != null ? redirectUrl : config.getRedirectUri();
        return config.getAuthorizeUrl() + "?client_id=" + config.getClientId() + "&redirect_uri=" + callbackUrl
                + "&scope=user:email";
    }

    @Override
    public SsoUserInfo getUserInfo(String authCode) {
        try {
            // 1. 获取访问令牌
            GitHubTokenResponse tokenResponse = getAccessToken(authCode);
            if (tokenResponse == null || !StringUtils.hasText(tokenResponse.getAccessToken())) {
                throw new BusinessException("获取GitHub访问令牌失败");
            }

            // 2. 获取用户信息
            GitHubUserInfo userInfo = getGitHubUserInfo(tokenResponse.getAccessToken());
            if (userInfo == null || userInfo.getId() == null) {
                throw new BusinessException("获取GitHub用户信息失败");
            }

            // 3. 如果用户邮箱为空，尝试获取用户主邮箱
            if (!StringUtils.hasText(userInfo.getEmail())) {
                String email = getPrimaryEmail(tokenResponse.getAccessToken());
                userInfo.setEmail(email);
            }

            // 4. 转换为统一的SsoUserInfo
            return new SsoUserInfo(String.valueOf(userInfo.getId()),
                    userInfo.getName() != null ? userInfo.getName() : userInfo.getLogin(), userInfo.getEmail(),
                    userInfo.getAvatarUrl(), "GitHub用户: " + userInfo.getLogin(), SsoProvider.GITHUB);

        } catch (Exception e) {
            logger.error("GitHub SSO登录失败", e);
            throw new BusinessException("GitHub SSO登录失败: " + e.getMessage());
        }
    }

    @Override
    public SsoProvider getProvider() {
        return SsoProvider.GITHUB;
    }

    private GitHubTokenResponse getAccessToken(String code) {
        SsoConfigProvider.GitHubSsoConfig config = getEffectiveConfig();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(config.getTokenUrl());

            // 设置请求头
            httpPost.setHeader(HttpHeaders.ACCEPT, "application/json");
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            // 设置请求参数
            Map<String, String> params = new HashMap<>();
            params.put("client_id", config.getClientId());
            params.put("client_secret", config.getClientSecret());
            params.put("code", code);
            params.put("redirect_uri", config.getRedirectUri());

            String paramJson = JSON.toJSONString(params);
            httpPost.setEntity(new StringEntity(paramJson, StandardCharsets.UTF_8));

            // 发送请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    return JSON.parseObject(result, GitHubTokenResponse.class);
                }
            }
        } catch (IOException e) {
            logger.error("获取GitHub访问令牌失败", e);
        }
        return null;
    }

    private GitHubUserInfo getGitHubUserInfo(String accessToken) {
        SsoConfigProvider.GitHubSsoConfig config = getEffectiveConfig();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(config.getUserInfoUrl());

            // 设置请求头
            httpGet.setHeader(HttpHeaders.ACCEPT, "application/json");
            httpGet.setHeader(HttpHeaders.AUTHORIZATION, "token " + accessToken);

            // 发送请求
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    return JSON.parseObject(result, GitHubUserInfo.class);
                }
            }
        } catch (IOException e) {
            logger.error("获取GitHub用户信息失败", e);
        }
        return null;
    }

    private String getPrimaryEmail(String accessToken) {
        SsoConfigProvider.GitHubSsoConfig config = getEffectiveConfig();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(config.getUserEmailUrl());

            // 设置请求头
            httpGet.setHeader(HttpHeaders.ACCEPT, "application/json");
            httpGet.setHeader(HttpHeaders.AUTHORIZATION, "token " + accessToken);

            // 发送请求
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    // 解析邮箱列表，查找主邮箱
                    return JSON.parseArray(result).stream()
                            .filter(item -> item instanceof Map
                                    && Boolean.TRUE.equals(((Map<?, ?>) item).get("primary")))
                            .map(item -> (String) ((Map<?, ?>) item).get("email")).findFirst().orElse(null);
                }
            }
        } catch (IOException e) {
            logger.error("获取GitHub用户邮箱失败", e);
        }
        return null;
    }

    /** 获取有效的配置（数据库优先，配置文件回退）
     * 
     * @return 有效的GitHub配置 */
    private SsoConfigProvider.GitHubSsoConfig getEffectiveConfig() {
        SsoConfigProvider.GitHubSsoConfig dbConfig = ssoConfigProvider.getGitHubConfig();

        // 如果数据库配置完整，使用数据库配置
        if (dbConfig.getClientId() != null && dbConfig.getClientSecret() != null) {
            return dbConfig;
        }

        // 否则回退到配置文件配置
        SsoConfigProvider.GitHubSsoConfig fileConfig = new SsoConfigProvider.GitHubSsoConfig();
        fileConfig.setClientId(githubProperties.getClientId());
        fileConfig.setClientSecret(githubProperties.getClientSecret());
        fileConfig.setRedirectUri(githubProperties.getRedirectUri());
        fileConfig.setAuthorizeUrl(githubProperties.getAuthorizeUrl());
        fileConfig.setTokenUrl(githubProperties.getTokenUrl());
        fileConfig.setUserInfoUrl(githubProperties.getUserInfoUrl());
        fileConfig.setUserEmailUrl(githubProperties.getUserEmailUrl());

        return fileConfig;
    }
}