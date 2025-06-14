package org.xhy.application.apikey.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.apikey.dto.ApiKeyDTO;
import org.xhy.domain.apikey.model.ApiKeyEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** API密钥实体转换器 */
public class ApiKeyAssembler {

    /** 将API密钥实体转换为DTO
     *
     * @param entity API密钥实体
     * @return API密钥DTO */
    public static ApiKeyDTO toDTO(ApiKeyEntity entity) {
        if (entity == null) {
            return null;
        }

        ApiKeyDTO dto = new ApiKeyDTO();
        BeanUtils.copyProperties(entity, dto);

        // 设置计算字段
        dto.setExpired(entity.isExpired());
        dto.setAvailable(entity.isAvailable());

        return dto;
    }

    /** 将API密钥实体列表转换为DTO列表
     *
     * @param entities API密钥实体列表
     * @return API密钥DTO列表 */
    public static List<ApiKeyDTO> toDTOs(List<ApiKeyEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(ApiKeyAssembler::toDTO).collect(Collectors.toList());
    }

    /** 将DTO转换为API密钥实体（用于创建）
     *
     * @param dto API密钥DTO
     * @return API密钥实体 */
    public static ApiKeyEntity toEntity(ApiKeyDTO dto) {
        if (dto == null) {
            return null;
        }

        ApiKeyEntity entity = new ApiKeyEntity();
        BeanUtils.copyProperties(dto, entity);

        return entity;
    }
}