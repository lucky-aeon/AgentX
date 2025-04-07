package org.xhy.application.llm.assembler;

import org.xhy.application.llm.dto.ModelDTO;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.interfaces.dto.llm.request.ModelCreateRequest;
import org.xhy.interfaces.dto.llm.request.ModelUpdateRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型对象转换器
 */
public class ModelAssembler {
    

    /**
     * 将领域对象转换为DTO
     */
    public static ModelDTO toDTO(ModelEntity model) {
        if (model == null) {
            return null;
        }
        
        ModelDTO dto = new ModelDTO();
        dto.setId(model.getId());
        dto.setUserId(model.getUserId());
        dto.setProviderId(model.getProviderId());
        dto.setModelId(model.getModelId());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        dto.setType(model.getType());
        dto.setStatus(model.getStatus());
        dto.setCreatedAt(model.getCreatedAt());
        dto.setUpdatedAt(model.getUpdatedAt());
        dto.setIsOfficial(model.getOfficial());
        return dto;
    }
    
    /**
     * 将多个领域对象转换为DTO列表
     */
    public static List<ModelDTO> toDTOs(List<ModelEntity> models) {
        if (models == null || models.isEmpty()) {
            return Collections.emptyList();
        }
        return models.stream()
                .map(ModelAssembler::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将创建请求转换为领域对象
     */
    public static ModelEntity toEntity(ModelCreateRequest request, String userId) {
        ModelEntity model = new ModelEntity();
        model.setUserId(userId);
        model.setProviderId(request.getProviderId());
        model.setModelId(request.getModelId());
        model.setName(request.getName());
        model.setDescription(request.getDescription());
        model.setType(request.getType());
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());
        
        return model;
    }


    public static ModelEntity toEntity(ModelUpdateRequest request, String userId) {
        ModelEntity model = new ModelEntity();
        model.setUserId(userId);
        model.setName(request.getName());
        model.setDescription(request.getDescription());
        model.setModelId(request.getModelId());
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());
        model.setId(request.getId());

        return model;
    }

} 