package org.xhy.infrastructure.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.dto.AgentChatResponse;

import java.io.IOException;

/** SSE消息传输实现 */
@Component
public class SseMessageTransport implements MessageTransport<SseEmitter> {

    private static final Logger logger = LoggerFactory.getLogger(SseMessageTransport.class);

    /** 系统超时消息 */
    private static final String TIMEOUT_MESSAGE = "\n\n[系统提示：响应超时，请重试]";

    /** 系统错误消息前缀 */
    private static final String ERROR_MESSAGE_PREFIX = "\n\n[系统错误：";

    @Override
    public SseEmitter createConnection(long timeout) {
        SseEmitter emitter = new SseEmitter(timeout);

        // 添加简单的生命周期回调
        emitter.onCompletion(() -> {
            logger.debug("SSE连接正常完成");
        });
        
        // 添加超时回调
        emitter.onTimeout(() -> {
            logger.debug("SSE连接超时");
            try {
                AgentChatResponse response = new AgentChatResponse();
                response.setContent(TIMEOUT_MESSAGE);
                response.setDone(true);
                safeSendMessage(emitter, response);
            } finally {
                safeCompleteEmitter(emitter);
            }
        });

        // 添加错误回调
        emitter.onError((ex) -> {
            logger.debug("SSE连接发生错误: {}", ex.getMessage());
            try {
                AgentChatResponse response = new AgentChatResponse();
                response.setContent(ERROR_MESSAGE_PREFIX + ex.getMessage() + "]");
                response.setDone(true);
                safeSendMessage(emitter, response);
            } finally {
                safeCompleteEmitter(emitter);
            }
        });

        return emitter;
    }

    @Override
    public void sendMessage(SseEmitter connection, AgentChatResponse streamChatResponse) {
        safeSendMessage(connection, streamChatResponse);
    }

    @Override
    public void sendEndMessage(SseEmitter connection, AgentChatResponse streamChatResponse) {
        try {
            safeSendMessage(connection, streamChatResponse);
        } finally {
            safeCompleteEmitter(connection);
        }
    }

    @Override
    public void completeConnection(SseEmitter connection) {
        safeCompleteEmitter(connection);
    }

    @Override
    public void handleError(SseEmitter connection, Throwable error) {
        try {
            AgentChatResponse response = new AgentChatResponse();
            response.setContent(error.getMessage());
            response.setDone(true);
            safeSendMessage(connection, response);
        } finally {
            safeCompleteEmitter(connection);
        }
    }

    /** 安全发送消息，直接处理网络异常
     * @param emitter SSE发送器
     * @param response 响应消息 */
    private void safeSendMessage(SseEmitter emitter, AgentChatResponse response) {
        try {
            emitter.send(response);
        } catch (IllegalStateException e) {
            // 连接已关闭，这是正常情况
            logger.debug("SSE连接已关闭，跳过消息发送: {}", e.getMessage());
        } catch (IOException e) {
            // 网络问题，这也是正常情况
            logger.debug("SSE网络异常，跳过消息发送: {}", e.getMessage());
        } catch (Exception e) {
            // 其他异常，记录但不抛出
            logger.debug("SSE消息发送异常: {}", e.getMessage());
        }
    }

    /** 安全完成SSE连接
     * @param emitter SSE发送器 */
    private void safeCompleteEmitter(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException e) {
            // 连接已关闭，正常情况
            logger.debug("SSE连接已完成或已关闭: {}", e.getMessage());
        } catch (Exception e) {
            // 其他异常，记录但不抛出
            logger.debug("完成SSE连接时异常: {}", e.getMessage());
        }
    }
}