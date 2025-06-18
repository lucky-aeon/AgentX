package org.xhy.infrastructure.transport;

import org.springframework.stereotype.Component;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.application.conversation.dto.ChatResponse;

/** 同步消息传输实现 */
@Component
public class SyncMessageTransport implements MessageTransport<ChatResponse> {

    @Override
    public ChatResponse createConnection(long timeout) {
        return new ChatResponse();
    }

    @Override
    public void sendMessage(ChatResponse connection, AgentChatResponse streamChatResponse) {
        // 同步模式下，累积消息内容
        String existingContent = connection.getContent() != null ? connection.getContent() : "";
        connection.setContent(existingContent + streamChatResponse.getContent());
        connection.setTimestamp(streamChatResponse.getTimestamp());
    }

    @Override
    public void sendEndMessage(ChatResponse connection, AgentChatResponse streamChatResponse) {
        // 同步模式下，设置最终响应
        if (streamChatResponse.getContent() != null && !streamChatResponse.getContent().isEmpty()) {
            String existingContent = connection.getContent() != null ? connection.getContent() : "";
            connection.setContent(existingContent + streamChatResponse.getContent());
        }
        connection.setTimestamp(streamChatResponse.getTimestamp());
    }

    @Override
    public void completeConnection(ChatResponse connection) {
        // 同步模式下无需特殊处理
    }

    @Override
    public void handleError(ChatResponse connection, Throwable error) {
        connection.setContent("错误: " + error.getMessage());
        connection.setTimestamp(System.currentTimeMillis());
    }
}