package org.xhy.application.llm.assembler;

import org.springframework.stereotype.Component;
import org.xhy.application.llm.dto.ProviderDTO;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.interfaces.dto.llm.ProviderCreateRequest;
import org.xhy.interfaces.dto.llm.ProviderUpdateRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务提供商对象转换器
 */
@Component
public class ProviderAssembler {
    
    /**
     * 将领域对象转换为DTO
     */
    public static ProviderDTO toDTO(ProviderEntity provider) {
        if (provider == null) {
            return null;
        }
        
        ProviderDTO dto = new ProviderDTO();
        dto.setId(provider.getId());
        dto.setUserId(provider.getUserId());
        dto.setCode(provider.getCode());
        dto.setName(provider.getName());
        dto.setDescription(provider.getDescription());
        dto.setConfig(provider.getConfig());
        dto.setIsOfficial(provider.getIsOfficial());
        dto.setStatus(provider.getStatus());
        dto.setCreatedAt(provider.getCreatedAt());
        dto.setUpdatedAt(provider.getUpdatedAt());
        
        return dto;
    }
    
    /**
     * 将多个领域对象转换为DTO列表
     */
    public static List<ProviderDTO> toDTOList(List<ProviderEntity> providers) {
        return providers.stream()
                .map(ProviderAssembler::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将创建请求转换为领域对象
     */
    public static ProviderEntity toEntity(ProviderCreateRequest request, String userId) {
        ProviderEntity provider = new ProviderEntity();
        provider.setUserId(userId);
        provider.setCode(request.getCode());
        provider.setName(request.getName());
        provider.setDescription(request.getDescription());
        provider.setConfig(request.getConfig());
        provider.setIsOfficial(request.getIsOfficial());
        provider.setStatus(request.getStatus());
        provider.setCreatedAt(LocalDateTime.now());
        provider.setUpdatedAt(LocalDateTime.now());
        
        return provider;
    }

    public static ProviderEntity toEntity(ProviderUpdateRequest request, String userId) {
        ProviderEntity provider = new ProviderEntity();
        provider.setUserId(userId);
        provider.setName(request.getName());
        provider.setDescription(request.getDescription());
        provider.setConfig(request.getConfig());
        provider.setStatus(request.getStatus());
        provider.setUpdatedAt(LocalDateTime.now());
        return provider;
    }
}