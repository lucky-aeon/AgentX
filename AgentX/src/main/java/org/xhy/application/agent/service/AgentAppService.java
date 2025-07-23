package org.xhy.application.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.agent.assembler.AgentAssembler;
import org.xhy.application.agent.assembler.AgentVersionAssembler;
import org.xhy.application.agent.dto.AgentDTO;
import org.xhy.application.agent.dto.AgentWithUserDTO;
import org.xhy.application.agent.dto.AgentStatisticsDTO;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.application.agent.dto.AgentVersionDTO;
import org.xhy.domain.agent.model.AgentVersionEntity;
import org.xhy.domain.agent.model.AgentWorkspaceEntity;
import org.xhy.domain.agent.model.LLMModelConfig;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.domain.agent.service.AgentWorkspaceDomainService;
import org.xhy.infrastructure.exception.ParamValidationException;
import org.xhy.domain.agent.constant.PublishStatus;
import org.xhy.interfaces.dto.agent.request.*;
import org.xhy.domain.scheduledtask.service.ScheduledTaskExecutionService;
import org.xhy.domain.tool.service.UserToolDomainService;
import org.xhy.domain.rag.service.UserRagDomainService;
import org.xhy.domain.rag.service.RagVersionDomainService;
import org.xhy.domain.rag.constant.RagPublishStatus;
import org.xhy.domain.rag.model.UserRagEntity;
import org.xhy.domain.rag.model.RagVersionEntity;
import org.xhy.domain.tool.model.UserToolEntity;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Agent应用服务，用于适配领域层的Agent服务 职责： 1. 接收和验证来自接口层的请求 2. 将请求转换为领域对象或参数 3. 调用领域服务执行业务逻辑 4. 转换和返回结果给接口层 */
@Service
public class AgentAppService {

    private final AgentDomainService agentServiceDomainService;
    private final AgentWorkspaceDomainService agentWorkspaceDomainService;
    private final ScheduledTaskExecutionService scheduledTaskExecutionService;
    private final UserToolDomainService userToolDomainService;
    private final UserRagDomainService userRagDomainService;
    private final RagVersionDomainService ragVersionDomainService;

    public AgentAppService(AgentDomainService agentServiceDomainService,
            AgentWorkspaceDomainService agentWorkspaceDomainService,
            ScheduledTaskExecutionService scheduledTaskExecutionService, UserToolDomainService userToolDomainService,
            UserRagDomainService userRagDomainService, RagVersionDomainService ragVersionDomainService) {
        this.agentServiceDomainService = agentServiceDomainService;
        this.agentWorkspaceDomainService = agentWorkspaceDomainService;
        this.scheduledTaskExecutionService = scheduledTaskExecutionService;
        this.userToolDomainService = userToolDomainService;
        this.userRagDomainService = userRagDomainService;
        this.ragVersionDomainService = ragVersionDomainService;
    }

    /** 创建新Agent */
    @Transactional
    public AgentDTO createAgent(CreateAgentRequest request, String userId) {
        // 使用组装器创建领域实体
        AgentEntity entity = AgentAssembler.toEntity(request, userId);
        entity.setUserId(userId);
        AgentEntity agent = agentServiceDomainService.createAgent(entity);
        AgentWorkspaceEntity agentWorkspaceEntity = new AgentWorkspaceEntity(agent.getId(), userId,
                new LLMModelConfig());
        agentWorkspaceDomainService.save(agentWorkspaceEntity);
        return AgentAssembler.toDTO(agent);
    }

    /** 获取Agent信息 */
    public AgentDTO getAgent(String agentId, String userId) {
        // todo xhy 判断用户是否存在
        AgentEntity agent = agentServiceDomainService.getAgent(agentId, userId);
        return AgentAssembler.toDTO(agent);
    }

    /** 获取用户的Agent列表，支持状态和名称过滤 */
    public List<AgentDTO> getUserAgents(String userId, SearchAgentsRequest searchAgentsRequest) {
        AgentEntity entity = AgentAssembler.toEntity(searchAgentsRequest);
        List<AgentEntity> agents = agentServiceDomainService.getUserAgents(userId, entity);
        return AgentAssembler.toDTOs(agents);
    }
    /** 获取已上架的Agent列表，支持名称搜索 */

