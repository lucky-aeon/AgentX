package org.xhy.application.plugins.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.xhy.application.plugins.assembler.PluginAssembler;
import org.xhy.application.plugins.dto.PluginDTO;
import org.xhy.domain.plugins.constant.PluginStatus;
import org.xhy.domain.plugins.service.PluginUseDomainService;

/**
 * 插件使用服务, 用户使用插件
 */
@Service
public class PluginUseAppService {
    private final PluginUseDomainService pluginUseService;

    public PluginUseAppService(PluginUseDomainService pluginUseService) {
        this.pluginUseService = pluginUseService;
    }

    /**
     * 搜索插件
     */
    public List<PluginDTO> searchPlugins(String name, PluginStatus status, Integer page, Integer size) {
        return pluginUseService.searchPlugins(name, status, page, size).stream()
            .map(PluginAssembler::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * 获取插件详情
     */
    public PluginDTO getPlugin(String id) {
        return PluginAssembler.toDTO(pluginUseService.getPluginDetail(id));
    }
}
