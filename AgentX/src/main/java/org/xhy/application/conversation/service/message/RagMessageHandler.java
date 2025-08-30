package org.xhy.application.conversation.service.message;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.application.conversation.service.message.agent.tool.RagToolManager;
import org.xhy.application.conversation.service.ChatSessionManager;
import org.xhy.application.conversation.service.message.rag.RagChatContext;
import org.xhy.application.conversation.service.message.rag.RagRetrievalResult;
import org.xhy.application.conversation.dto.RagRetrievalDocumentDTO;
import org.xhy.application.rag.dto.DocumentUnitDTO;
import org.xhy.application.rag.service.search.RAGSearchAppService;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.domain.conversation.service.SessionDomainService;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.user.service.UserSettingsDomainService;
import org.xhy.domain.user.service.AccountDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;
import org.xhy.application.billing.service.BillingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** RAG专用的消息处理器 继承AbstractMessageHandler，添加RAG检索和问答的特定逻辑 */
@Component("ragMessageHandler")
public class RagMessageHandler extends AbstractMessageHandler {

    private final static String ragSystemPrompt = """
            你是一个专业、精准的AI助手。请严格且仅根据提供的<context>来回答用户的问题。请遵循以下规则：
            1.  ** grounding（信息 grounding）**：你的答案必须完全基于提供的<context>生成。不允许引入外部知识或内部记忆。
            2.  ** 准确性 **：如果<context>中包含的具体信息能直接回答问题，请直接、准确地引用这些信息。
            3.  ** 不确定性处理 **：如果<context>中的信息不足以完全回答问题，或者信息与问题部分相关但不完全匹配，请在回答中明确指出信息的局限性。
            4.  ** 拒绝机制 **：如果<context>中完全没有任何与问题相关的信息，或者问题超出了提供的文档范围，你必须明确且礼貌地告知用户“根据提供的资料，我无法找到相关信息来回答这个问题。” 严禁编造答案（即防止幻觉）。
            5.  ** 格式与结构 **：在可能的情况下，使用清晰、有条理的方式组织答案（如分点、列表或简短的段落）。如果答案涉及多个方面，请合理地进行分点说明。

            context为：${context}

            请现在开始处理用户的问题。
                """;

    private static final Logger logger = LoggerFactory.getLogger(RagMessageHandler.class);

    private final RAGSearchAppService ragSearchAppService;
    private final ObjectMapper objectMapper;

    public RagMessageHandler(LLMServiceFactory llmServiceFactory, MessageDomainService messageDomainService,
            HighAvailabilityDomainService highAvailabilityDomainService, SessionDomainService sessionDomainService,
            UserSettingsDomainService userSettingsDomainService, LLMDomainService llmDomainService,
            RagToolManager ragToolManager, BillingService billingService, AccountDomainService accountDomainService,
            ChatSessionManager chatSessionManager, RAGSearchAppService ragSearchAppService, ObjectMapper objectMapper) {
        super(llmServiceFactory, messageDomainService, highAvailabilityDomainService, sessionDomainService,
                userSettingsDomainService, llmDomainService, ragToolManager, billingService, accountDomainService,
                chatSessionManager);
        this.ragSearchAppService = ragSearchAppService;
        this.objectMapper = objectMapper;
    }

    /** 重写流式聊天处理，添加RAG检索逻辑 */
    @Override
    protected <T> void processStreamingChat(ChatContext chatContext, T connection, MessageTransport<T> transport,
            MessageEntity userEntity, MessageEntity llmEntity, MessageWindowChatMemory memory,
            ToolProvider toolProvider) {

        // 检查是否是RAG上下文
        if (!(chatContext instanceof RagChatContext)) {
            throw new IllegalArgumentException("RagMessageHandler requires RagChatContext");
        }

        RagChatContext ragContext = (RagChatContext) chatContext;

        try {
            // 第一阶段：RAG检索
            RagRetrievalResult retrievalResult = performRagRetrieval(ragContext, transport, connection);

            if (!retrievalResult.hasDocuments()) {
                transport.sendEndMessage(connection, AgentChatResponse.build("没有搜索到相关文档，可以换一个方式提问", MessageType.TEXT));
            }

            // 第二阶段：基于检索结果生成回答
            generateRagAnswer(ragContext, retrievalResult, connection, transport, userEntity, llmEntity, memory,
                    toolProvider);

        } catch (Exception e) {
            logger.error("RAG流式处理失败", e);
            AgentChatResponse errorResponse = AgentChatResponse.buildEndMessage("处理过程中发生错误: " + e.getMessage(),
                    MessageType.TEXT);
            transport.sendMessage(connection, errorResponse);
        }
    }

