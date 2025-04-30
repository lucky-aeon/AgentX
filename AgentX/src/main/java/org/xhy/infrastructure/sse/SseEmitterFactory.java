package org.xhy.infrastructure.sse;

import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.dto.AgentChatResponse;

/** SSE发射器工厂 负责创建和配置Server-Sent Events发射器 */
@Component
public class SseEmitterFactory {

    /** 创建带有超时和错误处理的SSE发射器
     *
     * @param timeoutMillis 超时时间(毫秒)
     * @return 配置好的SSE发射器 */
    public SseEmitter createEmitter(long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);

        // 添加超时回调
        emitter.onTimeout(() -> {
            try {
                AgentChatResponse response = new AgentChatResponse();
                response.setContent("\n\n[系统提示：响应超时，请重试]");
                response.setDone(true);
                emitter.send(response);
                emitter.complete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // 添加错误回调
        emitter.onError((ex) -> {
            try {
                AgentChatResponse response = new AgentChatResponse();
                response.setContent("\n\n[系统错误：" + ex.getMessage() + "]");
                response.setDone(true);
                emitter.send(response);
                emitter.complete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return emitter;
    }

    /** 发送部分响应
     *
     * @param emitter SSE发射器
     * @param content 响应内容
     * @param providerName 提供商名称
     * @param modelId 模型ID */
    public void sendPartialResponse(SseEmitter emitter, String content, String providerName, String modelId) {
        try {
            AgentChatResponse response = new AgentChatResponse();
            response.setContent(content);
            response.setDone(false);
            emitter.send(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** 发送完成响应
     *
     * @param emitter SSE发射器
     * @param providerName 提供商名称
     * @param modelId 模型ID */
    public void sendCompleteResponse(SseEmitter emitter, String providerName, String modelId) {
        try {
            AgentChatResponse response = new AgentChatResponse();
            response.setContent("");
            response.setDone(true);
            emitter.send(response);
            emitter.complete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** 发送错误响应
     *
     * @param emitter SSE发射器
     * @param error 错误信息 */
    public void sendErrorResponse(SseEmitter emitter, String error) {
        try {
            AgentChatResponse response = new AgentChatResponse();
            response.setContent(error);
            response.setDone(true);
            emitter.send(response);
            emitter.complete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
