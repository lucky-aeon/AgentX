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
import org.xhy.application.rag.service.manager.RagMarketAppService;
import org.xhy.application.conversation.dto.RagRetrievalDocumentDTO;
import org.xhy.application.rag.service.manager.RagModelConfigService;
import org.xhy.application.rag.service.manager.RagPublishAppService;
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
    private final RagModelConfigService ragModelConfigService;
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
            RagDataAccessDomainService ragDataAccessService, RagModelConfigService ragModelConfigService,
            EmbeddingModelFactory embeddingModelFactory, HybridSearchDomainService hybridSearchDomainService,
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
        this.ragModelConfigService = ragModelConfigService;
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
        ModelConfig embeddingModelConfig = ragModelConfigService.getUserEmbeddingModelConfig(userId);
        EmbeddingModelFactory.EmbeddingConfig embeddingConfig = toEmbeddingConfig(embeddingModelConfig);

        // 获取用户的聊天模型配置用于HyDE
        ModelConfig chatModelConfig = null;
        try {
            // 使用UserModelConfigResolver获取聊天模型配置
            chatModelConfig = userModelConfigResolver.getUserChatModelConfig(userId);
            log.debug("获取用户 {} 的聊天模型配置成功，modelId: {}", userId, chatModelConfig.getModelId());
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
        ModelConfig embeddingModelConfig = ragModelConfigService.getUserEmbeddingModelConfig(userId);
        EmbeddingModelFactory.EmbeddingConfig embeddingConfig = toEmbeddingConfig(embeddingModelConfig);

        // 获取用户的聊天模型配置用于HyDE
        ModelConfig chatModelConfig = null;
        try {
            // 使用UserModelConfigResolver获取聊天模型配置
            chatModelConfig = userModelConfigResolver.getUserChatModelConfig(userId);
            log.debug("ragSearchByUserRag - 获取用户 {} 的聊天模型配置成功，modelId: {}", userId, chatModelConfig.getModelId());
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
            // SNAPSHOT类型：搜索版本快照数据，使用混合检索
            List<DocumentUnitEntity> snapshotDocuments = ragDataAccessService.getRagDocuments(userId, userRagId);
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

    /** RAG流式问答
     * @param request 流式问答请求
     * @param userId 用户ID
     * @return SSE流式响应 */
    public SseEmitter ragStreamChat(RagStreamChatRequest request, String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        AtomicBoolean completed = new AtomicBoolean(false);

        // 设置连接关闭回调
        emitter.onCompletion(() -> {
            completed.set(true);
            log.info("用户 {} 的RAG流式对话完成", userId);
        });
        emitter.onTimeout(() -> {
            log.warn("用户 {} 的RAG流式对话超时", userId);
            sendSseData(emitter, createErrorResponse("连接超时"), completed);
            safeCompleteEmitter(emitter, completed);
        });
        emitter.onError((ex) -> {
            log.error("用户 {} 的RAG流式对话连接错误", userId, ex);
            safeCompleteEmitter(emitter, completed);
        });

        // 异步处理流式问答
        CompletableFuture.runAsync(() -> {
            try {
                processRagStreamChat(request, userId, emitter, completed);
            } catch (Exception e) {
                log.error("RAG流式对话错误", e);
                sendSseData(emitter, createErrorResponse("处理过程中发生错误: " + e.getMessage()), completed);
            } finally {
                // 确保连接被正确关闭
                safeCompleteEmitter(emitter, completed);
            }
        });

        return emitter;
    }

    /** 处理RAG流式问答的核心逻辑 */
    private void processRagStreamChat(RagStreamChatRequest request, String userId, SseEmitter emitter,
            AtomicBoolean completed) {
        try {
            // 第一阶段：检索文档
            log.info("开始RAG流式对话 用户: {}, 问题: '{}'", userId, request.getQuestion());

            // 发送检索开始信号
            sendSseData(emitter, AgentChatResponse.build("开始检索相关文档...", MessageType.RAG_RETRIEVAL_START), completed);
            Thread.sleep(500);

            // 确定检索范围
            List<String> searchDatasetIds = new ArrayList<>();
            List<String> searchFileIds = new ArrayList<>();

            if (request.getFileId() != null && !request.getFileId().trim().isEmpty()) {
                FileDetailEntity fileEntity = fileDetailDomainService.getFileById(request.getFileId(), userId);
                searchFileIds.add(request.getFileId());
                searchDatasetIds.add(fileEntity.getDataSetId());
                sendSseData(emitter, AgentChatResponse.build("正在指定文件中检索...", MessageType.RAG_RETRIEVAL_PROGRESS),
                        completed);
            } else if (request.getDatasetIds() != null && !request.getDatasetIds().isEmpty()) {
                for (String datasetId : request.getDatasetIds()) {
                    ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);
                }
                searchDatasetIds.addAll(request.getDatasetIds());
                sendSseData(emitter, AgentChatResponse.build("正在数据集中检索...", MessageType.RAG_RETRIEVAL_PROGRESS),
                        completed);
            } else {
                throw new IllegalArgumentException("必须指定文件ID或数据集ID");
            }

            // 获取用户的嵌入模型配置
            ModelConfig embeddingModelConfig = ragModelConfigService.getUserEmbeddingModelConfig(userId);
            EmbeddingModelFactory.EmbeddingConfig embeddingConfig = toEmbeddingConfig(embeddingModelConfig);

            // 执行RAG检索
            List<DocumentUnitEntity> retrievedDocuments;
            if (request.getFileId() != null && !request.getFileId().trim().isEmpty()) {
                retrievedDocuments = retrieveFromFile(request.getFileId(), request.getQuestion(),
                        request.getMaxResults(), embeddingConfig);
            } else {
                HybridSearchConfig config = HybridSearchConfig.builder(searchDatasetIds, request.getQuestion())
                        .maxResults(request.getMaxResults()).minScore(request.getMinScore())
                        .enableRerank(request.getEnableRerank()).candidateMultiplier(2).embeddingConfig(embeddingConfig)
                        .enableQueryExpansion(false) // 流式问答中暂时不启用查询扩展，保持现有行为
                        .build();
                retrievedDocuments = hybridSearchDomainService.hybridSearch(config);
            }

            // 构建检索结果
            List<RetrievedDocument> retrievedDocs = new ArrayList<>();
            for (DocumentUnitEntity doc : retrievedDocuments) {
                FileDetailEntity fileDetail = fileDetailDomainService.getFileById(doc.getFileId());
                double similarityScore = doc.getSimilarityScore() != null ? doc.getSimilarityScore() : 0.0;
                retrievedDocs.add(new RetrievedDocument(doc.getFileId(),
                        fileDetail != null ? fileDetail.getOriginalFilename() : "未知文件", doc.getId(), similarityScore));
            }

            // 发送检索完成信号
            String retrievalMessage = String.format("检索完成，找到 %d 个相关文档", retrievedDocs.size());
            AgentChatResponse retrievalEndResponse = AgentChatResponse.build(retrievalMessage,
                    MessageType.RAG_RETRIEVAL_END);
            try {
                retrievalEndResponse.setPayload(objectMapper.writeValueAsString(retrievedDocs));
            } catch (Exception e) {
                log.error("失败：序列化检索到的文档", e);
            }
            sendSseData(emitter, retrievalEndResponse, completed);
            Thread.sleep(1000);

            // 第二阶段：生成回答
            sendSseData(emitter, AgentChatResponse.build("开始生成回答...", MessageType.RAG_ANSWER_START), completed);
            Thread.sleep(500);

            // 构建LLM上下文
            String context = buildContextFromDocuments(retrievedDocuments);
            String prompt = buildRagPrompt(request.getQuestion(), context);

            // 调用流式LLM - 使用同步等待确保流式处理完成
            generateStreamAnswerAndWait(prompt, userId, emitter, completed);

            // 在LLM流式处理完成后发送完成信号
            sendSseData(emitter, AgentChatResponse.buildEndMessage("回答生成完成", MessageType.RAG_ANSWER_END), completed);

        } catch (Exception e) {
            log.error("RAG流式对话处理错误", e);
            sendSseData(emitter, createErrorResponse("处理过程中发生错误: " + e.getMessage()), completed);
        } finally {
            safeCompleteEmitter(emitter, completed);
        }
    }

    /** 从指定文件中检索相关文档 */
    private List<DocumentUnitEntity> retrieveFromFile(String fileId, String question, Integer maxResults,
            EmbeddingModelFactory.EmbeddingConfig embeddingConfig) {
        // 查询文件下的所有文档单元
        List<DocumentUnitEntity> fileDocuments = documentUnitDomainService.listVectorizedDocumentsByFile(fileId);

        if (fileDocuments.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用向量搜索在这些文档中检索
        FileDetailEntity fileEntity = fileDetailDomainService.getFileById(fileId);
        List<String> datasetIds = List.of(fileEntity.getDataSetId());

        HybridSearchConfig config = HybridSearchConfig.builder(datasetIds, question).maxResults(maxResults)
                .minScore(0.5).enableRerank(true).candidateMultiplier(2).embeddingConfig(embeddingConfig)
                .enableQueryExpansion(false) // 文件内检索暂时不启用查询扩展，保持现有行为
                .build();
        return hybridSearchDomainService.hybridSearch(config);
    }

    /** 构建检索文档的上下文 */
    private String buildContextFromDocuments(List<DocumentUnitEntity> documents) {
        if (documents.isEmpty()) {
            return "暂无相关文档信息。";
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是相关的文档片段：\n\n");

        for (int i = 0; i < documents.size(); i++) {
            DocumentUnitEntity doc = documents.get(i);
            context.append(String.format("文档片段 %d：\n", i + 1));
            context.append(doc.getContent());
            context.append("\n\n");
        }

        return context.toString();
    }

    /** 构建RAG提示词 */
    private String buildRagPrompt(String question, String context) {
        return String.format(
                "请基于以下提供的文档内容回答用户的问题。如果文档中没有相关信息，请诚实地告知用户。\n\n" + "文档内容：\n%s\n\n" + "用户问题：%s\n\n" + "请提供准确、有帮助的回答：",
                context, question);
    }

    /** 生成流式回答并等待完成
     * @param prompt RAG提示词
     * @param userId 用户ID
     * @param emitter SSE连接
     * @param completed 完成状态标志 */
    private void generateStreamAnswerAndWait(String prompt, String userId, SseEmitter emitter,
            AtomicBoolean completed) {
        try {
            log.info("开始生成RAG回答，用户: {}, 提示词长度: {}", userId, prompt.length());

            // 获取用户默认模型配置
            String userDefaultModelId = userSettingsDomainService.getUserDefaultModelId(userId);
            if (userDefaultModelId == null) {
                log.warn("用户 {} 未配置默认模型，使用临时简化响应", userId);
                generateMockStreamAnswer(emitter, completed);
                return;
            }

            ModelEntity model = llmDomainService.getModelById(userDefaultModelId);
            List<String> fallbackChain = userSettingsDomainService.getUserFallbackChain(userId);

            // 获取最佳服务商（支持高可用、降级）
            HighAvailabilityResult result = highAvailabilityDomainService.selectBestProvider(model, userId,
                    "rag-session-" + userId, fallbackChain);
            ProviderEntity provider = result.getProvider();
            ModelEntity selectedModel = result.getModel();

            // 创建流式LLM客户端
            StreamingChatModel streamingClient = llmServiceFactory.getStreamingClient(provider, selectedModel);

            // 创建Agent并启动流式处理
            Agent agent = buildStreamingAgent(streamingClient);
            TokenStream tokenStream = agent.chat(prompt);

            // 记录调用开始时间
            long startTime = System.currentTimeMillis();

            // 使用CompletableFuture来等待流式处理完成
            CompletableFuture<Void> streamComplete = new CompletableFuture<>();

            // 思维链状态跟踪
            final boolean[] thinkingStarted = {false};
            final boolean[] thinkingEnded = {false};
            final boolean[] hasThinkingProcess = {false};

            // 普通模型的流式处理方式
            tokenStream.onPartialResponse(fragment -> {
                log.debug("收到响应片段: {}", fragment);

                // 如果有思考过程但还没结束思考，先结束思考阶段
                if (hasThinkingProcess[0] && !thinkingEnded[0]) {
                    sendSseData(emitter, AgentChatResponse.build("思考完成", MessageType.RAG_THINKING_END), completed);
                    thinkingEnded[0] = true;
                }

                // 如果没有思考过程且还没开始过思考，先发送思考开始和结束
                if (!hasThinkingProcess[0] && !thinkingStarted[0]) {
                    sendSseData(emitter, AgentChatResponse.build("开始思考...", MessageType.RAG_THINKING_START), completed);
                    sendSseData(emitter, AgentChatResponse.build("思考完成", MessageType.RAG_THINKING_END), completed);
                    thinkingStarted[0] = true;
                    thinkingEnded[0] = true;
                }

                sendSseData(emitter, AgentChatResponse.build(fragment, MessageType.RAG_ANSWER_PROGRESS), completed);
            }).onPartialReasoning(reasoning -> {

                // 标记有思考过程
                hasThinkingProcess[0] = true;

                // 如果还没开始思考，发送思考开始
                if (!thinkingStarted[0]) {
                    sendSseData(emitter, AgentChatResponse.build("开始思考...", MessageType.RAG_THINKING_START), completed);
                    thinkingStarted[0] = true;
                }

                // 发送思考进行中的状态（可选择是否发送思考内容）
                sendSseData(emitter, AgentChatResponse.build(reasoning, MessageType.RAG_THINKING_PROGRESS), completed);
            }).onCompleteReasoning(completeReasoning -> {
                log.info("思维链生成完成，长度: {}", completeReasoning.length());
                log.info("完整思维链内容:\n{}", completeReasoning);
            }).onCompleteResponse(chatResponse -> {
                String fullAnswer = chatResponse.aiMessage().text();
                log.info("RAG回答生成完成，用户: {}, 响应长度: {}", userId, fullAnswer.length());
                log.info("完整RAG回答内容:\n{}", fullAnswer);

                // 上报调用成功结果
                long latency = System.currentTimeMillis() - startTime;
                highAvailabilityDomainService.reportCallResult(result.getInstanceId(), selectedModel.getId(), true,
                        latency, null);

                streamComplete.complete(null);
            }).onError(throwable -> {
                log.error("用户 {} 的RAG流式答案生成错误", userId, throwable);
                sendSseData(emitter, createErrorResponse("回答生成失败: " + throwable.getMessage()), completed);

                long latency = System.currentTimeMillis() - startTime;
                highAvailabilityDomainService.reportCallResult(result.getInstanceId(), selectedModel.getId(), false,
                        latency, throwable.getMessage());

                streamComplete.completeExceptionally(throwable);
            });

            // 启动流处理
            tokenStream.start();

            // 等待流式处理完成，最多等待30分钟
            try {
                streamComplete.get(30, java.util.concurrent.TimeUnit.MINUTES);
            } catch (java.util.concurrent.TimeoutException e) {
                log.warn("LLM流式响应超时，用户: {}", userId);
                sendSseData(emitter, createErrorResponse("响应超时"), completed);
            } catch (Exception e) {
                log.error("等待LLM流式响应时发生错误，用户: {}", userId, e);
            }

        } catch (Exception e) {
            log.error("用户 {} 的RAG流式答案生成错误", userId, e);
            sendSseData(emitter, createErrorResponse("回答生成失败: " + e.getMessage()), completed);
        }
    }

    /** 生成模拟流式回答（备用方案） */
    private void generateMockStreamAnswer(SseEmitter emitter, AtomicBoolean completed) {
        try {
            // 模拟流式回答生成
            String[] responseFragments = {"根据检索到的文档内容，", "我可以为您提供以下回答：\n\n", "这是基于文档内容生成的回答。", "\n\n如需更详细的信息，",
                    "请提供更具体的问题。"};

            // 用于拼接完整回答
            StringBuilder fullMockAnswer = new StringBuilder();

            for (String fragment : responseFragments) {
                fullMockAnswer.append(fragment);
                sendSseData(emitter, AgentChatResponse.build(fragment, MessageType.RAG_ANSWER_PROGRESS), completed);
                Thread.sleep(200);
            }

            log.info("完整模拟RAG回答内容:\n{}", fullMockAnswer);

        } catch (Exception e) {
            log.error("生成模拟流式答案错误", e);
            sendSseData(emitter, createErrorResponse("回答生成失败: " + e.getMessage()), completed);
        }
    }

    /** 构建流式Agent */
    private Agent buildStreamingAgent(StreamingChatModel streamingClient) {
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder().maxMessages(10)
                .chatMemoryStore(new InMemoryChatMemoryStore()).build();
        memory.add(new SystemMessage("""
                    你是一位专业的文档问答助手，你的任务是基于提供的文档回答用户问题。
                你需要遵循以下Markdown格式要求：
                1. 使用标准Markdown语法
                2. 列表项使用 ' - ' 而不是 '*'，确保破折号后有一个空格
                3. 引用页码使用方括号，例如：[页码: 1]
                4. 在每个主要段落之间添加一个空行
                5. 加粗使用 **文本** 格式
                6. 保持一致的缩进，列表项不要过度缩进
                7. 确保列表项之间没有多余的空行
                8. 该加## 这种标题的时候要加上

                回答结构应该是：
                1. 首先是简短的介绍语
                2. 然后是主要内容（使用列表形式）
                3. 最后是"信息来源"部分，总结使用的页面及其贡献
                """));

        return AiServices.builder(Agent.class).streamingChatModel(streamingClient).chatMemory(memory).build();
    }

    /** 安全地完成SseEmitter，避免重复完成错误 */
    private void safeCompleteEmitter(SseEmitter emitter, AtomicBoolean completed) {
        if (completed.compareAndSet(false, true)) {
            try {
                emitter.complete();
                log.debug("SSE发射器成功完成");
            } catch (Exception e) {
                log.debug("SSE发射器完成错误（可能已经完成）: {}", e.getMessage());
            }
        } else {
            log.debug("SSE发射器已经完成，跳过");
        }
    }

    /** 发送SSE数据（带状态检查） */
    private void sendSseData(SseEmitter emitter, AgentChatResponse response, AtomicBoolean completed) {
        // 检查连接是否已经完成
        if (completed.get()) {
            return;
        }

        try {
            String jsonData = objectMapper.writeValueAsString(response);
            emitter.send(SseEmitter.event().data(jsonData));
        } catch (Exception e) {
            log.error("发送SSE数据失败", e);
            // 如果发送失败，标记为已完成避免后续操作
            safeCompleteEmitter(emitter, completed);
        }
    }

    /** 发送SSE数据（向后兼容方法） */
    private void sendSseData(SseEmitter emitter, AgentChatResponse response) {
        try {
            String jsonData = objectMapper.writeValueAsString(response);
            emitter.send(SseEmitter.event().data(jsonData));
        } catch (Exception e) {
            log.error("发送SSE数据失败", e);
        }
    }

    /** 创建错误响应 */
    private AgentChatResponse createErrorResponse(String errorMessage) {
        AgentChatResponse response = AgentChatResponse.buildEndMessage(errorMessage, MessageType.TEXT);
        return response;
    }

    /** 将ModelConfig转换为EmbeddingModelFactory.EmbeddingConfig
     *
     * @param modelConfig RAG模型配置
     * @return 嵌入模型工厂配置 */
    private EmbeddingModelFactory.EmbeddingConfig toEmbeddingConfig(ModelConfig modelConfig) {
        return new EmbeddingModelFactory.EmbeddingConfig(modelConfig.getApiKey(), modelConfig.getBaseUrl(),
                modelConfig.getModelId());
    }

    /** 检索到的文档信息（内部类） */
    private static class RetrievedDocument {
        private String fileId;
        private String fileName;
        private String documentId;
        private Double score;

        public RetrievedDocument() {
        }

        public RetrievedDocument(String fileId, String fileName, String documentId, Double score) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.documentId = documentId;
            this.score = score;
        }

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getDocumentId() {
            return documentId;
        }

        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }
    }

    /** 基于已安装知识库的RAG流式问答
     *
     * @param request 流式问答请求
     * @param userRagId 用户RAG安装记录ID
     * @param userId 用户ID
     * @return SSE流式响应 */
    public SseEmitter ragStreamChatByUserRag(RagStreamChatRequest request, String userRagId, String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        AtomicBoolean completed = new AtomicBoolean(false);

        // 设置连接关闭回调
        emitter.onCompletion(() -> {
            completed.set(true);
            log.info("用户 {} 通过userRag {} 的RAG流式对话完成", userId, userRagId);
        });
        emitter.onTimeout(() -> {
            log.warn("用户 {} 通过userRag {} 的RAG流式对话超时", userId, userRagId);
            sendSseData(emitter, createErrorResponse("连接超时"), completed);
            safeCompleteEmitter(emitter, completed);
        });
        emitter.onError((ex) -> {
            log.error("用户 {} 通过userRag {} 的RAG流式对话连接错误", userId, userRagId, ex);
            safeCompleteEmitter(emitter, completed);
        });

        // 异步处理流式问答
        CompletableFuture.runAsync(() -> {
            try {
                processRagStreamChatByUserRag(request, userRagId, userId, emitter, completed);
            } catch (Exception e) {
                log.error("通过userRag的RAG流式对话错误", e);
                sendSseData(emitter, createErrorResponse("处理过程中发生错误: " + e.getMessage()), completed);
            } finally {
                // 确保连接被正确关闭
                safeCompleteEmitter(emitter, completed);
            }
        });

        return emitter;
    }

    /** 处理基于用户RAG的流式问答核心逻辑 */
    private void processRagStreamChatByUserRag(RagStreamChatRequest request, String userRagId, String userId,
            SseEmitter emitter, AtomicBoolean completed) {
        try {
            // 检查用户RAG是否存在和有权限访问
            if (!ragDataAccessService.canAccessRag(userId, userRagId)) {
                sendSseData(emitter, createErrorResponse("处理过程中发生错误: 数据集不存在"), completed);
                return;
            }

            // 获取RAG数据源信息
            var dataSourceInfo = ragDataAccessService.getRagDataSourceInfo(userId, userRagId);
            log.info("开始RAG流式对话 通过userRag: {}, 用户: {}, 问题: '{}', 安装类型: {}", userRagId, userId, request.getQuestion(),
                    dataSourceInfo.getInstallType());

            // 第一阶段：检索文档
            sendSseData(emitter, AgentChatResponse.build("开始检索相关文档...", MessageType.RAG_RETRIEVAL_START), completed);
            Thread.sleep(500);

            // 获取用户的嵌入模型配置
            ModelConfig embeddingModelConfig = ragModelConfigService.getUserEmbeddingModelConfig(userId);
            EmbeddingModelFactory.EmbeddingConfig embeddingConfig = toEmbeddingConfig(embeddingModelConfig);

            List<DocumentUnitEntity> retrievedDocuments;

            // 根据RAG类型选择不同的数据源
            if (dataSourceInfo.getIsRealTime()) {
                // REFERENCE类型：使用原始RAG的数据集进行向量搜索
                List<String> ragDatasetIds = List.of(dataSourceInfo.getOriginalRagId());
                // 使用HybridSearchConfig配置对象调用混合检索
                HybridSearchConfig config = HybridSearchConfig.builder(ragDatasetIds, request.getQuestion())
                        .maxResults(request.getMaxResults()).minScore(request.getMinScore())
                        .enableRerank(request.getEnableRerank()).candidateMultiplier(2).embeddingConfig(embeddingConfig)
                        .enableQueryExpansion(false) // UserRag流式问答中暂时不启用查询扩展，保持现有行为
                        .build();
                retrievedDocuments = hybridSearchDomainService.hybridSearch(config);
            } else {
                // SNAPSHOT类型：使用用户快照数据进行检索
                retrievedDocuments = ragDataAccessService.getRagDocuments(userId, userRagId);

                // 如果快照数据为空，返回空结果
                if (retrievedDocuments.isEmpty()) {
                    log.info("用户RAG [{}] 的快照数据为空，无法进行检索", userRagId);
                } else {
                    // 对快照文档进行相关性过滤和排序
                    retrievedDocuments = filterAndRankSnapshotDocuments(retrievedDocuments, request.getQuestion(),
                            request.getMaxResults(), embeddingConfig);
                }
            }

            // 构建检索结果
            List<RetrievedDocument> retrievedDocs = new ArrayList<>();

            if (dataSourceInfo.getIsRealTime()) {
                // REFERENCE类型：使用原始文件信息
                for (DocumentUnitEntity doc : retrievedDocuments) {
                    FileDetailEntity fileDetail = fileDetailDomainService.getFileById(doc.getFileId());
                    double similarityScore = doc.getSimilarityScore() != null ? doc.getSimilarityScore() : 0.0;
                    retrievedDocs.add(new RetrievedDocument(doc.getFileId(),
                            fileDetail != null ? fileDetail.getOriginalFilename() : "未知文件", doc.getId(),
                            similarityScore));
                }
            } else {
                // SNAPSHOT类型：使用快照文件信息
                for (DocumentUnitEntity doc : retrievedDocuments) {
                    // doc.getFileId() 在SNAPSHOT模式下是 user_rag_files 的ID
                    UserRagFileEntity userFile = userRagFileDomainService.getById(doc.getFileId());
                    double similarityScore = doc.getSimilarityScore() != null ? doc.getSimilarityScore() : 0.0;
                    retrievedDocs.add(new RetrievedDocument(doc.getFileId(),
                            userFile != null ? userFile.getFileName() : "未知文件", doc.getId(), similarityScore));
                }
            }

            // 发送检索完成信号
            String retrievalMessage = String.format("检索完成，找到 %d 个相关文档", retrievedDocs.size());
            AgentChatResponse retrievalEndResponse = AgentChatResponse.build(retrievalMessage,
                    MessageType.RAG_RETRIEVAL_END);
            try {
                retrievalEndResponse.setPayload(objectMapper.writeValueAsString(retrievedDocs));
            } catch (Exception e) {
                log.error("失败：序列化检索到的文档", e);
            }
            sendSseData(emitter, retrievalEndResponse, completed);

            Thread.sleep(500);

            // 第二阶段：生成回答
            sendSseData(emitter, AgentChatResponse.build("开始生成回答...", MessageType.RAG_ANSWER_START), completed);
            Thread.sleep(500);

            // 构建LLM上下文
            String context = buildContextFromDocuments(retrievedDocuments);
            String prompt = buildRagPrompt(request.getQuestion(), context);

            // 调用流式LLM - 使用同步等待确保流式处理完成
            generateStreamAnswerAndWait(prompt, userId, emitter, completed);

            // 在LLM流式处理完成后发送完成信号
            sendSseData(emitter, AgentChatResponse.buildEndMessage("回答生成完成", MessageType.RAG_ANSWER_END), completed);

        } catch (Exception e) {
            log.error("processRagStreamChatByUserRag处理错误", e);
            sendSseData(emitter, createErrorResponse("处理过程中发生错误: " + e.getMessage()), completed);
        }
    }

    /** 对快照文档进行相关性过滤和排序
     *
     * @param documents 快照文档列表
     * @param question 用户问题
     * @param maxResults 最大返回数量
     * @param embeddingConfig 嵌入模型配置
     * @return 过滤和排序后的文档列表 */
    private List<DocumentUnitEntity> filterAndRankSnapshotDocuments(List<DocumentUnitEntity> documents, String question,
            Integer maxResults, EmbeddingModelFactory.EmbeddingConfig embeddingConfig) {

        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 获取嵌入模型
            EmbeddingModel embeddingModel = embeddingModelFactory.createEmbeddingModel(embeddingConfig);

            // 计算问题的向量
            Embedding questionEmbedding = embeddingModel.embed(question).content();

            // 为每个文档计算相似度
            List<DocumentWithScore> documentsWithScores = new ArrayList<>();
            for (DocumentUnitEntity doc : documents) {
                try {
                    // 计算文档内容的向量
                    Embedding docEmbedding = embeddingModel.embed(doc.getContent()).content();

                    // 计算余弦相似度
                    double similarity = cosineSimilarity(questionEmbedding.vectorAsList(), docEmbedding.vectorAsList());

                    // 设置相似度分数到文档实体
                    doc.setSimilarityScore(similarity);
                    documentsWithScores.add(new DocumentWithScore(doc, similarity));

                } catch (Exception e) {
                    log.warn("计算文档相似度失败: {}", e.getMessage());
                    // 如果计算失败，设置较低的相似度分数
                    doc.setSimilarityScore(0.0);
                    documentsWithScores.add(new DocumentWithScore(doc, 0.0));
                }
            }

            // 按相似度降序排序
            documentsWithScores.sort((a, b) -> Double.compare(b.score, a.score));

            // 限制返回数量
            int limit = maxResults != null
                    ? Math.min(maxResults, documentsWithScores.size())
                    : documentsWithScores.size();

            return documentsWithScores.stream().limit(limit).map(dws -> dws.document).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("快照文档相关性过滤失败", e);
            // 如果计算相似度失败，返回前N个文档
            int limit = maxResults != null ? Math.min(maxResults, documents.size()) : documents.size();
            return documents.subList(0, limit);
        }
    }

    /** 计算两个向量的余弦相似度 */
    private double cosineSimilarity(List<Float> vectorA, List<Float> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** 文档与分数的内部类 */
    private static class DocumentWithScore {
        final DocumentUnitEntity document;
        final double score;

        DocumentWithScore(DocumentUnitEntity document, double score) {
            this.document = document;
            this.score = score;
        }
    }

}
