package org.xhy.domain.conversation.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.domain.conversation.model.MessageDTO;

/**
 * 对话服务接口
 * 处理带上下文的对话流程
 */
public interface ConversationService {

    /**
     * 发送消息并流式获取回复
     * 
     * @param sessionId 会话ID
     * @param content   用户消息内容
     * @return SSE事件发射器
     */
    SseEmitter chat(String sessionId, String content);

    /**
     * 发送消息并获取回复（非流式）
     * 
     * @param sessionId 会话ID
     * @param content   用户消息内容
     * @return 助手回复消息
     */
    MessageDTO chatSync(String sessionId, String content);

    /**
     * 创建新会话并发送第一条消息
     * 
     * @param title   会话标题
     * @param userId  用户ID
     * @param content 用户消息内容
     * @return SSE事件发射器
     */
    SseEmitter createSessionAndChat(String title, String userId, String content);

    /**
     * 清除会话上下文
     * 
     * @param sessionId 会话ID
     */
    void clearContext(String sessionId);
}