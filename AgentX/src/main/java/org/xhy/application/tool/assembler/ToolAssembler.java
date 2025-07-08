package org.xhy.application.tool.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.tool.dto.ToolDTO;
import org.xhy.application.tool.dto.ToolVersionDTO;
import org.xhy.application.tool.dto.ToolWithUserDTO;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.constant.ToolType;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.ToolVersionEntity;
import org.xhy.domain.tool.model.UserToolEntity;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.infrastructure.utils.JsonUtils;
import org.xhy.interfaces.dto.tool.request.CreateToolRequest;
import org.xhy.interfaces.dto.tool.request.UpdateToolRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 工具实体转换器 */
public class ToolAssembler {

    /** 将创建工具请求转换为工具实体
     *
     * @param request 创建工具请求
     * @param userId 用户ID
     * @return 工具实体 */
    public static ToolEntity toEntity(CreateToolRequest request, String userId) {
        ToolEntity toolEntity = new ToolEntity();
        BeanUtils.copyProperties(request, toolEntity);
        toolEntity.setUserId(userId);

        return toolEntity;
    }

    /** 将工具实体转换为DTO
     *
     * @param entity 工具实体
     * @return 工具DTO */
    public static ToolDTO toDTO(ToolEntity entity) {
        ToolDTO toolDTO = new ToolDTO();
        BeanUtils.copyProperties(entity, toolDTO);
        toolDTO.setInstallCommand(JsonUtils.toJsonString(entity.getInstallCommand()));
        return toolDTO;
    }

    public static ToolVersionDTO toDTO(ToolVersionEntity entity) {
        ToolVersionDTO toolVersionDTO = new ToolVersionDTO();
        BeanUtils.copyProperties(entity, toolVersionDTO);
        return toolVersionDTO;
    }

    /** 将工具实体列表转换为DTO列表
     *
     * @param entities 工具实体列表
     * @return 工具DTO列表 */
    public static List<ToolDTO> toDTOs(List<ToolEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(ToolAssembler::toDTO).collect(Collectors.toList());
    }

    public static ToolEntity toEntity(UpdateToolRequest request, String userId) {
        ToolEntity toolEntity = new ToolEntity();
        BeanUtils.copyProperties(request, toolEntity);
        toolEntity.setUserId(userId);
        return toolEntity;
    }

    public static ToolVersionDTO toDTO(UserToolEntity userToolEntity) {
        ToolVersionDTO toolVersionDTO = new ToolVersionDTO();
        BeanUtils.copyProperties(userToolEntity, toolVersionDTO);
        return toolVersionDTO;
    }

    /** 将工具实体转换为包含用户信息的DTO
     *
     * @param entity 工具实体
     * @param user 用户实体
     * @return 包含用户信息的工具DTO */
    public static ToolWithUserDTO toToolWithUserDTO(ToolEntity entity, UserEntity user) {
        if (entity == null) {
            return null;
        }

        ToolWithUserDTO dto = new ToolWithUserDTO();
        BeanUtils.copyProperties(entity, dto);

        // 设置用户信息
        if (user != null) {
            dto.setUserNickname(user.getNickname());
            dto.setUserEmail(user.getEmail());
            dto.setUserAvatarUrl(user.getAvatarUrl());
        }

        return dto;
    }

    /** 将工具实体列表转换为包含用户信息的DTO列表
     *
     * @param entities 工具实体列表
     * @return 包含用户信息的工具DTO列表 */
    public static List<ToolWithUserDTO> toToolWithUserDTOs(List<ToolEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(entity -> toToolWithUserDTO(entity, null)).collect(Collectors.toList());
    }

}