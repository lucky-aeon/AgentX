package org.xhy.infrastructure.transport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/** 消息传输工厂，用于获取不同类型的消息传输实现 */
@Component
public class MessageTransportFactory {

    /** 传输类型常量 */
    public static final String TRANSPORT_TYPE_SSE = "sse";
    public static final String TRANSPORT_TYPE_WEBSOCKET = "websocket";
    public static final String TRANSPORT_TYPE_SYNC = "sync";

    private final Map<String, MessageTransport<?>> transports = new HashMap<>();

    @Autowired
    public MessageTransportFactory(SseMessageTransport sseTransport, SyncMessageTransport syncTransport) {
        transports.put(TRANSPORT_TYPE_SSE, sseTransport);
        transports.put(TRANSPORT_TYPE_SYNC, syncTransport);
        // 将来可以添加WebSocket实现
        // transports.put(TRANSPORT_TYPE_WEBSOCKET, webSocketTransport);
    }

    /** 获取指定类型的消息传输实现
     * @param type 传输类型
     * @return 消息传输实现 */
    @SuppressWarnings("unchecked")
    public <T> MessageTransport<T> getTransport(String type) {
        return (MessageTransport<T>) transports.getOrDefault(type, transports.get(TRANSPORT_TYPE_SSE));
    }
}