    public List<AgentVersionDTO> getPublishedAgentsByName(SearchAgentsRequest searchAgentsRequest, String userId) {
        AgentEntity entity = AgentAssembler.toEntity(searchAgentsRequest);
        List<AgentVersionEntity> agentVersionEntities = agentServiceDomainService.getPublishedAgentsByName(entity);
        if (agentVersionEntities.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> agentIds = agentVersionEntities.stream().map(AgentVersionEntity::getAgentId).toList();
        List<AgentWorkspaceEntity> agentWorkspaceEntities = agentWorkspaceDomainService.listAgents(agentIds, userId);
        Set<String> agentIdsSet = agentWorkspaceEntities.stream().map(AgentWorkspaceEntity::getAgentId)
                .collect(Collectors.toSet());

        List<AgentVersionDTO> agentVersionDTOS = AgentVersionAssembler.toDTOs(agentVersionEntities);
        if (agentIdsSet.isEmpty()) {
            return agentVersionDTOS;
        }
        for (AgentVersionDTO agentVersionDTO : agentVersionDTOS) {
            agentVersionDTO.setAddWorkspace(agentIdsSet.contains(agentVersionDTO.getAgentId()));
        }
        return agentVersionDTOS;
    }

    /** 更新Agent信息（基本信息和配置合并更新） */
    public AgentDTO updateAgent(UpdateAgentRequest request, String userId) {

        // 使用组装器创建更新实体
        AgentEntity updateEntity = AgentAssembler.toEntity(request, userId);

        // 调用领域服务更新Agent
        AgentEntity agentEntity = agentServiceDomainService.updateAgent(updateEntity);
        return AgentAssembler.toDTO(agentEntity);
    }

    /** 切换Agent的启用/禁用状态 */
    public AgentDTO toggleAgentStatus(String agentId) {
        AgentEntity agentEntity = agentServiceDomainService.toggleAgentStatus(agentId);
        return AgentAssembler.toDTO(agentEntity);
    }

    /** 删除Agent */
    @Transactional
    public void deleteAgent(String agentId, String userId) {
        // 先删除Agent关联的定时任务（包括取消延迟队列中的任务）
        scheduledTaskExecutionService.deleteTasksByAgentId(agentId, userId);
        // 再删除Agent本身
        agentServiceDomainService.deleteAgent(agentId, userId);
    }

    /** 发布Agent版本 */
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
                throw new ParamValidationException("versionNumber", "新版本号(" + request.getVersionNumber()
                        + ")必须大于当前最新版本号(" + agentVersionEntity.getVersionNumber() + ")");
            }
        }

        // 使用组装器创建版本实体
        AgentVersionEntity versionEntity = AgentVersionAssembler.createVersionEntity(agent, request);

        versionEntity.setUserId(userId);

        // 验证Agent依赖的工具和知识库权限
        validateAgentDependencies(versionEntity, userId);

        // 调用领域服务发布版本
        agentVersionEntity = agentServiceDomainService.publishAgentVersion(agentId, versionEntity);
        return AgentVersionAssembler.toDTO(agentVersionEntity);
    }

    /** 获取Agent的所有版本 */
    public List<AgentVersionDTO> getAgentVersions(String agentId, String userId) {
        List<AgentVersionEntity> agentVersions = agentServiceDomainService.getAgentVersions(agentId, userId);
        return AgentVersionAssembler.toDTOs(agentVersions);
    }

    /** 获取Agent的特定版本 */
    public AgentVersionDTO getAgentVersion(String agentId, String versionNumber) {
        AgentVersionEntity agentVersion = agentServiceDomainService.getAgentVersion(agentId, versionNumber);
        return AgentVersionAssembler.toDTO(agentVersion);
    }

    /** 获取Agent的最新版本 */
    public AgentVersionDTO getLatestAgentVersion(String agentId) {
        AgentVersionEntity latestAgentVersion = agentServiceDomainService.getLatestAgentVersion(agentId);
        return AgentVersionAssembler.toDTO(latestAgentVersion);
    }

    /** 审核Agent版本 */
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

    /** 根据发布状态获取版本列表
     * 
     * @param status 发布状态
     * @return 版本列表（每个助理只返回最新版本） */
    public List<AgentVersionDTO> getVersionsByStatus(PublishStatus status) {
        List<AgentVersionEntity> versionsByStatus = agentServiceDomainService.getVersionsByStatus(status);
        return AgentVersionAssembler.toDTOs(versionsByStatus);
    }

    /** 分页查询Agent列表（管理员使用，包含用户信息）
     * 
     * @param queryAgentRequest 查询条件
     * @return Agent分页数据（包含用户信息） */
    public Page<AgentWithUserDTO> getAgents(QueryAgentRequest queryAgentRequest) {
        Page<AgentEntity> page = agentServiceDomainService.getAgents(queryAgentRequest);
        return agentServiceDomainService.getAgentsWithUserInfo(page);
    }

    /** 获取Agent统计信息
     * 
     * @return Agent统计数据 */
    public AgentStatisticsDTO getAgentStatistics() {
        return agentServiceDomainService.getAgentStatistics();
    }

    /** 验证Agent发布时依赖的工具和知识库权限
     * 
     * @param versionEntity Agent版本实体
     * @param userId 当前用户ID
     * @throws BusinessException 当权限验证失败时抛出异常 */
    private void validateAgentDependencies(AgentVersionEntity versionEntity, String userId) {
        // 验证工具权限
        if (versionEntity.getToolIds() != null && !versionEntity.getToolIds().isEmpty()) {
            for (String toolId : versionEntity.getToolIds()) {
                validateToolPermission(toolId, userId);
            }
        }

        // 验证知识库权限
        if (versionEntity.getKnowledgeBaseIds() != null && !versionEntity.getKnowledgeBaseIds().isEmpty()) {
            for (String knowledgeBaseId : versionEntity.getKnowledgeBaseIds()) {
                validateKnowledgeBasePermission(knowledgeBaseId, userId);
            }
        }
    }

    /** 验证工具权限
     * 
     * @param toolId 工具ID
     * @param userId 用户ID
     * @throws BusinessException 当用户未安装该工具或工具版本未公开时抛出异常 */
    private void validateToolPermission(String toolId, String userId) {
        UserToolEntity userTool = userToolDomainService.findByToolIdAndUserId(toolId, userId);
        if (userTool == null) {
            // 尝试获取工具名称用于友好提示
            String toolName = getToolDisplayName(toolId);
            throw new BusinessException("您尚未安装工具「" + toolName + "」，无法发布使用该工具的Agent");
        }

        // 检查用户安装的工具版本是否为公开版本（创建者可以使用私有版本）
        if (!userId.equals(userTool.getUserId()) && !Boolean.TRUE.equals(userTool.getPublicState())) {
            throw new BusinessException(
                    "工具「" + userTool.getName() + " v" + userTool.getVersion() + "」的版本未公开，无法发布使用该工具的Agent");
        }
    }

    /** 获取工具显示名称（用于错误提示）
     * 
     * @param toolId 工具ID
     * @return 工具显示名称 */
    private String getToolDisplayName(String toolId) {
        try {
            // 这里可以尝试从工具服务获取工具名称，但为了避免循环依赖，暂时返回简化ID
            return toolId.length() > 8 ? toolId.substring(0, 8) + "..." : toolId;
        } catch (Exception e) {
            return toolId.length() > 8 ? toolId.substring(0, 8) + "..." : toolId;
        }
    }

    /** 验证知识库权限
     * 
     * @param knowledgeBaseId 知识库ID
     * @param userId 用户ID
     * @throws BusinessException 当用户未安装该知识库或知识库版本未发布时抛出异常 */
    private void validateKnowledgeBasePermission(String knowledgeBaseId, String userId) {
        // 先尝试获取知识库信息用于友好的错误提示
        String knowledgeBaseName = getKnowledgeBaseDisplayName(knowledgeBaseId);

        // 检查用户是否安装了该知识库
        if (!userRagDomainService.isRagInstalledByOriginalId(userId, knowledgeBaseId)) {
            throw new BusinessException("您尚未安装知识库「" + knowledgeBaseName + "」，无法发布使用该知识库的Agent");
        }

        // 获取用户安装的知识库信息
        UserRagEntity userRag = userRagDomainService.findInstalledRagByOriginalId(userId, knowledgeBaseId);
        if (userRag == null) {
            throw new BusinessException("知识库「" + knowledgeBaseName + "」未找到安装记录");
        }

        // 获取对应的RAG版本信息来检查创建者和发布状态
        RagVersionEntity ragVersion = ragVersionDomainService.getRagVersion(userRag.getRagVersionId());

        // 如果不是自己创建的知识库，需要检查版本是否已发布
        if (!userId.equals(ragVersion.getUserId())) {
            // 对于非创建者，需要确保使用的是已发布的版本
            if (!RagPublishStatus.PUBLISHED.getCode().equals(ragVersion.getPublishStatus())) {
                throw new BusinessException(
                        "知识库「" + ragVersion.getName() + " v" + ragVersion.getVersion() + "」的版本未发布，无法发布使用该知识库的Agent");
            }
        }
        // 创建者可以使用自己的任何版本，包括未发布的版本
    }

    /** 获取知识库显示名称（用于错误提示）
     * 
     * @param knowledgeBaseId 知识库ID
     * @return 知识库显示名称 */
    private String getKnowledgeBaseDisplayName(String knowledgeBaseId) {
        try {
            // 尝试获取任意一个版本来获取知识库名称
            List<RagVersionEntity> versions = ragVersionDomainService.getVersionsByOriginalRagId(knowledgeBaseId,
                    "system");
            if (!versions.isEmpty()) {
                RagVersionEntity firstVersion = versions.get(0);
                return firstVersion.getName() + " v" + firstVersion.getVersion();
            }
        } catch (Exception e) {
            // 如果获取失败，返回ID的前8位作为友好显示
        }
        return knowledgeBaseId.length() > 8 ? knowledgeBaseId.substring(0, 8) + "..." : knowledgeBaseId;
    }
}