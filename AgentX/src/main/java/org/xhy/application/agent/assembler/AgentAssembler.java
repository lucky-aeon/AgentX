package org.xhy.application.agent.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.domain.agent.model.AgentEntity;

import org.xhy.application.agent.dto.AgentDTO;
import org.xhy.interfaces.dto.agent.request.CreateAgentRequest;
import org.xhy.interfaces.dto.agent.request.SearchAgentsRequest;
import org.xhy.interfaces.dto.agent.request.UpdateAgentRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Agent领域对象组装器 负责DTO、Entity和Request之间的转换 */
public class AgentAssembler {

    /** 将CreateAgentRequest转换为AgentEntity */
    public static AgentEntity toEntity(CreateAgentRequest request, String userId) {
        AgentEntity entity = new AgentEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setAvatar(request.getAvatar());
        entity.setSystemPrompt(request.getSystemPrompt());
        entity.setWelcomeMessage(request.getWelcomeMessage());

        entity.setUserId(userId);

        // 设置初始状态为启用
        entity.setEnabled(true);

        // 设置工具和知识库ID
        entity.setToolIds(request.getToolIds() != null ? request.getToolIds() : new ArrayList<>());
        entity.setKnowledgeBaseIds(
                request.getKnowledgeBaseIds() != null ? request.getKnowledgeBaseIds() : new ArrayList<>());

        // 设置创建和更新时间
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setToolIds(request.getToolIds());
        // 设置预先设置的工具参数
        entity.setToolPresetParams(request.getToolPresetParams());
        entity.setMultiModal(request.getMultiModal());
        return entity;
    }

    /** 将UpdateAgentRequest转换为AgentEntity */
    public static AgentEntity toEntity(UpdateAgentRequest request, String userId) {
        AgentEntity entity = new AgentEntity();

        BeanUtils.copyProperties(request, entity);
        entity.setUserId(userId);
        return entity;
    }

    /** 将AgentEntity转换为AgentDTO */
    public static AgentDTO toDTO(AgentEntity entity) {
        if (entity == null) {
            return null;
        }
        AgentDTO dto = new AgentDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public static List<AgentDTO> toDTOs(List<AgentEntity> agents) {
        if (agents == null || agents.isEmpty()) {
            return Collections.emptyList();
        }
        return agents.stream().map(AgentAssembler::toDTO).collect(Collectors.toList());
    }

    public static AgentEntity toEntity(SearchAgentsRequest searchAgentsRequest) {
        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setName(searchAgentsRequest.getName());
        return agentEntity;
    }
}