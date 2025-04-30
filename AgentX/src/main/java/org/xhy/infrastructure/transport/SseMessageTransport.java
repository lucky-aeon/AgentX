package org.xhy.infrastructure.transport;

import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.dto.AgentChatResponse;

/** SSE消息传输实现 */
@Component
public class SseMessageTransport implements MessageTransport<SseEmitter> {

    /** 系统超时消息 */
    private static final String TIMEOUT_MESSAGE = "\n\n[系统提示：响应超时，请重试]";

    /** 系统错误消息前缀 */
    private static final String ERROR_MESSAGE_PREFIX = "\n\n[系统错误：";

    @Override
    public SseEmitter createConnection(long timeout) {
        SseEmitter emitter = new SseEmitter(timeout);

        // 添加超时回调
        emitter.onTimeout(() -> {
            try {
                AgentChatResponse response = new AgentChatResponse();
                response.setContent(TIMEOUT_MESSAGE);
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
                response.setContent(ERROR_MESSAGE_PREFIX + ex.getMessage() + "]");
                response.setDone(true);
                emitter.send(response);
                emitter.complete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return emitter;
    }

    @Override
    public void sendMessage(SseEmitter connection, AgentChatResponse streamChatResponse) {
        try {

            connection.send(streamChatResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendEndMessage(SseEmitter connection, AgentChatResponse streamChatResponse) {
        try {

            connection.send(streamChatResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            connection.complete();
        }
    }

    @Override
    public void completeConnection(SseEmitter connection) {
        connection.complete();
    }

    @Override
    public void handleError(SseEmitter connection, Throwable error) {
        try {
            AgentChatResponse response = new AgentChatResponse();
            response.setContent(error.getMessage());
            response.setDone(true);
            connection.send(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            connection.complete();
        }
    }
}
