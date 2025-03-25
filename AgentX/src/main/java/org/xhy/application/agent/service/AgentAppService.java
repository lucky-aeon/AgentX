package org.xhy.application.agent.service;

import org.springframework.stereotype.Service;
import org.xhy.application.agent.assembler.AgentAssembler;
import org.xhy.application.agent.assembler.AgentVersionAssembler;
import org.xhy.domain.agent.dto.AgentDTO;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.dto.AgentVersionDTO;
import org.xhy.domain.agent.model.AgentVersionEntity;
import org.xhy.domain.agent.service.AgentDomainService;
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

    public AgentAppService(AgentDomainService agentServiceDomainService) {
        this.agentServiceDomainService = agentServiceDomainService;
    }

    /**
     * 创建新Agent
     */
    public AgentDTO createAgent(CreateAgentRequest request, String userId) {
        // 在应用层验证请求
        request.validate();

        // 使用组装器创建领域实体
        AgentEntity entity = AgentAssembler.toEntity(request,userId);

        entity.setUserId(userId);

        // 调用领域服务
        return agentServiceDomainService.createAgent(entity);
    }

    /**
     * 获取Agent信息
     */
    public AgentDTO getAgent(String agentId, String userId) {
        return agentServiceDomainService.getAgent(agentId, userId);
    }

    /**
     * 获取用户的Agent列表，支持状态和名称过滤
     */
    public List<AgentDTO> getUserAgents(String userId, SearchAgentsRequest searchAgentsRequest) {
        return agentServiceDomainService.getUserAgents(userId, searchAgentsRequest);
    }

    /**
     * 获取已上架的Agent列表，支持名称搜索
     */
    public List<AgentVersionDTO> getPublishedAgentsByName(SearchAgentsRequest searchAgentsRequest) {
        return agentServiceDomainService.getPublishedAgentsByName(searchAgentsRequest);
    }

    /**
     * 获取待审核的Agent列表
     */
    public List<AgentDTO> getPendingReviewAgents() {
        return agentServiceDomainService.getPendingReviewAgents();
    }

    /**
     * 更新Agent信息（基本信息和配置合并更新）
     */
    public AgentDTO updateAgent(String agentId, UpdateAgentRequest request, String userId) {
        // 在应用层验证请求
        request.validate();

        // 使用组装器创建更新实体
        AgentEntity updateEntity = AgentAssembler.toEntity(request,userId);

        updateEntity.setUserId(userId);
        // 调用领域服务更新Agent
        return agentServiceDomainService.updateAgent(agentId, updateEntity);
    }

    /**
     * 切换Agent的启用/禁用状态
     */
    public AgentDTO toggleAgentStatus(String agentId) {
        return agentServiceDomainService.toggleAgentStatus(agentId);
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
        AgentDTO currentAgentDTO = agentServiceDomainService.getAgent(agentId,userId);

        // 获取最新版本，检查版本号大小
        AgentVersionDTO latestVersion = agentServiceDomainService.getLatestAgentVersion(agentId);
        if (latestVersion != null) {
            // 检查版本号是否大于上一个版本
            if (!request.isVersionGreaterThan(latestVersion.getVersionNumber())) {
                throw new ParamValidationException("versionNumber",
                        "新版本号(" + request.getVersionNumber() +
                                ")必须大于当前最新版本号(" + latestVersion.getVersionNumber() + ")");
            }
        }

        // 使用组装器创建版本实体
        AgentVersionEntity versionEntity = AgentVersionAssembler.createVersionEntity(currentAgentDTO.toEntity(), request);

        versionEntity.setUserId(userId);
        // 调用领域服务发布版本
        return agentServiceDomainService.publishAgentVersion(agentId, versionEntity);
    }

    /**
     * 获取Agent的所有版本
     */
    public List<AgentVersionDTO> getAgentVersions(String agentId, String userId) {
        return agentServiceDomainService.getAgentVersions(agentId, userId);
    }

    /**
     * 获取Agent的特定版本
     */
    public AgentVersionDTO getAgentVersion(String agentId, String versionNumber) {
        return agentServiceDomainService.getAgentVersion(agentId, versionNumber);
    }

    /**
     * 获取Agent的最新版本
     */
    public AgentVersionDTO getLatestAgentVersion(String agentId) {
        return agentServiceDomainService.getLatestAgentVersion(agentId);
    }

    /**
     * 审核Agent版本
     */
    public AgentVersionDTO reviewAgentVersion(String versionId, ReviewAgentVersionRequest request) {
        // 在应用层验证请求
        request.validate();

        // 根据状态执行相应操作
        if (PublishStatus.REJECTED.equals(request.getStatus())) {
            // 拒绝发布，需使用拒绝原因
            return agentServiceDomainService.rejectVersion(versionId, request.getRejectReason());
        } else {
            // 其他状态变更，直接更新状态
            return agentServiceDomainService.updateVersionPublishStatus(versionId, request.getStatus());
        }
    }

    /**
     * 根据发布状态获取版本列表
     * 
     * @param status 发布状态
     * @return 版本列表（每个助理只返回最新版本）
     */
    public List<AgentVersionDTO> getVersionsByStatus(PublishStatus status) {
        return agentServiceDomainService.getVersionsByStatus(status);
    }
}