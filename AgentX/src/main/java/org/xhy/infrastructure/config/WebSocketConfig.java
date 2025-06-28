package org.xhy.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.xhy.infrastructure.terminal.TerminalWebSocketHandler;

/** WebSocket配置 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);
    private final TerminalWebSocketHandler terminalWebSocketHandler;

    public WebSocketConfig(TerminalWebSocketHandler terminalWebSocketHandler) {
        this.terminalWebSocketHandler = terminalWebSocketHandler;
        logger.info("WebSocket配置初始化完成");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        logger.info("注册WebSocket处理器: /ws/terminal");
        registry.addHandler(terminalWebSocketHandler, "/ws/terminal")
                .setAllowedOriginPatterns("*"); // 开发环境允许所有来源，生产环境应该限制
        logger.info("WebSocket处理器注册完成");
    }
}