package org.xhy.domain.conversation.handler;

import org.xhy.infrastructure.transport.MessageTransport;

/**
 * 消息处理器接口
 */
public interface MessageHandler {
    
    /**
     * 处理对话
     * 
     * @param environment 对话环境
     * @param transport 消息传输实现
     * @return 连接对象
     * @param <T> 连接类型
     */
    <T> T handleChat(ChatEnvironment environment, MessageTransport<T> transport);
} 