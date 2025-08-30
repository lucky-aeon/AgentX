package org.xhy.application.rag.service.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.application.conversation.service.message.Agent;
import org.xhy.application.rag.assembler.DocumentUnitAssembler;
import org.xhy.application.rag.assembler.RagQaDatasetAssembler;
import org.xhy.application.rag.dto.*;
import org.xhy.application.rag.service.manager.RagQaDatasetAppService;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.llm.model.HighAvailabilityResult;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.rag.service.DocumentUnitDomainService;
import org.xhy.domain.rag.service.EmbeddingDomainService;
import org.xhy.domain.rag.service.FileDetailDomainService;
import org.xhy.domain.rag.service.RagQaDatasetDomainService;
import org.xhy.domain.rag.service.management.RagDataAccessDomainService;
import org.xhy.domain.rag.service.management.RagVersionDomainService;
import org.xhy.domain.rag.service.management.UserRagDomainService;
import org.xhy.domain.rag.service.management.UserRagFileDomainService;
import org.xhy.domain.user.service.UserSettingsDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.rag.factory.EmbeddingModelFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import org.xhy.domain.rag.model.UserRagFileEntity;
import org.xhy.domain.rag.model.ModelConfig;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
import org.xhy.domain.rag.model.RagVersionEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;
import org.xhy.domain.rag.repository.UserRagFileRepository;
import org.xhy.domain.rag.dto.HybridSearchConfig;
import org.xhy.domain.rag.service.*;
import org.xhy.infrastructure.rag.service.UserModelConfigResolver;

