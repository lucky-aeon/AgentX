package org.xhy.application.conversation.service.handler;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.message.AbstractMessageHandler;
import org.xhy.domain.agent.model.AgentEntity;

/** 消息处理器类型枚举 */
enum MessageHandlerType {
    STANDARD, AGENT
}

/** 消息处理器工厂 根据智能体类型选择适合的消息处理器 */
@Component
public class MessageHandlerFactory {

    private final ApplicationContext applicationContext;

    public MessageHandlerFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /** 根据智能体获取合适的消息处理器
     * 
     * @param agent 智能体实体
     * @return 消息处理器 */
    public AbstractMessageHandler getHandler(AgentEntity agent) {
        // 统一使用标准消息处理器
        return getHandlerByType(MessageHandlerType.STANDARD);
    }

    /** 根据处理器类型获取对应的处理器实例
     * 
     * @param type 处理器类型
     * @return 消息处理器 */
    private AbstractMessageHandler getHandlerByType(MessageHandlerType type) {
        switch (type) {
            case AGENT :
                return applicationContext.getBean("agentMessageHandler", AbstractMessageHandler.class);
            case STANDARD :
            default :
                return applicationContext.getBean("chatMessageHandler", AbstractMessageHandler.class);
        }
    }
}