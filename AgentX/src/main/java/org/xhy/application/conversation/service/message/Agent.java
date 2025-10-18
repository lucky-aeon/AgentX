package org.xhy.application.conversation.service.message;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;

public interface Agent {
    /** 流式对话（异步） */
    TokenStream chat(String message);

    /** 同步对话（阻塞直到完成），返回完整响应信息 */
    Response<AiMessage> chatSync(String message);
}
