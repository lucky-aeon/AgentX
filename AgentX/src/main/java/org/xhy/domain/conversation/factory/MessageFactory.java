package org.xhy.domain.conversation.factory;

import org.springframework.stereotype.Component;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.conversation.model.MessageEntity;

/** 消息工厂类，负责创建各类消息实体 */
@Component
public class MessageFactory {

    /** 创建用户消息实体
     *
     * @param content 消息内容
     * @param sessionId 会话ID
     * @return 用户消息实体 */
    public MessageEntity createUserMessage(String content, String sessionId) {
        MessageEntity userMessageEntity = new MessageEntity();
        userMessageEntity.setRole(Role.USER);
        userMessageEntity.setContent(content);
        userMessageEntity.setSessionId(sessionId);
        return userMessageEntity;
    }

    /** 创建系统消息实体(LLM回复消息)
     *
     * @param sessionId 会话ID
     * @param modelId 模型ID
     * @param providerId 提供商ID
     * @return 系统消息实体 */
    public MessageEntity createSystemMessage(String sessionId, String modelId, String providerId) {
        MessageEntity llmMessageEntity = new MessageEntity();
        llmMessageEntity.setRole(Role.SYSTEM);
        llmMessageEntity.setSessionId(sessionId);
        llmMessageEntity.setModel(modelId);
        llmMessageEntity.setProvider(providerId);
        return llmMessageEntity;
    }
}