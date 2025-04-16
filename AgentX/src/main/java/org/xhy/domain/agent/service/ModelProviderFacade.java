package org.xhy.domain.agent.service;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.springframework.stereotype.Service;
import org.xhy.domain.agent.model.AgentWorkspaceEntity;
import org.xhy.domain.agent.model.LLMModelConfig;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;

/**
 * 模型提供商门面
 * 负责获取和验证模型及提供商
 */
@Service
public class ModelProviderFacade {

    private final AgentWorkspaceDomainService agentWorkspaceDomainService;
    private final LLMDomainService llmDomainService;

    public ModelProviderFacade(
            AgentWorkspaceDomainService agentWorkspaceDomainService,
            LLMDomainService llmDomainService) {
        this.agentWorkspaceDomainService = agentWorkspaceDomainService;
        this.llmDomainService = llmDomainService;
    }

    /**
     * 获取并验证模型和提供商
     *
     * @param agentId 代理ID
     * @param userId  用户ID
     * @return 模型提供商结果
     */
    public ModelProviderResult getModelAndProvider(String agentId, String userId) {
        // 从工作区中获取对应的模型信息
        AgentWorkspaceEntity workspace = agentWorkspaceDomainService.getWorkspace(agentId, userId);
        LLMModelConfig llmModelConfig = workspace.getLlmModelConfig();
        String modelId = llmModelConfig.getModelId();
        ModelEntity model = llmDomainService.getModelById(modelId);

        // 验证模型是否激活
        model.isActive();

        // 获取并验证服务商信息
        ProviderEntity provider = llmDomainService.getProvider(model.getProviderId(), userId);
        provider.isActive();

        // 构建提供商配置
        org.xhy.domain.llm.model.config.ProviderConfig domainConfig = provider.getConfig();
        ProviderConfig providerConfig = new ProviderConfig(
                domainConfig.getApiKey(),
                domainConfig.getBaseUrl(),
                model.getModelId(),
                provider.getProtocol());

        // 获取流式聊天客户端
        StreamingChatLanguageModel chatStreamClient = LLMProviderService.getStream(
                provider.getProtocol(),
                providerConfig);

        return new ModelProviderResult(model, provider, llmModelConfig, providerConfig, chatStreamClient);
    }

    /**
     * 模型提供商结果
     */
    public static class ModelProviderResult {
        private final ModelEntity modelEntity;
        private final ProviderEntity providerEntity;
        private final LLMModelConfig llmModelConfig;
        private final ProviderConfig providerConfig;
        private final StreamingChatLanguageModel chatStreamClient;

        public ModelProviderResult(
                ModelEntity modelEntity,
                ProviderEntity providerEntity,
                LLMModelConfig llmModelConfig,
                ProviderConfig providerConfig,
                StreamingChatLanguageModel chatStreamClient) {
            this.modelEntity = modelEntity;
            this.providerEntity = providerEntity;
            this.llmModelConfig = llmModelConfig;
            this.providerConfig = providerConfig;
            this.chatStreamClient = chatStreamClient;
        }

        public ModelEntity getModelEntity() {
            return modelEntity;
        }

        public ProviderEntity getProviderEntity() {
            return providerEntity;
        }

        public LLMModelConfig getLlmModelConfig() {
            return llmModelConfig;
        }

        public ProviderConfig getProviderConfig() {
            return providerConfig;
        }

        public StreamingChatLanguageModel getChatStreamClient() {
            return chatStreamClient;
        }
    }
} 