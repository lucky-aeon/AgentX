package org.xhy.application.agent.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.agent.dto.AgentVersionDTO;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.model.AgentVersionEntity;
import org.xhy.interfaces.dto.agent.request.PublishAgentVersionRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AgentVersionAssembler {

    /** 将AgentVersionEntity列表转换为AgentVersionDTO列表 */
    public static List<AgentVersionDTO> toVersionDTOList(List<AgentVersionEntity> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }

        List<AgentVersionDTO> dtoList = new ArrayList<>(entities.size());
        for (AgentVersionEntity entity : entities) {
            dtoList.add(toDTO(entity));
        }

        return dtoList;
    }

    /** 将AgentVersionEntity转换为AgentVersionDTO */
    public static AgentVersionDTO toDTO(AgentVersionEntity entity) {
        if (entity == null) {
            return null;
        }

        AgentVersionDTO dto = new AgentVersionDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    /** 创建AgentVersionEntity */
    public static AgentVersionEntity createVersionEntity(AgentEntity agent, PublishAgentVersionRequest request) {
        return AgentVersionEntity.createFromAgent(agent, request.getVersionNumber(), request.getChangeLog());
    }

    public static List<AgentVersionDTO> toDTOs(List<AgentVersionEntity> agents) {
        if (agents == null || agents.isEmpty()) {
            return Collections.emptyList();
        }
        return agents.stream().map(AgentVersionAssembler::toDTO).collect(Collectors.toList());
    }

}
