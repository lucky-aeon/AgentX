package org.xhy.application.auth.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.auth.dto.AuthSettingDTO;
import org.xhy.application.auth.dto.UpdateAuthSettingRequest;
import org.xhy.domain.auth.model.AuthSettingEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 认证配置转换器 */
public class AuthSettingAssembler {

    /** 实体转DTO
     * 
     * @param entity 实体
     * @return DTO */
    public static AuthSettingDTO toDTO(AuthSettingEntity entity) {
        if (entity == null) {
            return null;
        }

        AuthSettingDTO dto = new AuthSettingDTO();
        BeanUtils.copyProperties(entity, dto);

        // 配置数据已经是Map类型，直接设置
        dto.setConfigData(entity.getConfigData());

        return dto;
    }

    /** 实体列表转DTO列表
     * 
     * @param entities 实体列表
     * @return DTO列表 */
    public static List<AuthSettingDTO> toDTOs(List<AuthSettingEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(AuthSettingAssembler::toDTO).collect(Collectors.toList());
    }

    /** 更新请求转实体（部分更新）
     * 
     * @param entity 原实体
     * @param request 更新请求
     * @return 更新后的实体 */
    public static AuthSettingEntity updateEntity(AuthSettingEntity entity, UpdateAuthSettingRequest request) {
        if (request.getFeatureName() != null) {
            entity.setFeatureName(request.getFeatureName());
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getConfigData() != null) {
            entity.setConfigData(request.getConfigData());
        }

        return entity;
    }
}