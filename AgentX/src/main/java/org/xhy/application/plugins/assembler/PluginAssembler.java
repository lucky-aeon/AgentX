package org.xhy.application.plugins.assembler;

import org.xhy.application.plugins.dto.PluginDTO;
import org.xhy.domain.plugins.model.PluginEntity;

/**
 * 插件对象转换器
 */
public class PluginAssembler {
    
    public static PluginDTO toDTO(PluginEntity entity) {
        if (entity == null) {
            return null;
        }

        PluginDTO dto = new PluginDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setRepositoryUrl(entity.getRepositoryUrl());
        dto.setLicense(entity.getLicense());
        dto.setVersion(entity.getVersion());
        dto.setIcon(entity.getIcon());
        dto.setTags(entity.getTags());
        dto.setConfig(entity.getConfig());
        return dto;
    }
}