import java.util.concurrent.CompletableFuture;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class RAGSearchAppService {

    private static final Logger log = LoggerFactory.getLogger(RagQaDatasetAppService.class);

    private final RagQaDatasetDomainService ragQaDatasetDomainService;
    private final FileDetailDomainService fileDetailDomainService;
    private final DocumentUnitDomainService documentUnitDomainService;
    private final EmbeddingDomainService embeddingDomainService;
    private final ObjectMapper objectMapper;
    private final LLMServiceFactory llmServiceFactory;
    private final LLMDomainService llmDomainService;
    private final UserSettingsDomainService userSettingsDomainService;
    private final HighAvailabilityDomainService highAvailabilityDomainService;

    private final UserRagDomainService userRagDomainService;
    private final RagDataAccessDomainService ragDataAccessService;
    private final EmbeddingModelFactory embeddingModelFactory;
    private final HybridSearchDomainService hybridSearchDomainService;
    private final UserModelConfigResolver userModelConfigResolver;
    private final UserRagFileDomainService userRagFileDomainService;

    public RAGSearchAppService(RagQaDatasetDomainService ragQaDatasetDomainService,
            FileDetailDomainService fileDetailDomainService, DocumentUnitDomainService documentUnitDomainService,
            EmbeddingDomainService embeddingDomainService, ObjectMapper objectMapper,
            LLMServiceFactory llmServiceFactory, LLMDomainService llmDomainService,
            UserSettingsDomainService userSettingsDomainService,
            HighAvailabilityDomainService highAvailabilityDomainService, UserRagDomainService userRagDomainService,
            RagDataAccessDomainService ragDataAccessService, EmbeddingModelFactory embeddingModelFactory,
            HybridSearchDomainService hybridSearchDomainService,
            org.xhy.infrastructure.rag.service.UserModelConfigResolver userModelConfigResolver,
            UserRagFileDomainService userRagFileDomainService) {
        this.ragQaDatasetDomainService = ragQaDatasetDomainService;
        this.fileDetailDomainService = fileDetailDomainService;
        this.documentUnitDomainService = documentUnitDomainService;
        this.embeddingDomainService = embeddingDomainService;
        this.objectMapper = objectMapper;
        this.llmServiceFactory = llmServiceFactory;
        this.llmDomainService = llmDomainService;
        this.userSettingsDomainService = userSettingsDomainService;
        this.highAvailabilityDomainService = highAvailabilityDomainService;
        this.userRagDomainService = userRagDomainService;
        this.ragDataAccessService = ragDataAccessService;
        this.embeddingModelFactory = embeddingModelFactory;
        this.hybridSearchDomainService = hybridSearchDomainService;
        this.userModelConfigResolver = userModelConfigResolver;
        this.userRagFileDomainService = userRagFileDomainService;
    }

    /** RAG搜索文档（使用智能参数优化）
     * @param request 搜索请求
     * @param userId 用户ID
     * @return 搜索结果 */
    public List<DocumentUnitDTO> ragSearch(RagSearchRequest request, String userId) {
        // 验证数据集权限 - 检查用户是否安装了这些知识库
        List<String> validDatasetIds = new ArrayList<>();
        for (String datasetId : request.getDatasetIds()) {
            // 检查用户是否安装了这个知识库
            if (userRagDomainService.isRagInstalledByOriginalId(userId, datasetId)) {
                validDatasetIds.add(datasetId);
                log.debug("用户 {} 已安装知识库 {}，允许搜索", userId, datasetId);
            } else {
                // 检查用户是否是创建者（向后兼容）
                try {
                    ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);
                    validDatasetIds.add(datasetId);
                    log.debug("用户 {} 是知识库 {} 的创建者，允许搜索", userId, datasetId);
                } catch (Exception e) {
                    log.warn("用户 {} 既没有安装知识库 {} 也不是创建者，跳过搜索", userId, datasetId);
                }
            }
        }

        if (validDatasetIds.isEmpty()) {
            log.warn("用户 {} 没有任何有效的知识库可搜索", userId);
            return new ArrayList<>();
        }

        // 使用智能调整后的参数进行混合检索
        Double adjustedMinScore = request.getAdjustedMinScore();
        Integer adjustedCandidateMultiplier = request.getAdjustedCandidateMultiplier();

        // 获取用户的嵌入模型配置
        ModelConfig embeddingModelConfig = userModelConfigResolver.getUserEmbeddingModelConfig(userId);
        EmbeddingModelFactory.EmbeddingConfig embeddingConfig = toEmbeddingConfig(embeddingModelConfig);

        // 获取用户的聊天模型配置用于HyDE
        ModelConfig chatModelConfig = null;
        try {
            // 使用UserModelConfigResolver获取聊天模型配置
            chatModelConfig = userModelConfigResolver.getUserChatModelConfig(userId);
            log.debug("获取用户 {} 的聊天模型配置成功，modelId: {}", userId, chatModelConfig.getModelEndpoint());
        } catch (Exception e) {
            log.warn("获取用户 {} 的聊天模型配置失败，HyDE功能将不可用: {}", userId, e.getMessage());
        }

        // 使用HybridSearchConfig配置对象调用混合检索服务
        HybridSearchConfig config = HybridSearchConfig.builder(validDatasetIds, request.getQuestion())
                .maxResults(request.getMaxResults()).minScore(adjustedMinScore) // 使用智能调整的相似度阈值
                .enableRerank(request.getEnableRerank()).candidateMultiplier(adjustedCandidateMultiplier) // 使用智能调整的候选结果倍数
                .embeddingConfig(embeddingConfig) // 传入嵌入模型配置
                .enableQueryExpansion(request.getEnableQueryExpansion()) // 传递查询扩展参数
                .chatModelConfig(chatModelConfig) // 传入聊天模型配置（用于HyDE）
                .build();
        List<DocumentUnitEntity> entities = hybridSearchDomainService.hybridSearch(config);

        // 转换为DTO并返回
        return DocumentUnitAssembler.toDTOs(entities);
    }

    /** 基于已安装知识库的RAG搜索
     *
     * @param request RAG搜索请求（使用userRagId作为数据源）
     * @param userRagId 用户已安装的RAG ID
     * @param userId 用户ID
     * @return 搜索结果 */
    public List<DocumentUnitDTO> ragSearchByUserRag(RagSearchRequest request, String userRagId, String userId) {
        // 获取RAG数据源信息
        RagDataAccessDomainService.RagDataSourceInfo sourceInfo = ragDataAccessService.getRagDataSourceInfo(userId,
                userRagId);

        // 根据安装类型获取实际的数据集ID
        String actualDatasetId;
        if (sourceInfo.getIsRealTime()) {
            // REFERENCE类型：使用原始数据集ID
            actualDatasetId = sourceInfo.getOriginalRagId();
        } else {
            // SNAPSHOT类型：使用原始数据集ID（但实际搜索会通过版本控制过滤）
            actualDatasetId = sourceInfo.getOriginalRagId();
        }

        // 验证数据集权限 - 通过userRagId已经验证了权限，不需要再检查用户是否是创建者
        // 只需要确认原始数据集仍然存在
        var originalDataset = ragQaDatasetDomainService.findDatasetById(actualDatasetId);
        if (originalDataset == null) {
            throw new BusinessException("原始数据集不存在或已被删除");
        }

        // 使用智能调整后的参数进行RAG搜索
        Double adjustedMinScore = request.getAdjustedMinScore();
        Integer adjustedCandidateMultiplier = request.getAdjustedCandidateMultiplier();

        // 获取用户的嵌入模型配置
        ModelConfig embeddingModelConfig = userModelConfigResolver.getUserEmbeddingModelConfig(userId);
        EmbeddingModelFactory.EmbeddingConfig embeddingConfig = toEmbeddingConfig(embeddingModelConfig);

        // 获取用户的聊天模型配置用于HyDE
        ModelConfig chatModelConfig = null;
        try {
            // 使用UserModelConfigResolver获取聊天模型配置
            chatModelConfig = userModelConfigResolver.getUserChatModelConfig(userId);
            log.debug("ragSearchByUserRag - 获取用户 {} 的聊天模型配置成功，modelId: {}", userId, chatModelConfig.getModelEndpoint());
        } catch (Exception e) {
            log.warn("ragSearchByUserRag - 获取用户 {} 的聊天模型配置失败，HyDE功能将不可用: {}", userId, e.getMessage());
        }

        List<DocumentUnitEntity> entities;
        if (sourceInfo.getIsRealTime()) {
            // REFERENCE类型：使用混合检索搜索实时数据
            HybridSearchConfig config = HybridSearchConfig.builder(List.of(actualDatasetId), request.getQuestion())
                    .maxResults(request.getMaxResults()).minScore(adjustedMinScore)
                    .enableRerank(request.getEnableRerank()).candidateMultiplier(adjustedCandidateMultiplier)
                    .embeddingConfig(embeddingConfig).enableQueryExpansion(request.getEnableQueryExpansion())
                    .chatModelConfig(chatModelConfig) // 传入聊天模型配置（用于HyDE）
                    .build();
            entities = hybridSearchDomainService.hybridSearch(config);
        } else {
            // 对快照数据进行混合检索（这里可能需要特殊处理，暂时使用相同逻辑）
            HybridSearchConfig config = HybridSearchConfig.builder(List.of(actualDatasetId), request.getQuestion())
                    .maxResults(request.getMaxResults()).minScore(adjustedMinScore)
                    .enableRerank(request.getEnableRerank()).candidateMultiplier(adjustedCandidateMultiplier)
                    .embeddingConfig(embeddingConfig).enableQueryExpansion(request.getEnableQueryExpansion())
                    .chatModelConfig(chatModelConfig) // 传入聊天模型配置（用于HyDE）
                    .build();
            entities = hybridSearchDomainService.hybridSearch(config);
        }

        // 转换为DTO并返回
        return DocumentUnitAssembler.toDTOs(entities);
    }

    /** 将ModelConfig转换为EmbeddingModelFactory.EmbeddingConfig
     *
     * @param modelConfig RAG模型配置
     * @return 嵌入模型工厂配置 */
    private EmbeddingModelFactory.EmbeddingConfig toEmbeddingConfig(ModelConfig modelConfig) {
        return new EmbeddingModelFactory.EmbeddingConfig(modelConfig.getApiKey(), modelConfig.getBaseUrl(),
                modelConfig.getModelEndpoint());
    }
}
