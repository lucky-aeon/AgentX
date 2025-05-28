package org.xhy.application.conversation.service.message;

import org.springframework.stereotype.Service;
import org.xhy.infrastructure.transport.MessageTransport;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/** 流状态管理器 */

public class StreamStateManager {
    private static final Map<String, AtomicReference<StreamState>> streamStates = new ConcurrentHashMap<>();

    /** 流状态 */
    public static class StreamState {
        volatile boolean isActive;
        volatile Object connection;
        volatile boolean isCompleted;
        volatile StringBuilder partialContent;

        public StreamState(Object connection) {
            this.isActive = true;
            this.connection = connection;
            this.isCompleted = false;
            this.partialContent = new StringBuilder();
        }
    }

    /** 创建新的流状态
     *
     * @param sessionId 会话ID
     * @param connection 连接对象
     * @return 流状态 */
    public static StreamState createState(String sessionId, Object connection) {
        StreamState newState = new StreamState(connection);
        streamStates.put(sessionId, new AtomicReference<>(newState));
        return newState;
    }

    /** 获取流状态
     *
     * @param sessionId 会话ID
     * @return 流状态引用 */
    public static AtomicReference<StreamState> getStateRef(String sessionId) {
        return streamStates.get(sessionId);
    }

    /** 移除流状态
     *
     * @param sessionId 会话ID */
    public static void removeState(String sessionId) {
        streamStates.remove(sessionId);
    }

    /** 处理已存在的流
     *
     * @param sessionId 会话ID
     * @param transport 消息传输接口 */
    public static <T> void handleExistingStream(String sessionId, MessageTransport<T> transport) {
        AtomicReference<StreamState> stateRef = streamStates.get(sessionId);
        if (stateRef != null) {
            StreamState oldState = stateRef.get();
            if (oldState != null && oldState.isActive && !oldState.isCompleted) {
                oldState.isActive = false;
                if (oldState.connection != null) {
                    transport.completeConnection((T) oldState.connection);
                }
            }
        }
    }
}
