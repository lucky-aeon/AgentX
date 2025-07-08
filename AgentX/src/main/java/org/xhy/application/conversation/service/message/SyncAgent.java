package org.xhy.application.conversation.service.message;

import dev.langchain4j.model.chat.response.ChatResponse;

/** 同步聊天Agent接口 */
public interface SyncAgent {

    /** 同步聊天方法
     * @param userMessage 用户消息
     * @return 聊天响应 */
    String chat(String userMessage);
}