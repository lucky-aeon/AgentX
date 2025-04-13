package org.xhy.domain.plugins.service;

import org.springframework.stereotype.Service;
import org.xhy.domain.plugins.repository.PluginRepository;

@Service
public class PluginAuditDomainService extends PluginBaseDomainService {
    
    public PluginAuditDomainService(PluginRepository pluginRepository) {
        super(pluginRepository);
    }
}
