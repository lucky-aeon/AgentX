package org.xhy.application.conversation.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.service.ConversationAppService;
import org.xhy.domain.scheduledtask.event.ScheduledTaskExecuteEvent;

/** 定时任务事件监听器 监听Domain层发布的任务执行事件，调用ConversationAppService执行对话 */
@Component
public class ConversationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ConversationEventListener.class);

    private final ConversationAppService conversationAppService;

    public ConversationEventListener(ConversationAppService conversationAppService) {
        this.conversationAppService = conversationAppService;
    }

    /** 处理定时任务执行事件
     * @param event 任务执行事件 */
    @EventListener
    @Async
    public void chatEvent(ScheduledTaskExecuteEvent event) {
        try {
            logger.info("接收到定时任务执行事件: taskId={}, userId={}, sessionId={}", event.getTaskId(), event.getUserId(),
                    event.getSessionId());

            // 创建聊天请求
            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setMessage(event.getContent());
            chatRequest.setSessionId(event.getSessionId());

            // 调用对话服务
            conversationAppService.chat(chatRequest, event.getUserId());

            logger.info("定时任务消息发送成功: taskId={}", event.getTaskId());

        } catch (Exception e) {
            logger.error("处理定时任务执行事件失败: taskId={}, error={}", event.getTaskId(), e.getMessage(), e);

        }
    }
}