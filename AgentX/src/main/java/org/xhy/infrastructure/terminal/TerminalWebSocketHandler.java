package org.xhy.infrastructure.terminal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.net.URI;

/** 终端WebSocket处理器 */
@Component
public class TerminalWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(TerminalWebSocketHandler.class);

    private final WebTerminalService webTerminalService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TerminalWebSocketHandler(WebTerminalService webTerminalService) {
        this.webTerminalService = webTerminalService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket连接建立: {}", session.getId());

        // 从URL参数中获取容器ID
        URI uri = session.getUri();
        if (uri != null && uri.getQuery() != null) {
            String[] params = uri.getQuery().split("&");
            String containerId = null;

            for (String param : params) {
                if (param.startsWith("containerId=")) {
                    containerId = param.substring("containerId=".length());
                    break;
                }
            }

            if (containerId != null) {
                // 创建终端会话
                boolean success = webTerminalService.createTerminalSession(session.getId(), containerId, session);
                if (!success) {
                    session.close(CloseStatus.SERVER_ERROR.withReason("无法创建终端会话"));
                }
            } else {
                session.close(CloseStatus.BAD_DATA.withReason("缺少容器ID参数"));
            }
        } else {
            session.close(CloseStatus.BAD_DATA.withReason("无效的连接参数"));
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            String payload = textMessage.getPayload();

            try {
                // 解析JSON消息
                JsonNode jsonNode = objectMapper.readTree(payload);
                String type = jsonNode.get("type").asText();

                if ("input".equals(type)) {
                    String input = jsonNode.get("data").asText();
                    webTerminalService.sendCommand(session.getId(), input);
                } else if ("resize".equals(type)) {
                    // 处理终端大小调整（可选实现）
                    logger.debug("终端大小调整: {}", payload);
                }
            } catch (Exception e) {
                // 如果不是JSON格式，直接作为命令发送
                webTerminalService.sendCommand(session.getId(), payload);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket传输错误: {}", session.getId(), exception);
        webTerminalService.closeTerminalSession(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.info("WebSocket连接关闭: {} - {}", session.getId(), closeStatus);
        webTerminalService.closeTerminalSession(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}