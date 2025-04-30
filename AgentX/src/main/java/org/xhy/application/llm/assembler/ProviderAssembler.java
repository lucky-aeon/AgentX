package org.xhy.application.llm.assembler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.xhy.application.llm.dto.ModelDTO;
import org.xhy.application.llm.dto.ProviderDTO;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderAggregate;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.interfaces.dto.llm.request.ProviderCreateRequest;
import org.xhy.interfaces.dto.llm.request.ProviderUpdateRequest;

/** 服务提供商对象转换器 */
public class ProviderAssembler {

    /** 将实体转换为DTO，并进行敏感信息脱敏 */
    public static ProviderDTO toDTO(ProviderEntity provider) {
        if (provider == null) {
            return null;
        }

        ProviderDTO dto = new ProviderDTO();
        dto.setId(provider.getId());
        dto.setProtocol(provider.getProtocol());
        dto.setName(provider.getName());
        dto.setDescription(provider.getDescription());
        dto.setConfig(provider.getConfig());
        dto.setIsOfficial(provider.getIsOfficial());
        dto.setStatus(provider.getStatus());
        dto.setCreatedAt(provider.getCreatedAt());
        dto.setUpdatedAt(provider.getUpdatedAt());

        // 脱敏处理（针对返回前端的场景）
        dto.maskSensitiveInfo();

        return dto;
    }

    /** 将多个聚合根转换为DTO列表 */
    public static List<ProviderDTO> toDTOList(List<ProviderAggregate> providers) {
        return providers.stream().map(ProviderAssembler::toDTO).collect(Collectors.toList());
    }

    /** 将多个实体转换为DTO列表 */
    public static List<ProviderDTO> toDTOListFromEntities(List<ProviderEntity> providers) {
        return providers.stream().map(ProviderAssembler::toDTO).collect(Collectors.toList());
    }

    /** 将创建请求转换为实体 */
    public static ProviderEntity toEntity(ProviderCreateRequest request, String userId) {
        ProviderEntity provider = new ProviderEntity();
        provider.setUserId(userId);
        provider.setProtocol(request.getProtocol());
        provider.setName(request.getName());
        provider.setDescription(request.getDescription());
        provider.setConfig(request.getConfig()); // 会自动加密
        provider.setStatus(request.getStatus());
        provider.setCreatedAt(LocalDateTime.now());
        provider.setUpdatedAt(LocalDateTime.now());

        return provider;
    }

    /** 将更新请求转换为实体 */
    public static ProviderEntity toEntity(ProviderUpdateRequest request, String userId) {
        ProviderEntity provider = new ProviderEntity();
        provider.setId(request.getId());
        provider.setUserId(userId);
        provider.setProtocol(request.getProtocol());
        provider.setName(request.getName());
        provider.setDescription(request.getDescription());
        provider.setConfig(request.getConfig()); // 会自动加密
        provider.setStatus(request.getStatus());
        provider.setUpdatedAt(LocalDateTime.now());
        return provider;
    }

    /** 根据更新请求更新实体 */
    public static void updateEntity(ProviderEntity entity, ProviderUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            entity.setName(request.getName());
        }

        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }

        if (request.getConfig() != null) {
            entity.setConfig(request.getConfig()); // 会自动加密
        }

        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        entity.setUpdatedAt(LocalDateTime.now());
    }

    // 将聚合根转换为dto
    public static ProviderDTO toDTO(ProviderAggregate provider) {
        if (provider == null) {
            return null;
        }
        ProviderDTO dto = toDTO(provider.getEntity());

        List<ModelEntity> models = provider.getModels();
        if (models == null || models.isEmpty()) {
            return dto;
        }
        for (ModelEntity model : models) {
            ModelDTO modelDTO = ModelAssembler.toDTO(model);
            modelDTO.setIsOfficial(provider.getIsOfficial());
            dto.getModels().add(modelDTO);
        }
        return dto;
    }
}
