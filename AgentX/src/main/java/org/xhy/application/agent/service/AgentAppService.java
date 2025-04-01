package org.xhy.application.agent.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.agent.assembler.AgentAssembler;
import org.xhy.application.agent.assembler.AgentVersionAssembler;
import org.xhy.application.agent.dto.AgentDTO;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.application.agent.dto.AgentVersionDTO;
import org.xhy.domain.agent.model.AgentVersionEntity;
import org.xhy.domain.agent.model.AgentWorkspaceEntity;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.domain.agent.service.AgentWorkspaceDomainService;
import org.xhy.infrastructure.exception.ParamValidationException;
import org.xhy.interfaces.dto.agent.*;
import org.xhy.domain.agent.constant.PublishStatus;

import java.util.List;

/**
 * Agent应用服务，用于适配领域层的Agent服务
 * 职责：
 * 1. 接收和验证来自接口层的请求
 * 2. 将请求转换为领域对象或参数
 * 3. 调用领域服务执行业务逻辑
 * 4. 转换和返回结果给接口层
 */
@Service
public class AgentAppService {

    private final AgentDomainService agentServiceDomainService;
    private final AgentWorkspaceDomainService agentWorkspaceDomainService;

    public AgentAppService(AgentDomainService agentServiceDomainService, AgentWorkspaceDomainService agentWorkspaceDomainService) {
        this.agentServiceDomainService = agentServiceDomainService;
        this.agentWorkspaceDomainService = agentWorkspaceDomainService;
    }

    /**
     * 创建新Agent
     */
    @Transactional
    public AgentDTO createAgent(CreateAgentRequest request, String userId) {

        // todo xhy 判断用户是否存在

        // 使用组装器创建领域实体
        AgentEntity entity = AgentAssembler.toEntity(request,userId);
        entity.setUserId(userId);
        AgentEntity agent = agentServiceDomainService.createAgent(entity);
        AgentWorkspaceEntity agentWorkspaceEntity = new AgentWorkspaceEntity();
        agentWorkspaceEntity.setAgentId(agent.getId());
        agentWorkspaceEntity.setUserId(userId);
        agentWorkspaceDomainService.save(agentWorkspaceEntity);
        return AgentAssembler.toDTO(agent);
    }

    /**
     * 获取Agent信息
     */
    public AgentDTO getAgent(String agentId, String userId) {
        // todo xhy 判断用户是否存在
        AgentEntity agent = agentServiceDomainService.getAgent(agentId, userId);
        return AgentAssembler.toDTO(agent);
    }

    /**
     * 获取用户的Agent列表，支持状态和名称过滤
     */
    public List<AgentDTO> getUserAgents(String userId, SearchAgentsRequest searchAgentsRequest) {
        AgentEntity entity = AgentAssembler.toEntity(searchAgentsRequest);
        List<AgentEntity> agents = agentServiceDomainService.getUserAgents(userId, entity);
        return AgentAssembler.toDTOs(agents);
    }

    /**
     * 获取已上架的Agent列表，支持名称搜索
     */
    public List<AgentVersionDTO> getPublishedAgentsByName(SearchAgentsRequest searchAgentsRequest) {
        AgentEntity entity = AgentAssembler.toEntity(searchAgentsRequest);
        List<AgentVersionEntity> agentVersionEntities = agentServiceDomainService.getPublishedAgentsByName(entity);
        return AgentVersionAssembler.toDTOs(agentVersionEntities);
    }

    /**
     * 更新Agent信息（基本信息和配置合并更新）
     */
    public AgentDTO updateAgent(UpdateAgentRequest request, String userId) {

        // 使用组装器创建更新实体
        AgentEntity updateEntity = AgentAssembler.toEntity(request,userId);

        updateEntity.setUserId(userId);
        // 调用领域服务更新Agent
        AgentEntity agentEntity = agentServiceDomainService.updateAgent(updateEntity);
        return AgentAssembler.toDTO(agentEntity);
    }

