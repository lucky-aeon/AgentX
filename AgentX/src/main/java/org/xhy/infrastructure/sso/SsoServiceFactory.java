package org.xhy.infrastructure.sso;

import org.springframework.stereotype.Component;
import org.xhy.domain.auth.constant.AuthFeatureKey;
import org.xhy.domain.auth.service.AuthSettingDomainService;
import org.xhy.domain.sso.model.SsoProvider;
import org.xhy.domain.sso.service.SsoService;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SsoServiceFactory {

    private final Map<SsoProvider, SsoService> ssoServiceMap;
    private final AuthSettingDomainService authSettingDomainService;

    public SsoServiceFactory(List<SsoService> ssoServices, AuthSettingDomainService authSettingDomainService) {
        this.ssoServiceMap = ssoServices.stream()
                .collect(Collectors.toMap(SsoService::getProvider, Function.identity()));
        this.authSettingDomainService = authSettingDomainService;
    }

    public SsoService getSsoService(SsoProvider provider) {
        SsoService ssoService = ssoServiceMap.get(provider);
        if (ssoService == null) {
            throw new BusinessException("不支持的SSO提供商: " + provider.getName());
        }

        // 检查SSO提供商是否启用
        AuthFeatureKey featureKey = getAuthFeatureKeyByProvider(provider);
        if (featureKey != null && !authSettingDomainService.isFeatureEnabled(featureKey)) {
            throw new BusinessException("SSO提供商已禁用: " + provider.getName());
        }

        return ssoService;
    }

    public SsoService getSsoService(String providerCode) {
        return getSsoService(SsoProvider.fromCode(providerCode));
    }

    public List<SsoProvider> getSupportedProviders() {
        return ssoServiceMap.keySet().stream().filter(this::isProviderEnabled).collect(Collectors.toList());
    }

    private boolean isProviderEnabled(SsoProvider provider) {
        AuthFeatureKey featureKey = getAuthFeatureKeyByProvider(provider);
        return featureKey == null || authSettingDomainService.isFeatureEnabled(featureKey);
    }

    private AuthFeatureKey getAuthFeatureKeyByProvider(SsoProvider provider) {
        switch (provider) {
            case GITHUB :
                return AuthFeatureKey.GITHUB_LOGIN;
            case COMMUNITY :
                return AuthFeatureKey.COMMUNITY_LOGIN;
            default :
                return null;
        }
    }
}