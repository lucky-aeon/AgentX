package org.xhy.application.apikey.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.apikey.assembler.ApiKeyAssembler;
import org.xhy.application.apikey.dto.ApiKeyDTO;
import org.xhy.application.apikey.dto.ApiKeyValidationResult;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.domain.apikey.model.ApiKeyEntity;
import org.xhy.domain.apikey.service.ApiKeyDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.dto.apikey.request.QueryApiKeyRequest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** API密钥应用服务 */
@Service
public class ApiKeyAppService {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAppService.class);

    private final ApiKeyDomainService apiKeyDomainService;
    private final AgentDomainService agentDomainService;

    public ApiKeyAppService(ApiKeyDomainService apiKeyDomainService, AgentDomainService agentDomainService) {
        this.apiKeyDomainService = apiKeyDomainService;
        this.agentDomainService = agentDomainService;
    }

    /** 创建API密钥
     *
     * @param agentId Agent ID
     * @param name API密钥名称
     * @param userId 用户ID
     * @return 创建的API密钥DTO */
    @Transactional
    public ApiKeyDTO createApiKey(String agentId, String name, String userId) {
        // 验证Agent是否存在且属于当前用户
        AgentEntity agent = agentDomainService.getAgent(agentId, userId);
        if (agent == null) {
            throw new BusinessException("Agent不存在或无权限访问");
        }

        // 创建API密钥实体
        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setAgentId(agentId);
        apiKeyEntity.setUserId(userId);
        apiKeyEntity.setName(name);

        // 调用领域服务创建
        ApiKeyEntity createdApiKey = apiKeyDomainService.createApiKey(apiKeyEntity);

        // 转换为DTO并设置Agent名称
        ApiKeyDTO dto = ApiKeyAssembler.toDTO(createdApiKey);
        dto.setAgentName(agent.getName());

        logger.info("用户 {} 为Agent {} 创建了API密钥: {}", userId, agentId, createdApiKey.getId());

        return dto;
    }

    /** 获取用户的API密钥列表
     *
     * @param userId 用户ID
     * @param queryRequest 查询条件
     * @return API密钥列表 */
    public List<ApiKeyDTO> getUserApiKeys(String userId, QueryApiKeyRequest queryRequest) {
        List<ApiKeyEntity> apiKeys = apiKeyDomainService.getUserApiKeys(userId, queryRequest);
        List<ApiKeyDTO> dtos = ApiKeyAssembler.toDTOs(apiKeys);

        // 批量获取Agent信息
        List<String> agentIds = apiKeys.stream().map(ApiKeyEntity::getAgentId).distinct().collect(Collectors.toList());

        if (!agentIds.isEmpty()) {
            Map<String, AgentEntity> agentMap = agentDomainService.getAgentsByIds(agentIds).stream()
                    .collect(Collectors.toMap(AgentEntity::getId, Function.identity()));

            // 设置Agent名称
            dtos.forEach(dto -> {
                AgentEntity agent = agentMap.get(dto.getAgentId());
                if (agent != null) {
                    dto.setAgentName(agent.getName());
                }
            });
        }

        return dtos;
    }

    /** 获取用户的API密钥列表（无查询条件，保持向后兼容）
     *
     * @param userId 用户ID
     * @return API密钥列表 */
    public List<ApiKeyDTO> getUserApiKeys(String userId) {
        return getUserApiKeys(userId, null);
    }

    /** 获取Agent的API密钥列表
     *
     * @param agentId Agent ID
     * @param userId 用户ID
     * @return API密钥列表 */
    public List<ApiKeyDTO> getAgentApiKeys(String agentId, String userId) {
        // 验证Agent权限
        AgentEntity agent = agentDomainService.getAgent(agentId, userId);
        if (agent == null) {
            throw new BusinessException("Agent不存在或无权限访问");
        }

        List<ApiKeyEntity> apiKeys = apiKeyDomainService.getAgentApiKeys(agentId, userId);
        List<ApiKeyDTO> dtos = ApiKeyAssembler.toDTOs(apiKeys);

        // 设置Agent名称
        dtos.forEach(dto -> dto.setAgentName(agent.getName()));

        return dtos;
    }

    /** 获取API密钥详情
     *
     * @param apiKeyId API密钥ID
     * @param userId 用户ID
     * @return API密钥DTO */
    public ApiKeyDTO getApiKey(String apiKeyId, String userId) {
        ApiKeyEntity apiKey = apiKeyDomainService.getApiKey(apiKeyId, userId);
        ApiKeyDTO dto = ApiKeyAssembler.toDTO(apiKey);

        // 设置Agent名称
        AgentEntity agent = agentDomainService.getAgent(apiKey.getAgentId(), userId);
        if (agent != null) {
            dto.setAgentName(agent.getName());
        }

        return dto;
    }

    /** 更新API密钥状态
     *
     * @param apiKeyId API密钥ID
     * @param status 状态
     * @param userId 用户ID */
    @Transactional
    public void updateApiKeyStatus(String apiKeyId, Boolean status, String userId) {
        apiKeyDomainService.updateStatus(apiKeyId, userId, status);
        logger.info("用户 {} 更新API密钥 {} 状态为: {}", userId, apiKeyId, status);
    }

    /** 删除API密钥
     *
     * @param apiKeyId API密钥ID
     * @param userId 用户ID */
    @Transactional
    public void deleteApiKey(String apiKeyId, String userId) {
        apiKeyDomainService.deleteApiKey(apiKeyId, userId);
        logger.info("用户 {} 删除了API密钥: {}", userId, apiKeyId);
    }

    /** 重置API密钥
     *
     * @param apiKeyId API密钥ID
     * @param userId 用户ID
     * @return 新的API密钥DTO */
    @Transactional
    public ApiKeyDTO resetApiKey(String apiKeyId, String userId) {
        ApiKeyEntity resetApiKey = apiKeyDomainService.resetApiKey(apiKeyId, userId);
        ApiKeyDTO dto = ApiKeyAssembler.toDTO(resetApiKey);

        // 设置Agent名称
        AgentEntity agent = agentDomainService.getAgent(resetApiKey.getAgentId(), userId);
        if (agent != null) {
            dto.setAgentName(agent.getName());
        }

        logger.info("用户 {} 重置了API密钥: {}", userId, apiKeyId);

        return dto;
    }

    /** 验证外部API Key
     *
     * @param apiKey API密钥
     * @return 验证结果 */
    public ApiKeyValidationResult validateExternalApiKey(String apiKey) {
        try {
            ApiKeyEntity apiKeyEntity = apiKeyDomainService.validateApiKey(apiKey);
            // 更新使用记录
            apiKeyDomainService.updateUsage(apiKey);

            return ApiKeyValidationResult.success(apiKeyEntity.getUserId(), apiKeyEntity.getAgentId());
        } catch (BusinessException e) {
            return ApiKeyValidationResult.failure(e.getMessage());
        }
    }
}