    /** 执行RAG检索
     * @param ragContext RAG聊天上下文
     * @param transport 消息传输
     * @param connection 连接
     * @return 检索结果 */
    private <T> RagRetrievalResult performRagRetrieval(RagChatContext ragContext, MessageTransport<T> transport,
            T connection) {
        try {
            // 发送检索开始信号
            transport.sendMessage(connection, AgentChatResponse.build("开始检索相关文档...", MessageType.RAG_RETRIEVAL_START));
            Thread.sleep(500);

            // 执行RAG检索 - 获取完整数据用于答案生成
            List<DocumentUnitDTO> fullRetrievedDocuments;
            if (ragContext.getUserRagId() != null) {
                // 基于已安装知识库检索
                fullRetrievedDocuments = ragSearchAppService.ragSearchByUserRag(ragContext.getRagSearchRequest(),
                        ragContext.getUserRagId(), ragContext.getUserId());
            } else {
                // 基于数据集ID检索
                fullRetrievedDocuments = ragSearchAppService.ragSearch(ragContext.getRagSearchRequest(),
                        ragContext.getUserId());
            }

            // 转换为轻量级DTO用于前端展示
            List<RagRetrievalDocumentDTO> lightweightDocuments = convertToLightweightDTOs(fullRetrievedDocuments);

            // 构建检索结果响应
            String retrievalMessage = String.format("检索完成，找到 %d 个相关文档", lightweightDocuments.size());
            AgentChatResponse retrievalEndResponse = AgentChatResponse.build(retrievalMessage,
                    MessageType.RAG_RETRIEVAL_END);

            // 设置轻量级文档作为payload（优化传输）
            try {
                retrievalEndResponse.setPayload(objectMapper.writeValueAsString(lightweightDocuments));
            } catch (Exception e) {
                logger.error("序列化检索文档失败", e);
            }

            transport.sendMessage(connection, retrievalEndResponse);
            Thread.sleep(500);

            // 返回包含完整数据的结果用于答案生成
            return new RagRetrievalResult(fullRetrievedDocuments, retrievalMessage);

        } catch (Exception e) {
            logger.error("RAG检索失败", e);
            transport.sendMessage(connection, AgentChatResponse.build("文档检索失败: " + e.getMessage(), MessageType.TEXT));
            return new RagRetrievalResult(Collections.emptyList(), "检索失败");
        }
    }

    /** 基于检索结果生成回答
     * @param ragContext RAG聊天上下文
     * @param retrievalResult 检索结果
     * @param connection 连接
     * @param transport 消息传输
     * @param userEntity 用户消息实体
     * @param llmEntity LLM消息实体
     * @param memory 聊天内存
     * @param toolProvider 工具提供者 */
    private <T> void generateRagAnswer(RagChatContext ragContext, RagRetrievalResult retrievalResult, T connection,
            MessageTransport<T> transport, MessageEntity userEntity, MessageEntity llmEntity,
            MessageWindowChatMemory memory, ToolProvider toolProvider) {

        // 发送回答生成开始信号
        transport.sendMessage(connection, AgentChatResponse.build("开始生成回答...", MessageType.RAG_ANSWER_START));

        // 保存用户消息
        messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(userEntity),
                ragContext.getContextEntity());

        // 获取流式LLM客户端
        StreamingChatModel streamingClient = llmServiceFactory.getStreamingClient(ragContext.getProvider(),
                ragContext.getModel());

        // 创建RAG专用的流式Agent
        Agent agent = buildRagStreamingAgent(streamingClient, memory, toolProvider, ragContext.getAgent(),
                retrievalResult.getRetrievedDocuments());

