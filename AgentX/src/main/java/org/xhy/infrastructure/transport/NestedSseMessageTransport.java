package org.xhy.infrastructure.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.domain.conversation.constant.MessageType;

import java.io.IOException;

/**
 * 嵌套流式传输器：
 * - 复用现有 SseEmitter，将子Agent的流式输出转译为 SUB_AGENT_* 事件，
 * - 不会关闭连接，结束时仅发送 COMPLETE 事件。
 */
public class NestedSseMessageTransport implements MessageTransport<SseEmitter> {

    private static final Logger logger = LoggerFactory.getLogger(NestedSseMessageTransport.class);

    private final SseEmitter emitter;
    private final String subAgentName;
    private final StringBuilder buffer = new StringBuilder();
    private final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);

    public NestedSseMessageTransport(SseEmitter emitter, String subAgentName) {
        this.emitter = emitter;
        this.subAgentName = subAgentName;
    }

    @Override
    public SseEmitter createConnection(long timeout) {
        // 直接复用外部传入的 emitter
        return emitter;
    }

    @Override
    public void sendMessage(SseEmitter connection, AgentChatResponse streamChatResponse) {
        AgentChatResponse resp = new AgentChatResponse();
        resp.setTimestamp(streamChatResponse.getTimestamp());
        resp.setContent(streamChatResponse.getContent());
        resp.setDone(false);
        // 将普通文本转为子Agent片段
        MessageType type = streamChatResponse.getMessageType();
        if (type == null || type == MessageType.TEXT) {
            resp.setMessageType(MessageType.SUB_AGENT_PARTIAL);
        } else {
            resp.setMessageType(type);
        }
        if (streamChatResponse.getContent() != null) {
            buffer.append(streamChatResponse.getContent());
        }
        safeSend(connection, resp);
    }

    @Override
    public void sendEndMessage(SseEmitter connection, AgentChatResponse streamChatResponse) {
        AgentChatResponse resp = new AgentChatResponse();
        resp.setTimestamp(streamChatResponse.getTimestamp());
        resp.setContent(streamChatResponse.getContent());
        resp.setDone(true);
        resp.setMessageType(MessageType.SUB_AGENT_COMPLETE);
        if (streamChatResponse.getContent() != null) {
            buffer.append(streamChatResponse.getContent());
        }
        safeSend(connection, resp);
        // 不关闭连接
        done.countDown();
    }

    @Override
    public void completeConnection(SseEmitter connection) {
        // 嵌套流不结束外层连接
    }

    @Override
    public void handleError(SseEmitter connection, Throwable error) {
        AgentChatResponse resp = new AgentChatResponse();
        resp.setContent(error != null ? error.getMessage() : "子Agent调用异常");
        resp.setDone(true);
        resp.setMessageType(MessageType.SUB_AGENT_ERROR);
        safeSend(connection, resp);
        done.countDown();
    }

    private void safeSend(SseEmitter emitter, AgentChatResponse response) {
        try {
            emitter.send(response);
        } catch (IllegalStateException e) {
            logger.debug("SSE连接已关闭，跳过子Agent消息发送: {}", e.getMessage());
        } catch (IOException e) {
            logger.debug("SSE网络异常，跳过子Agent消息发送: {}", e.getMessage());
        } catch (Exception e) {
            logger.debug("SSE子Agent消息发送异常: {}", e.getMessage());
        }
    }

    /** 等待子流程完成 */
    public boolean awaitCompletion(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis <= 0) {
            done.await();
            return true;
        }
        return done.await(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /** 获取聚合文本 */
    public String getAggregatedText() {
        return buffer.toString();
    }
}
