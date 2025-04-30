package org.xhy.infrastructure.transport;

import org.xhy.application.conversation.dto.AgentChatResponse;

/** 消息传输接口，用于抽象不同的消息传输方式(SSE、WebSocket等) */
public interface MessageTransport<T> {
    /** 创建连接
     *
     * @param timeout 超时时间(毫秒)
     * @return 连接对象 */
    T createConnection(long timeout);

    /** 发送消息
     *
     * @param connection 连接对象
     * @param streamChatResponse 消息内容 */
    void sendMessage(T connection, AgentChatResponse streamChatResponse);

    /** 发送消息
     *
     * @param connection 连接对象
     * @param streamChatResponse 消息内容 */
    void sendEndMessage(T connection, AgentChatResponse streamChatResponse);

    /** 完成连接
     *
     * @param connection 连接对象 */
    void completeConnection(T connection);

    /** 处理错误
     *
     * @param connection 连接对象
     * @param error 错误对象 */
    void handleError(T connection, Throwable error);
}
