package org.xhy.application.agent.service;

import org.springframework.stereotype.Service;
import org.xhy.application.agent.assembler.AgentAssembler;
import org.xhy.domain.agent.model.AgentDTO;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.model.AgentStatus;
import org.xhy.domain.agent.model.AgentVersionDTO;
import org.xhy.domain.agent.model.AgentVersionEntity;
import org.xhy.domain.agent.service.AgentService;
import org.xhy.interfaces.dto.agent.*;
import org.xhy.domain.agent.model.PublishStatus;

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

    private final AgentService agentService;

    public AgentAppService(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 创建新Agent
     */
    public AgentDTO createAgent(CreateAgentRequest request) {
        // 在应用层验证请求
        request.validate();

        // 使用组装器创建领域实体
        AgentEntity entity = AgentAssembler.toEntity(request);

        // 调用领域服务
        return agentService.createAgent(entity);
    }

    /**
     * 获取Agent信息
     */
    public AgentDTO getAgent(String agentId) {
        return agentService.getAgent(agentId);
    }

    /**
     * 获取用户的Agent列表，支持状态和名称过滤
     */
    public List<AgentDTO> getUserAgents(String userId, SearchAgentsRequest searchAgentsRequest) {
        return agentService.getUserAgents(userId, searchAgentsRequest);
    }

    /**
     * 获取已上架的Agent列表，支持名称搜索
     */
    public List<AgentVersionDTO> getPublishedAgentsByName(SearchAgentsRequest searchAgentsRequest) {
        return agentService.getPublishedAgentsByName(searchAgentsRequest);
    }


    /**
     * 获取待审核的Agent列表
     */
    public List<AgentDTO> getPendingReviewAgents() {
        return agentService.getPendingReviewAgents();
    }

    /**
     * 更新Agent信息（基本信息和配置合并更新）
     */
    public AgentDTO updateAgent(String agentId, UpdateAgentRequest request) {
        // 在应用层验证请求
        request.validate();

        // 使用组装器创建更新实体
        AgentEntity updateEntity = AgentAssembler.toEntity(request);

        // 调用领域服务更新Agent
        return agentService.updateAgent(agentId, updateEntity);
    }

    /**
     * 切换Agent的启用/禁用状态
     */
    public AgentDTO toggleAgentStatus(String agentId) {
        return agentService.toggleAgentStatus(agentId);
    }

    /**
     * 删除Agent
     */
    public void deleteAgent(String agentId) {
        agentService.deleteAgent(agentId);
    }

    /**
     * 发布Agent版本
     */
    public AgentVersionDTO publishAgentVersion(String agentId, PublishAgentVersionRequest request) {
        // 在应用层验证请求
        request.validate();

        // 获取当前Agent
        AgentDTO currentAgentDTO = agentService.getAgent(agentId);

        // 获取最新版本，检查版本号大小
        AgentVersionDTO latestVersion = agentService.getLatestAgentVersion(agentId);
        if (latestVersion != null) {
            // 检查版本号是否大于上一个版本
            if (!request.isVersionGreaterThan(latestVersion.getVersionNumber())) {
                throw new IllegalArgumentException("新版本号(" + request.getVersionNumber() +
                        ")必须大于当前最新版本号(" + latestVersion.getVersionNumber() + ")");
            }
        }

        // 使用组装器创建版本实体
        AgentVersionEntity versionEntity = AgentAssembler.createVersionEntity(currentAgentDTO.toEntity(), request);

        // 调用领域服务发布版本
        return agentService.publishAgentVersion(agentId, versionEntity);
    }

    /**
     * 获取Agent的所有版本
     */
    public List<AgentVersionDTO> getAgentVersions(String agentId) {
        return agentService.getAgentVersions(agentId);
    }

    /**
     * 获取Agent的特定版本
     */
    public AgentVersionDTO getAgentVersion(String agentId, String versionNumber) {
        return agentService.getAgentVersion(agentId, versionNumber);
    }

    /**
     * 获取Agent的最新版本
     */
    public AgentVersionDTO getLatestAgentVersion(String agentId) {
        return agentService.getLatestAgentVersion(agentId);
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
            return agentService.rejectVersion(versionId, request.getRejectReason());
        } else {
            // 其他状态变更，直接更新状态
            return agentService.updateVersionPublishStatus(versionId, request.getStatus());
        }
    }

    /**
     * 根据发布状态获取版本列表
     * 
     * @param status 发布状态
     * @return 版本列表（每个助理只返回最新版本）
     */
    public List<AgentVersionDTO> getVersionsByStatus(PublishStatus status) {
        return agentService.getVersionsByStatus(status);
    }
}