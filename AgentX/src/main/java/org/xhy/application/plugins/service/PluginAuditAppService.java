package org.xhy.application.plugins.service;

import org.xhy.domain.plugins.service.PluginAuditDomainService;

/**
 * 插件审核应用服务
 */
public class PluginAuditAppService {
    private final PluginAuditDomainService pluginAuditService;

    public PluginAuditAppService(PluginAuditDomainService pluginAuditService) {
        this.pluginAuditService = pluginAuditService;
    }


}
