package org.xhy.application.plugins.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.xhy.application.plugins.assembler.PluginAssembler;
import org.xhy.application.plugins.dto.PluginDTO;
import org.xhy.application.plugins.dto.PluginSubmissionCommand;
import org.xhy.domain.plugins.constant.PluginStatus;
import org.xhy.domain.plugins.model.PluginEntity;
import org.xhy.domain.plugins.service.PluginManagerDomainService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.infrastructure.exception.BusinessException;

/**
 * 插件管理服务, 用户管理插件
 */
@Service
public class PluginManagerAppService {

    private final PluginManagerDomainService pluginDomainService;

    public PluginManagerAppService(PluginManagerDomainService pluginDomainService) {
        this.pluginDomainService = pluginDomainService;
    }

    /**
     * 提交插件申请
     */
    public PluginDTO submitPlugin(PluginSubmissionCommand command) {
        return PluginAssembler.toDTO(
            pluginDomainService.submitPlugin(
                command.getName(),
                command.getDescription(),
                command.getRepositoryUrl(),
                command.getLicense(),
                command.getConfig()
            )
        );
    }

    /**
     * 获取插件列表
     */
    public List<PluginDTO> listPlugins(String name, PluginStatus status, Integer page, Integer size) {
        String userId = UserContext.getCurrentUserId();
        return pluginDomainService.listPlugins(userId, name, status, page, size).stream()
            .map(PluginAssembler::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * 获取插件详情
     */
    public PluginDTO getPluginDetail(String id) {
        String userId = UserContext.getCurrentUserId();
        PluginEntity pluginDetail = pluginDomainService.getPluginDetail(id);
        if (pluginDetail == null) {
            throw new BusinessException("插件不存在");
        }
        if (!userId.equals(pluginDetail.getUserId())) {
            throw new BusinessException("无权限访问");
        }
        return PluginAssembler.toDTO(pluginDetail);
    }

    /**
     * 更新插件
     */
    public PluginDTO updatePlugin(String id, PluginSubmissionCommand command) {
        return PluginAssembler.toDTO(
            pluginDomainService.updatePlugin(
                id,
                command.getName(),
                command.getDescription(),
                command.getRepositoryUrl(),
                command.getLicense(),
                command.getConfig()
            )
        );
    }

    /**
     * 删除插件
     */
    public void deletePlugin(String id) {
        pluginDomainService.deletePlugin(id);
    }
}
