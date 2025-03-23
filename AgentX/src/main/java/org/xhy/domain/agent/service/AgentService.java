package org.xhy.domain.agent.service;

import org.xhy.domain.agent.model.AgentDTO;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.model.AgentStatus;
import org.xhy.domain.agent.model.AgentVersionDTO;
import org.xhy.domain.agent.model.AgentVersionEntity;
import org.xhy.domain.agent.model.PublishStatus;
import org.xhy.interfaces.dto.agent.*;

import java.util.List;

/**
 * Agent服务接口
 */
public interface AgentService {

    /**
     * 创建新Agent
     * 
     * @param entity Agent实体对象
     * @return 创建的Agent信息
     */
    AgentDTO createAgent(AgentEntity entity);

    /**
     * 获取单个Agent信息
     * 
     * @param agentId Agent ID，不能为空
     * @return Agent信息
     */
    AgentDTO getAgent(String agentId);

    /**
     * 获取用户的Agent列表，支持状态和名称过滤
     * 
     * @param userId 用户ID，不能为空
     * @param searchAgentsRequest 查询条件
     * @return 符合条件的Agent列表
     */
    List<AgentDTO> getUserAgents(String userId, SearchAgentsRequest searchAgentsRequest);


    /**
     * 获取已上架的Agent列表，支持名称搜索
     * 当name为空时返回所有已上架Agent
     */
    List<AgentVersionDTO> getPublishedAgentsByName(SearchAgentsRequest name);

    /**
     * 获取待审核的Agent列表
     * 
     * @return 待审核的Agent列表
     */
    List<AgentDTO> getPendingReviewAgents();

    /**
     * 更新Agent信息
     * 
     * @param agentId Agent ID，不能为空
     * @param entity  更新的Agent实体对象
     * @return 更新后的Agent信息
     */
    AgentDTO updateAgent(String agentId, AgentEntity entity);

    /**
     * 切换Agent的启用/禁用状态
     * 
     * @param agentId Agent ID，不能为空
     * @return 更新后的Agent信息
     */
    AgentDTO toggleAgentStatus(String agentId);

    /**
     * 删除Agent
     * 
     * @param agentId Agent ID，不能为空
     */
    void deleteAgent(String agentId);


    /**
     * 发布Agent版本
     * 
     * @param agentId       Agent ID，不能为空
     * @param versionEntity 版本实体对象
     * @return 发布的版本信息
     */
    AgentVersionDTO publishAgentVersion(String agentId, AgentVersionEntity versionEntity);

    /**
     * 更新版本发布状态
     * 
     * @param versionId 版本ID，不能为空
     * @param status    发布状态，不能为空
     * @return 更新后的版本信息
     */
    AgentVersionDTO updateVersionPublishStatus(String versionId, PublishStatus status);

    /**
     * 拒绝版本发布
     * 
     * @param versionId 版本ID，不能为空
     * @param reason    拒绝原因，不能为空
     * @return 更新后的版本信息
     */
    AgentVersionDTO rejectVersion(String versionId, String reason);

    /**
     * 获取Agent的所有版本
     * 
     * @param agentId Agent ID，不能为空
     * @return 版本列表
     */
    List<AgentVersionDTO> getAgentVersions(String agentId);

    /**
     * 获取Agent的特定版本
     * 
     * @param agentId       Agent ID，不能为空
     * @param versionNumber 版本号，不能为空
     * @return 版本信息
     */
    AgentVersionDTO getAgentVersion(String agentId, String versionNumber);

    /**
     * 获取Agent的最新版本
     * 
     * @param agentId Agent ID，不能为空
     * @return 最新版本信息
     */
    AgentVersionDTO getLatestAgentVersion(String agentId);

    /**
     * 获取指定状态的所有版本
     * 
     * @param status 版本状态，不能为空
     * @return 符合状态的版本列表
     */
    List<AgentVersionDTO> getVersionsByStatus(PublishStatus status);

}