    /**
     * 切换Agent的启用/禁用状态
     */
    public AgentDTO toggleAgentStatus(String agentId) {
        AgentEntity agentEntity = agentServiceDomainService.toggleAgentStatus(agentId);
        return AgentAssembler.toDTO(agentEntity);
    }

    /**
     * 删除Agent
     */
    public void deleteAgent(String agentId, String userId) {
        agentServiceDomainService.deleteAgent(agentId, userId);
    }

    /**
     * 发布Agent版本
     */
    public AgentVersionDTO publishAgentVersion(String agentId, PublishAgentVersionRequest request, String userId) {
        // 在应用层验证请求
        request.validate();

        // 获取当前Agent
        AgentEntity agent = agentServiceDomainService.getAgent(agentId, userId);

        // 获取最新版本，检查版本号大小
        AgentVersionEntity agentVersionEntity = agentServiceDomainService.getLatestAgentVersion(agentId);
        if (agentVersionEntity != null) {
            // 检查版本号是否大于上一个版本
            if (!request.isVersionGreaterThan(agentVersionEntity.getVersionNumber())) {
                throw new ParamValidationException("versionNumber",
                        "新版本号(" + request.getVersionNumber() +
                                ")必须大于当前最新版本号(" + agentVersionEntity.getVersionNumber() + ")");
            }
        }

        // 使用组装器创建版本实体
        AgentVersionEntity versionEntity = AgentVersionAssembler.createVersionEntity(agent, request);

        versionEntity.setUserId(userId);
        // 调用领域服务发布版本
        agentVersionEntity = agentServiceDomainService.publishAgentVersion(agentId, versionEntity);
        return AgentVersionAssembler.toDTO(agentVersionEntity);
    }

    /**
     * 获取Agent的所有版本
     */
    public List<AgentVersionDTO> getAgentVersions(String agentId, String userId) {
        List<AgentVersionEntity> agentVersions = agentServiceDomainService.getAgentVersions(agentId, userId);
        return AgentVersionAssembler.toDTOs(agentVersions);
    }

    /**
     * 获取Agent的特定版本
     */
    public AgentVersionDTO getAgentVersion(String agentId, String versionNumber) {
        AgentVersionEntity agentVersion = agentServiceDomainService.getAgentVersion(agentId, versionNumber);
        return AgentVersionAssembler.toDTO(agentVersion);
    }

    /**
     * 获取Agent的最新版本
     */
    public AgentVersionDTO getLatestAgentVersion(String agentId) {
        AgentVersionEntity latestAgentVersion = agentServiceDomainService.getLatestAgentVersion(agentId);
        return AgentVersionAssembler.toDTO(latestAgentVersion);
    }

    /**
     * 审核Agent版本
     */
    public AgentVersionDTO reviewAgentVersion(String versionId, ReviewAgentVersionRequest request) {
        // 在应用层验证请求
        request.validate();

        AgentVersionEntity agentVersionEntity = null;
        // 根据状态执行相应操作
        if (PublishStatus.REJECTED.equals(request.getStatus())) {
            // 拒绝发布，需使用拒绝原因
            agentVersionEntity = agentServiceDomainService.rejectVersion(versionId, request.getRejectReason());
        } else {
            // 其他状态变更，直接更新状态
            agentVersionEntity = agentServiceDomainService.updateVersionPublishStatus(versionId, request.getStatus());
        }
        return AgentVersionAssembler.toDTO(agentVersionEntity);
    }

    /**
     * 根据发布状态获取版本列表
     * 
     * @param status 发布状态
     * @return 版本列表（每个助理只返回最新版本）
     */
    public List<AgentVersionDTO> getVersionsByStatus(PublishStatus status) {
        List<AgentVersionEntity> versionsByStatus = agentServiceDomainService.getVersionsByStatus(status);
        return AgentVersionAssembler.toDTOs(versionsByStatus);
    }
}