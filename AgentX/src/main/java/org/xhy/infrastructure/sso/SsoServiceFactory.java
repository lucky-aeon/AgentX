package org.xhy.infrastructure.sso;

import org.springframework.stereotype.Component;
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

    public SsoServiceFactory(List<SsoService> ssoServices) {
        this.ssoServiceMap = ssoServices.stream()
                .collect(Collectors.toMap(SsoService::getProvider, Function.identity()));
    }

    public SsoService getSsoService(SsoProvider provider) {
        SsoService ssoService = ssoServiceMap.get(provider);
        if (ssoService == null) {
            throw new BusinessException("不支持的SSO提供商: " + provider.getName());
        }
        return ssoService;
    }

    public SsoService getSsoService(String providerCode) {
        return getSsoService(SsoProvider.fromCode(providerCode));
    }

    public List<SsoProvider> getSupportedProviders() {
        return List.copyOf(ssoServiceMap.keySet());
    }
}