        // 启动流式处理
        processRagChat(agent, connection, transport, ragContext, userEntity, llmEntity, ragContext.getUserMessage());
    }

    /** RAG专用的聊天处理逻辑 */
    private <T> void processRagChat(Agent agent, T connection, MessageTransport<T> transport, RagChatContext ragContext,
            MessageEntity userEntity, MessageEntity llmEntity, String ragPrompt) {

        AtomicReference<StringBuilder> messageBuilder = new AtomicReference<>(new StringBuilder());
        TokenStream tokenStream = agent.chat(ragPrompt);

        // 记录调用开始时间
        long startTime = System.currentTimeMillis();

        // 思维链状态跟踪
        final boolean[] thinkingStarted = {false};
        final boolean[] thinkingEnded = {false};
        final boolean[] hasThinkingProcess = {false};

        // 错误处理
        tokenStream.onError(throwable -> {
            transport.sendMessage(connection,
                    AgentChatResponse.buildEndMessage(throwable.getMessage(), MessageType.TEXT));

            // 上报调用失败结果
            long latency = System.currentTimeMillis() - startTime;
            highAvailabilityDomainService.reportCallResult(ragContext.getInstanceId(), ragContext.getModel().getId(),
                    false, latency, throwable.getMessage());
        });

        // 部分回答处理
        tokenStream.onPartialResponse(fragment -> {
            // 如果有思考过程但还没结束思考，先结束思考阶段
            if (hasThinkingProcess[0] && !thinkingEnded[0]) {
                transport.sendMessage(connection, AgentChatResponse.build("思考完成", MessageType.RAG_THINKING_END));
                thinkingEnded[0] = true;
            }

            // 如果没有思考过程且还没开始过思考，先发送思考开始和结束
            if (!hasThinkingProcess[0] && !thinkingStarted[0]) {
                transport.sendMessage(connection, AgentChatResponse.build("开始思考...", MessageType.RAG_THINKING_START));
                transport.sendMessage(connection, AgentChatResponse.build("思考完成", MessageType.RAG_THINKING_END));
                thinkingStarted[0] = true;
                thinkingEnded[0] = true;
            }

            messageBuilder.get().append(fragment);
            transport.sendMessage(connection, AgentChatResponse.build(fragment, MessageType.RAG_ANSWER_PROGRESS));
        });

        // 思维链处理
        tokenStream.onPartialReasoning(reasoning -> {
            hasThinkingProcess[0] = true;
            if (!thinkingStarted[0]) {
                transport.sendMessage(connection, AgentChatResponse.build("开始思考...", MessageType.RAG_THINKING_START));
                thinkingStarted[0] = true;
            }
            transport.sendMessage(connection, AgentChatResponse.build(reasoning, MessageType.RAG_THINKING_PROGRESS));
        });

        // 完整响应处理
        tokenStream.onCompleteResponse(chatResponse -> {
            this.setMessageTokenCount(ragContext.getMessageHistory(), userEntity, llmEntity, chatResponse);

            messageDomainService.updateMessage(userEntity);
            messageDomainService.saveMessageAndUpdateContext(Collections.singletonList(llmEntity),
                    ragContext.getContextEntity());

            // 发送RAG回答结束信号
            transport.sendMessage(connection, AgentChatResponse.buildEndMessage("回答生成完成", MessageType.RAG_ANSWER_END));

            // 上报调用成功结果
            long latency = System.currentTimeMillis() - startTime;
            highAvailabilityDomainService.reportCallResult(ragContext.getInstanceId(), ragContext.getModel().getId(),
                    true, latency, null);

            // 执行模型调用计费
            performBillingWithErrorHandling(ragContext, chatResponse.tokenUsage().inputTokenCount(),
                    chatResponse.tokenUsage().outputTokenCount(), transport, connection);

            smartRenameSession(ragContext);
        });

        // 启动流处理
        tokenStream.start();
    }

    /** 将DocumentUnitDTO转换为轻量级展示DTO */
    private List<RagRetrievalDocumentDTO> convertToLightweightDTOs(List<DocumentUnitDTO> documents) {
        List<RagRetrievalDocumentDTO> lightweightDTOs = new ArrayList<>();

        for (DocumentUnitDTO doc : documents) {
            try {
                // 需要根据fileId查询文件名，这里先使用默认值
                String fileName = getFileNameFromCache(doc.getFileId());

                // 创建轻量级DTO，只包含前端需要的字段
                RagRetrievalDocumentDTO lightweightDTO = new RagRetrievalDocumentDTO(doc.getFileId(), fileName,
                        doc.getId(), // documentId
                        0.85, // 默认相似度，实际应该从其他地方获取
                        doc.getPage());

                lightweightDTOs.add(lightweightDTO);

            } catch (Exception e) {
                logger.warn("转换轻量级DTO失败，文档ID: {}", doc.getId(), e);
                // 使用默认值
                RagRetrievalDocumentDTO lightweightDTO = new RagRetrievalDocumentDTO(doc.getFileId(), "未知文件",
                        doc.getId(), 0.0, doc.getPage());
                lightweightDTOs.add(lightweightDTO);
            }
        }

        return lightweightDTOs;
    }

    /** 从缓存或数据库获取文件名（简化实现） */
    private String getFileNameFromCache(String fileId) {
        // 这里应该实现文件名查询逻辑，暂时返回默认值
        return "文档_" + fileId.substring(0, Math.min(8, fileId.length()));
    }

    /** 构建RAG提示词
     * @param question 用户问题
     * @param documents 检索到的文档
     * @return RAG提示词 */
    private String buildRagPrompt(String question, List<DocumentUnitDTO> documents) {
        StringBuilder context = new StringBuilder();
        context.append("以下是相关的文档片段：\n\n");

        for (int i = 0; i < documents.size(); i++) {
            DocumentUnitDTO doc = documents.get(i);
            context.append(String.format("文档片段 %d：\n", i + 1));
            context.append(doc.getContent());
            context.append("\n\n");
        }

        return String.format(
                "请基于以下提供的文档内容回答用户的问题。如果文档中没有相关信息，请诚实地告知用户。\n\n" + "文档内容：\n%s\n\n" + "用户问题：%s\n\n" + "请提供准确、有帮助的回答：",
                context.toString(), question);
    }

    /** 构建RAG专用的流式Agent */
    private Agent buildRagStreamingAgent(StreamingChatModel model, MessageWindowChatMemory memory,
            ToolProvider toolProvider, AgentEntity agent, List<DocumentUnitDTO> documentUnitDTOS) {

        // 为RAG对话添加专用的系统提示词
        MessageWindowChatMemory ragMemory = MessageWindowChatMemory.builder().maxMessages(1000)
                .chatMemoryStore(new InMemoryChatMemoryStore()).build();

        // 添加RAG专用系统提示词
        ragMemory.add(new SystemMessage(ragSystemPrompt.replace("${context}", documentUnitDTOS.toString())));

        return buildStreamingAgent(model, ragMemory, toolProvider, agent);
    }

    /** 设置消息Token计数（调用父类方法） */
    private void setMessageTokenCount(List<MessageEntity> historyMessages, MessageEntity userEntity,
            MessageEntity llmEntity, dev.langchain4j.model.chat.response.ChatResponse chatResponse) {
        // 调用父类AbstractMessageHandler中的方法
        llmEntity.setTokenCount(chatResponse.tokenUsage().outputTokenCount());
        llmEntity.setBodyTokenCount(chatResponse.tokenUsage().outputTokenCount());
        llmEntity.setContent(chatResponse.aiMessage().text());

        int bodyTokenSum = 0;
        if (historyMessages != null && !historyMessages.isEmpty()) {
            bodyTokenSum = historyMessages.stream().filter(java.util.Objects::nonNull)
                    .mapToInt(MessageEntity::getBodyTokenCount).sum();
        }
        userEntity.setTokenCount(chatResponse.tokenUsage().inputTokenCount());
        userEntity.setBodyTokenCount(chatResponse.tokenUsage().inputTokenCount() - bodyTokenSum);
    }
}