package org.xhy.application.user.service;

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
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.service.UserDomainService;
import org.xhy.infrastructure.config.GitHubOAuthProperties;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.utils.JwtUtils;
import org.xhy.interfaces.dto.user.GitHubTokenResponse;
import org.xhy.interfaces.dto.user.GitHubUserInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OAuthAppService {

    private static final Logger logger = LoggerFactory.getLogger(OAuthAppService.class);

    private final GitHubOAuthProperties githubProperties;
    private final UserDomainService userDomainService;

    public OAuthAppService(GitHubOAuthProperties githubProperties, UserDomainService userDomainService) {
        this.githubProperties = githubProperties;
        this.userDomainService = userDomainService;
    }

    /** 获取 GitHub 授权 URL
     * @return GitHub 授权 URL */
    public String getGitHubAuthorizeUrl() {
        return githubProperties.getAuthorizeUrl() + "?client_id=" + githubProperties.getClientId() + "&redirect_uri="
                + githubProperties.getRedirectUri() + "&scope=user:email";
    }

    /** 处理 GitHub 回调
     * @param code 授权码
     * @return 登录令牌 */
    public Map<String, String> handleGitHubCallback(String code) {
        try {
            // 1. 获取访问令牌
            GitHubTokenResponse tokenResponse = getAccessToken(code);
            if (tokenResponse == null || !StringUtils.hasText(tokenResponse.getAccessToken())) {
                throw new BusinessException("获取GitHub访问令牌失败");
            }

            // 2. 获取用户信息
            GitHubUserInfo userInfo = getUserInfo(tokenResponse.getAccessToken());
            if (userInfo == null || userInfo.getId() == null) {
                throw new BusinessException("获取GitHub用户信息失败");
            }

            // 3. 如果用户邮箱为空，尝试获取用户主邮箱
            if (!StringUtils.hasText(userInfo.getEmail())) {
                String email = getPrimaryEmail(tokenResponse.getAccessToken());
                userInfo.setEmail(email);
            }

            // 4. 查找或创建用户
            UserEntity user = findOrCreateUser(userInfo);

            // 5. 生成JWT令牌
            String token = JwtUtils.generateToken(user.getId());

            Map<String, String> result = new HashMap<>();
            result.put("token", token);
            return result;
        } catch (Exception e) {
            logger.error("GitHub登录处理失败", e);
            throw new BusinessException("GitHub登录失败: " + e.getMessage());
        }
    }

    /** 获取访问令牌
     * @param code 授权码
     * @return 访问令牌响应 */
    private GitHubTokenResponse getAccessToken(String code) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(githubProperties.getTokenUrl());

            // 设置请求头
            httpPost.setHeader(HttpHeaders.ACCEPT, "application/json");
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            // 设置请求参数
            Map<String, String> params = new HashMap<>();
            params.put("client_id", githubProperties.getClientId());
            params.put("client_secret", githubProperties.getClientSecret());
            params.put("code", code);
            params.put("redirect_uri", githubProperties.getRedirectUri());

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

    /** 获取用户信息
     * @param accessToken 访问令牌
     * @return 用户信息 */
    private GitHubUserInfo getUserInfo(String accessToken) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(githubProperties.getUserInfoUrl());

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

    /** 获取用户主邮箱
     * @param accessToken 访问令牌
     * @return 主邮箱 */
    private String getPrimaryEmail(String accessToken) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(githubProperties.getUserEmailUrl());

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

    /** 查找或创建用户
     * @param userInfo GitHub用户信息
     * @return 用户实体 */
    private UserEntity findOrCreateUser(GitHubUserInfo userInfo) {
        // 先通过 GitHub ID 查找用户
        UserEntity user = userDomainService.findUserByGithubId(String.valueOf(userInfo.getId()));

        // 如果用户不存在且有邮箱，尝试通过邮箱查找
        if (user == null && StringUtils.hasText(userInfo.getEmail())) {
            user = userDomainService.findUserByAccount(userInfo.getEmail());
        }

        // 如果用户存在，更新 GitHub 相关信息
        if (user != null) {
            user.setGithubId(String.valueOf(userInfo.getId()));
            user.setGithubLogin(userInfo.getLogin());
            if (StringUtils.hasText(userInfo.getAvatarUrl())) {
                user.setAvatarUrl(userInfo.getAvatarUrl());
            }
            userDomainService.updateUserInfo(user);
            return user;
        }

        // 创建新用户
        UserEntity newUser = new UserEntity();
        newUser.setGithubId(String.valueOf(userInfo.getId()));
        newUser.setGithubLogin(userInfo.getLogin());
        newUser.setNickname(userInfo.getName() != null ? userInfo.getName() : "github-" + userInfo.getLogin());
        newUser.setEmail(userInfo.getEmail());
        newUser.setAvatarUrl(userInfo.getAvatarUrl());
        // 生成随机密码（用户无需知道，因为使用GitHub登录）
        newUser.setPassword(generateRandomPassword());

        return userDomainService.register(newUser.getEmail(), null, newUser.getPassword());
    }

    /** 生成随机密码
     * @return 随机密码 */
    private String generateRandomPassword() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
        StringBuilder password = new StringBuilder();
        Random random = new Random();

        // 生成16位随机密码
        for (int i = 0; i < 16; i++) {
            password.append(characters.charAt(random.nextInt(characters.length())));
        }

        return password.toString();
    }
}