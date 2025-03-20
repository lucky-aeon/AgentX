package org.xhy.domain.conversation.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.domain.conversation.model.Message;
import org.xhy.domain.conversation.model.MessageDTO;
import org.xhy.domain.conversation.model.Session;
import org.xhy.domain.conversation.model.SessionDTO;
import org.xhy.domain.conversation.service.ContextService;
import org.xhy.domain.conversation.service.ConversationService;
import org.xhy.domain.conversation.service.MessageService;
import org.xhy.domain.conversation.service.SessionService;
import org.xhy.domain.llm.model.LlmMessage;
import org.xhy.domain.llm.model.LlmRequest;
import org.xhy.domain.llm.service.LlmService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 对话服务实现
 */
@Service
public class ConversationServiceImpl implements ConversationService {

    private final Logger log = LoggerFactory.getLogger(ConversationServiceImpl.class);

    private final SessionService sessionService;
    private final MessageService messageService;
    private final ContextService contextService;
    private final LlmService llmService;

    // 线程池，用于处理异步SSE请求
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // 默认的系统提示词
    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个有帮助的AI助手，请尽可能准确、有用地回答用户问题。";

    /**
     * 构造函数注入
     */
    public ConversationServiceImpl(SessionService sessionService,
            MessageService messageService,
            ContextService contextService,
            LlmService llmService) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.contextService = contextService;
        this.llmService = llmService;
    }

    @Override
    public SseEmitter chat(String sessionId, String content) {
        // 创建SSE发射器，设置超时时间为5分钟
        SseEmitter emitter = new SseEmitter(300000L);

        // 保存用户消息
        MessageDTO userMessageDTO = messageService.sendUserMessage(sessionId, content);

        // 异步处理LLM请求
        executorService.execute(() -> {
            try {
                // 1. 获取上下文消息
                List<Message> contextMessages = contextService.getContextMessages(sessionId);

                // 2. 将上下文消息转换为LLM消息格式
                List<LlmMessage> llmMessages = convertToLlmMessages(contextMessages);

                // 3. 创建LLM请求
                LlmRequest request = new LlmRequest();
                request.setMessages(llmMessages);
                request.setStream(true);

                // 4. 发送流式请求
                List<String> responseChunks = llmService.chatStreamList(request);

                // 5. 逐块发送给客户端
                StringBuilder fullResponse = new StringBuilder();
                for (String chunk : responseChunks) {
                    fullResponse.append(chunk);
                    emitter.send(SseEmitter.event()
                            .id(UUID.randomUUID().toString())
                            .name("message")
                            .data(chunk));
                }

                // 6. 保存完整的助手回复
                MessageDTO assistantMessageDTO = messageService.saveAssistantMessage(
                        sessionId,
                        fullResponse.toString(),
                        llmService.getProviderName(),
                        llmService.getDefaultModel(),
                        null // 这里token数量暂时为null，实际项目中可以计算
                );

                // 7. 发送完成事件
                emitter.send(SseEmitter.event()
                        .id(UUID.randomUUID().toString())
                        .name("done")
                        .data(assistantMessageDTO));

                // 8. 完成
                emitter.complete();

            } catch (Exception e) {
                log.error("Stream chat error", e);
                try {
                    emitter.send(SseEmitter.event()
                            .id(UUID.randomUUID().toString())
                            .name("error")
                            .data(e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

    @Override
    public MessageDTO chatSync(String sessionId, String content) {
        // 保存用户消息
        messageService.sendUserMessage(sessionId, content);

        // 获取上下文消息
        List<Message> contextMessages = contextService.getContextMessages(sessionId);

        // 将上下文消息转换为LLM消息格式
        List<LlmMessage> llmMessages = convertToLlmMessages(contextMessages);

        // 创建LLM请求
        LlmRequest request = new LlmRequest();
        request.setMessages(llmMessages);

        // 发送同步请求
        String response = llmService.chat(request).getContent();

        // 保存助手回复
        return messageService.saveAssistantMessage(
                sessionId,
                response,
                llmService.getProviderName(),
                llmService.getDefaultModel(),
                null // 这里token数量暂时为null，实际项目中可以计算
        );
    }

    @Override
    public SseEmitter createSessionAndChat(String title, String userId, String content) {
        // 1. 创建新会话
        SessionDTO sessionDTO = sessionService.createSession(title, userId, null);
        String sessionId = sessionDTO.getId();

        // 2. 添加系统消息
        messageService.saveSystemMessage(sessionId, DEFAULT_SYSTEM_PROMPT);

        // 3. 发送第一条消息并获取回复
        return this.chat(sessionId, content);
    }

    @Override
    public void clearContext(String sessionId) {
        contextService.clearContext(sessionId);
    }

    /**
     * 将内部消息转换为LLM消息格式
     */
    private List<LlmMessage> convertToLlmMessages(List<Message> messages) {
        List<LlmMessage> llmMessages = new ArrayList<>();
        for (Message message : messages) {
            llmMessages.add(new LlmMessage(message.getRole(), message.getContent()));
        }
        return llmMessages;
    }
}