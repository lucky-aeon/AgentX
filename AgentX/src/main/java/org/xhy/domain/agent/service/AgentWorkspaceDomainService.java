package org.xhy.domain.agent.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.model.AgentWorkspaceEntity;
import org.xhy.domain.agent.repository.AgentRepository;
import org.xhy.domain.agent.repository.AgentWorkspaceRepository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.xhy.infrastructure.exception.BusinessException;

@Service
public class AgentWorkspaceDomainService {

    private final AgentWorkspaceRepository agentWorkspaceRepository;

    private final AgentRepository agentRepository;

    public AgentWorkspaceDomainService(AgentWorkspaceRepository agentWorkspaceRepository,
                                       AgentDomainService agentServiceDomainService, AgentRepository agentRepository) {
        this.agentWorkspaceRepository = agentWorkspaceRepository;
        this.agentRepository = agentRepository;
    }

    public List<AgentEntity> getWorkspaceAgents(String userId) {

        LambdaQueryWrapper<AgentWorkspaceEntity> wrapper = Wrappers.<AgentWorkspaceEntity>lambdaQuery()
                .eq(AgentWorkspaceEntity::getUserId, userId).select(AgentWorkspaceEntity::getAgentId);

        List<String> agentIds = agentWorkspaceRepository.selectList(wrapper).stream()
                .map(AgentWorkspaceEntity::getAgentId).collect(Collectors.toList());

        if (agentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return agentRepository.selectByIds(agentIds);

    }

    public boolean exist(String agentId, String userId) {
            Wrapper<AgentWorkspaceEntity> wrapper = Wrappers.<AgentWorkspaceEntity>lambdaQuery()
                            .eq(AgentWorkspaceEntity::getAgentId, agentId)
                            .eq(AgentWorkspaceEntity::getUserId, userId);

        Long l = agentWorkspaceRepository.selectCount(wrapper);
        return  l > 0;
    }

    public boolean deleteAgent(String agentId, String userId) {
        return agentWorkspaceRepository.delete(Wrappers.<AgentWorkspaceEntity>lambdaQuery()
                .eq(AgentWorkspaceEntity::getAgentId, agentId).eq(AgentWorkspaceEntity::getUserId, userId)) > 0;
    }

    public AgentWorkspaceEntity getWorkspace(String agentId, String userId) {
        Wrapper<AgentWorkspaceEntity> wrapper = Wrappers.<AgentWorkspaceEntity>lambdaQuery()
                .eq(AgentWorkspaceEntity::getAgentId, agentId)
                .eq(AgentWorkspaceEntity::getUserId, userId);
        AgentWorkspaceEntity agentWorkspaceEntity = agentWorkspaceRepository.selectOne(wrapper);
        if (agentWorkspaceEntity ==null){
            throw new BusinessException("助理不存在");
        }
        return agentWorkspaceEntity;
    }

    public AgentWorkspaceEntity findWorkspace(String agentId, String userId) {
        Wrapper<AgentWorkspaceEntity> wrapper = Wrappers.<AgentWorkspaceEntity>lambdaQuery()
                .eq(AgentWorkspaceEntity::getAgentId, agentId)
                .eq(AgentWorkspaceEntity::getUserId, userId);
        return agentWorkspaceRepository.selectOne(wrapper);
    }

    public void save(AgentWorkspaceEntity workspace){

        agentWorkspaceRepository.checkInsert(workspace);
    }

    public void update(AgentWorkspaceEntity workspace) {
        LambdaUpdateWrapper<AgentWorkspaceEntity> wrapper = Wrappers.<AgentWorkspaceEntity>lambdaUpdate()
                .eq(AgentWorkspaceEntity::getAgentId, workspace.getAgentId())
                .eq(AgentWorkspaceEntity::getAgentId, workspace.getAgentId());
        agentWorkspaceRepository.checkedUpdate(workspace,wrapper);
    }
}
