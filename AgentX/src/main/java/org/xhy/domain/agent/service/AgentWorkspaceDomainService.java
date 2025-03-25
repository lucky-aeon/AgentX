package org.xhy.domain.agent.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.xhy.application.agent.assembler.AgentAssembler;
import org.xhy.domain.agent.dto.AgentDTO;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.model.AgentWorkspaceEntity;
import org.xhy.domain.agent.repository.AgentRepository;
import org.xhy.domain.agent.repository.AgentWorkspaceRepository;
import org.xhy.infrastructure.exception.BusinessException;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

@Service
public class AgentWorkspaceDomainService {

    private final AgentWorkspaceRepository agentWorkspaceRepository;

    private final AgentRepository agentRepository;

    public AgentWorkspaceDomainService(AgentWorkspaceRepository agentWorkspaceRepository,
                                       AgentDomainService agentServiceDomainService, AgentRepository agentRepository) {
        this.agentWorkspaceRepository = agentWorkspaceRepository;
        this.agentRepository = agentRepository;
    }

    public List<AgentDTO> getWorkspaceAgents(String userId) {

        LambdaQueryWrapper<AgentWorkspaceEntity> wrapper = Wrappers.<AgentWorkspaceEntity>lambdaQuery()
                .eq(AgentWorkspaceEntity::getUserId, userId).select(AgentWorkspaceEntity::getAgentId);

        List<String> agentIds = agentWorkspaceRepository.selectList(wrapper).stream()
                .map(AgentWorkspaceEntity::getAgentId).collect(Collectors.toList());

        if (agentIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<AgentEntity> agents = agentRepository.selectBatchIds(agentIds);
        return agents.stream().map(AgentAssembler::toDTO).collect(Collectors.toList());
        
    }

    public boolean checkAgentWorkspaceExist(String agentId, String userId) {
        return agentWorkspaceRepository.checkAgentWorkspaceExist(agentId,userId);
    }

    public boolean deleteAgent(String agentId, String userId) {
        return agentWorkspaceRepository.delete(Wrappers.<AgentWorkspaceEntity>lambdaQuery()
                .eq(AgentWorkspaceEntity::getAgentId, agentId).eq(AgentWorkspaceEntity::getUserId, userId)) > 0;
    }
}
