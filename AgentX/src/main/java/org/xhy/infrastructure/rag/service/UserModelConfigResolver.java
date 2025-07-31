package org.xhy.infrastructure.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.user.service.UserSettingsAppService;
import org.xhy.application.user.dto.UserSettingsDTO;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.rag.model.ModelConfig;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.Objects;

/** 用户模型配置解析器 - Infrastructure层服务
 * 
 * 解决Domain层需要获取用户模型配置的问题
 * 
 * @author shilong.zang */
@Service
public class UserModelConfigResolver {

    private static final Logger log = LoggerFactory.getLogger(UserModelConfigResolver.class);

    private final UserSettingsAppService userSettingsAppService;

    private final LLMDomainService llmDomainService;

    public UserModelConfigResolver(UserSettingsAppService userSettingsAppService, LLMDomainService llmDomainService) {
        this.userSettingsAppService = userSettingsAppService;
        this.llmDomainService = llmDomainService;
    }

    /** 获取用户的嵌入模型配置
     * 
     * @param userId 用户ID
     * @return 嵌入模型配置
     * @throws BusinessException 如果用户未配置嵌入模型或配置无效 */
    public ModelConfig getUserEmbeddingModelConfig(String userId) {
        try {
            UserSettingsDTO userSettingsDTO = userSettingsAppService.getUserSettings(userId);

            // 检查用户是否配置了嵌入模型
            if (userSettingsDTO == null || userSettingsDTO.getSettingConfig() == null
                    || userSettingsDTO.getSettingConfig().getDefaultEmbeddingModel() == null) {
                String errorMsg = String.format("用户 %s 未配置默认嵌入模型，无法进行向量化处理", userId);
                log.error(errorMsg);
                throw new BusinessException(errorMsg);
            }

            String modelId = userSettingsDTO.getSettingConfig().getDefaultEmbeddingModel();
            log.info("Getting embedding model config for user {}, modelId: {}", userId, modelId);

            // 根据模型ID从数据库获取真实的模型配置
            return getModelConfigFromDatabase(modelId, userId, "EMBEDDING");

        } catch (BusinessException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("用户 %s 获取嵌入模型配置失败: %s", userId, e.getMessage());
            log.error(errorMsg, e);
            throw new BusinessException(errorMsg, e);
        }
    }

    /** 从数据库获取模型配置
     * 
     * @param modelId 模型ID
     * @param userId 用户ID
     * @param expectedType 期望的模型类型
     * @return 模型配置
     * @throws BusinessException 如果模型不存在或配置无效 */
    private ModelConfig getModelConfigFromDatabase(String modelId, String userId, String expectedType) {
        try {
            // 获取模型实体
            ModelEntity modelEntity = llmDomainService.findModelById(modelId);
            if (modelEntity == null) {
                String errorMsg = String.format("用户 %s 配置的模型 %s 不存在", userId, modelId);
                log.error(errorMsg);
                throw new BusinessException(errorMsg);
            }

            // 验证模型类型
            if (!expectedType.equals(modelEntity.getType().getCode())) {
                String errorMsg = String.format("用户 %s 配置的模型 %s 类型不匹配，期望: %s，实际: %s", userId, modelId, expectedType,
                        modelEntity.getType().getCode());
                log.error(errorMsg);
                throw new BusinessException(errorMsg);
            }

            // 检查模型是否激活
            if (!modelEntity.getStatus()) {
                String errorMsg = String.format("用户 %s 配置的模型 %s 已禁用", userId, modelId);
                log.error(errorMsg);
                throw new BusinessException(errorMsg);
            }

            // 获取服务商配置
            ProviderEntity providerEntity = llmDomainService.getProvider(modelEntity.getProviderId());

            // 检查服务商是否激活
            if (!providerEntity.getStatus()) {
                String errorMsg = String.format("用户 %s 的模型 %s 关联的服务商已禁用", userId, modelId);
                log.error(errorMsg);
                throw new BusinessException(errorMsg);
            }

            providerEntity.isAvailable(providerEntity.getUserId());

            // 构建模型配置
            ModelConfig modelConfig = new ModelConfig(modelEntity.getModelId(), providerEntity.getConfig().getApiKey(),
                    providerEntity.getConfig().getBaseUrl(), expectedType);

            log.info("Successfully retrieved model config for user {}: modelId={}, baseUrl={}", userId,
                    modelEntity.getModelId(), providerEntity.getConfig().getBaseUrl());

            return modelConfig;

        } catch (BusinessException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("用户 %s 获取模型 %s 配置时发生错误: %s", userId, modelId, e.getMessage());
            log.error(errorMsg, e);
            throw new BusinessException(errorMsg, e);
        }
    }
}