package org.xhy.application.llm.assembler;

import org.springframework.stereotype.Component;
import org.xhy.application.llm.dto.ModelDTO;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.repository.ProviderRepository;
import org.xhy.interfaces.dto.llm.ModelCreateRequest;
import org.xhy.interfaces.dto.llm.ModelUpdateRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型对象转换器
 */
@Component
public class ModelAssembler {
    
    private final ProviderRepository providerRepository;
    
    public ModelAssembler(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }
    
    /**
     * 将领域对象转换为DTO
     */
    public ModelDTO toDTO(ModelEntity model) {
        if (model == null) {
            return null;
        }
        
        ModelDTO dto = new ModelDTO();
        dto.setId(model.getId());
        dto.setUserId(model.getUserId());
        dto.setProviderId(model.getProviderId());
        dto.setCode(model.getCode());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        dto.setType(model.getType());
        dto.setConfig(model.getConfig());
        dto.setIsOfficial(model.getIsOfficial());
        dto.setStatus(model.getStatus());
        dto.setCreatedAt(model.getCreatedAt());
        dto.setUpdatedAt(model.getUpdatedAt());
        
        // 获取Provider名称
        if (model.getProviderId() != null) {
            ProviderEntity provider = providerRepository.selectById(model.getProviderId());
            if (provider != null) {
                dto.setProviderName(provider.getName());
            }
        }
        
        return dto;
    }
    
    /**
     * 将多个领域对象转换为DTO列表
     */
    public List<ModelDTO> toDTOList(List<ModelEntity> models) {
        return models.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将创建请求转换为领域对象
     */
    public ModelEntity toEntity(ModelCreateRequest request, String userId) {
        ModelEntity model = new ModelEntity();
        model.setUserId(userId);
        model.setProviderId(request.getProviderId());
        model.setCode(request.getCode());
        model.setName(request.getName());
        model.setDescription(request.getDescription());
        model.setType(request.getType());
        model.setConfig(request.getConfig());
        model.setIsOfficial(request.getIsOfficial());
        model.setStatus(request.getStatus());
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());
        
        return model;
    }
    
    /**
     * 更新领域对象
     */
    public void updateEntity(ModelEntity model, ModelUpdateRequest request) {
        if (request.getName() != null) {
            model.setName(request.getName());
        }
        
        if (request.getDescription() != null) {
            model.setDescription(request.getDescription());
        }
        
        if (request.getConfig() != null) {
            model.setConfig(request.getConfig());
        }
        
        if (request.getStatus() != null) {
            model.setStatus(request.getStatus());
        }
        
        model.setUpdatedAt(LocalDateTime.now());
    }
} 