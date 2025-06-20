package org.xhy.application.user.service;

import org.springframework.stereotype.Service;
import org.xhy.domain.sso.model.SsoProvider;
import org.xhy.domain.sso.model.SsoUserInfo;
import org.xhy.domain.sso.service.SsoService;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.service.UserDomainService;
import org.xhy.infrastructure.sso.SsoServiceFactory;
import org.xhy.infrastructure.utils.JwtUtils;
import org.xhy.infrastructure.utils.PasswordUtils;

import java.util.UUID;

@Service
public class SsoAppService {

    private final SsoServiceFactory ssoServiceFactory;
    private final UserDomainService userDomainService;

    public SsoAppService(SsoServiceFactory ssoServiceFactory, UserDomainService userDomainService) {
        this.ssoServiceFactory = ssoServiceFactory;
        this.userDomainService = userDomainService;
    }

    public String getSsoLoginUrl(String provider, String redirectUrl) {
        SsoService ssoService = ssoServiceFactory.getSsoService(provider);
        return ssoService.getLoginUrl(redirectUrl);
    }

    public String handleSsoCallback(String provider, String authCode) {
        SsoService ssoService = ssoServiceFactory.getSsoService(provider);
        SsoUserInfo ssoUserInfo = ssoService.getUserInfo(authCode);

        // 根据SSO用户信息创建或获取本地用户
        UserEntity userEntity = findOrCreateUser(ssoUserInfo);

        // 生成JWT token
        return JwtUtils.generateToken(userEntity.getId());
    }

    private UserEntity findOrCreateUser(SsoUserInfo ssoUserInfo) {
        UserEntity existingUser = null;

        // GitHub用户优先通过GitHub ID查找
        if (ssoUserInfo.getProvider() == SsoProvider.GITHUB) {
            existingUser = userDomainService.findUserByGithubId(ssoUserInfo.getId());
        }

        // 如果通过特定ID没找到，再通过邮箱查找
        if (existingUser == null && ssoUserInfo.getEmail() != null) {
            existingUser = userDomainService.findUserByAccount(ssoUserInfo.getEmail());
        }

        if (existingUser != null) {
            // 用户已存在，更新用户信息
            updateUserFromSso(existingUser, ssoUserInfo);
            return existingUser;
        } else {
            // 用户不存在，创建新用户
            return createUserFromSso(ssoUserInfo);
        }
    }

    private void updateUserFromSso(UserEntity user, SsoUserInfo ssoUserInfo) {
        boolean needUpdate = false;

        // 更新用户头像和昵称（如果SSO提供的信息更新）
        if (ssoUserInfo.getName() != null && !ssoUserInfo.getName().equals(user.getNickname())) {
            user.setNickname(ssoUserInfo.getName());
            needUpdate = true;
        }
        if (ssoUserInfo.getAvatar() != null && !ssoUserInfo.getAvatar().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(ssoUserInfo.getAvatar());
            needUpdate = true;
        }

        // GitHub用户需要更新GitHub相关信息
        if (ssoUserInfo.getProvider() == SsoProvider.GITHUB) {
            if (!ssoUserInfo.getId().equals(user.getGithubId())) {
                user.setGithubId(ssoUserInfo.getId());
                needUpdate = true;
            }
        }

        if (needUpdate) {
            userDomainService.updateUserInfo(user);
        }
    }

    private UserEntity createUserFromSso(SsoUserInfo ssoUserInfo) {
        // 通过SSO信息创建新用户
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(ssoUserInfo.getEmail());
        userEntity.setNickname(
                ssoUserInfo.getName() != null ? ssoUserInfo.getName() : "sso-user-" + System.currentTimeMillis());
        userEntity.setAvatarUrl(ssoUserInfo.getAvatar());

        // 设置提供商特定的信息
        if (ssoUserInfo.getProvider() == SsoProvider.GITHUB) {
            userEntity.setGithubId(ssoUserInfo.getId());
            // 可以从描述中提取GitHub登录名
            if (ssoUserInfo.getDesc() != null && ssoUserInfo.getDesc().startsWith("GitHub用户: ")) {
                userEntity.setGithubLogin(ssoUserInfo.getDesc().substring("GitHub用户: ".length()));
            }
        }

        // SSO用户生成一个随机密码并加密（用户无法知道这个密码，只能通过SSO登录）
        String randomPassword = "SSO_" + UUID.randomUUID().toString().replace("-", "");
        userEntity.setPassword(PasswordUtils.encode(randomPassword));

        userDomainService.createDefaultUser(userEntity);
        return userEntity;
    }
}