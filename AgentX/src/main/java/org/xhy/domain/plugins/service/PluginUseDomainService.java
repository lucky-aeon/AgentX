package org.xhy.domain.plugins.service;

import org.springframework.stereotype.Service;
import org.xhy.domain.plugins.repository.PluginRepository;

@Service
public class PluginUseDomainService extends PluginBaseDomainService {

    public PluginUseDomainService(PluginRepository pluginRepository) {
        super(pluginRepository);
    }
}
