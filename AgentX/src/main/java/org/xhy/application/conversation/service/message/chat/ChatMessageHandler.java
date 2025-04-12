package org.xhy.application.conversation.service.message.chat;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.application.conversation.service.message.AbstractMessageHandler;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.application.conversation.service.handler.content.ChatContext;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;

/**
 * 标准消息处理器
 */
@Component(value = "chatMessageHandler")
public class ChatMessageHandler extends AbstractMessageHandler {


    public ChatMessageHandler(ConversationDomainService conversationDomainService, ContextDomainService contextDomainService, LLMServiceFactory llmServiceFactory) {
        super(conversationDomainService, contextDomainService, llmServiceFactory);
    }

    @Override
    public <T> T chat(ChatContext chatContext, MessageTransport<T> transport) {
        // 创建用户消息实体
        MessageEntity userMessageEntity = createUserMessage(chatContext);

        // 创建LLM消息实体
        MessageEntity llmMessageEntity = createLlmMessage(chatContext);

        // 创建连接
        T connection = transport.createConnection(CONNECTION_TIMEOUT);

        // 准备LLM请求
        dev.langchain4j.model.chat.request.ChatRequest llmRequest = prepareChatRequest(chatContext);

        // 获取LLM客户端
        StreamingChatLanguageModel llmClient = llmServiceFactory.getStreamingClient(
                chatContext.getProvider(), chatContext.getModel());

        // 处理对话
        processChat(llmClient, llmRequest, connection, transport, chatContext,
                  userMessageEntity, llmMessageEntity);

        return connection;
    }

    /**
     * 处理对话
     */
    protected <T> void processChat(
            StreamingChatLanguageModel llmClient,
            dev.langchain4j.model.chat.request.ChatRequest llmRequest,
            T connection,
            MessageTransport<T> transport,
            ChatContext chatContext,
            MessageEntity userMessageEntity,
            MessageEntity llmMessageEntity) {

        llmClient.doChat(llmRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                AgentChatResponse response = AgentChatResponse.build(partialResponse, MessageType.TEXT);
                transport.sendMessage(
                        connection,
                        response
                );
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                TokenUsage tokenUsage = completeResponse.metadata().tokenUsage();

                // 设置用户消息token数
                Integer inputTokenCount = tokenUsage.inputTokenCount();
                userMessageEntity.setTokenCount(inputTokenCount);

                // 设置LLM消息内容和token数
                Integer outputTokenCount = tokenUsage.outputTokenCount();
                llmMessageEntity.setTokenCount(outputTokenCount);
                llmMessageEntity.setContent(completeResponse.aiMessage().text());

                AgentChatResponse response = AgentChatResponse.buildEndMessage(MessageType.TEXT);

                saveMessages(chatContext, userMessageEntity, llmMessageEntity);

                // 发送完成消息
                transport.sendEndMessage(connection, response);
            }

            @Override
            public void onError(Throwable error) {
                transport.handleError(connection, error);
            }
        });
    }
} 