
package org.xhy.application.conversation.service.message;

import org.springframework.stereotype.Service;
import org.xhy.infrastructure.transport.MessageTransport;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 流状态管理器 */

public class StreamStateManager {
    private static final Map<String, StreamState> streamStates = new ConcurrentHashMap<>();

    /** 流状态 */
    public static class StreamState {
        private volatile boolean isActive;
        private volatile Object connection;
        private volatile boolean isCompleted;
        private volatile StringBuilder partialContent;

        public StreamState(Object connection) {
            this.isActive = true;
            this.connection = connection;
            this.isCompleted = false;
            this.partialContent = new StringBuilder();
        }

        public boolean isActive() {
            return isActive;
        }

        public void setActive(boolean active) {
            isActive = active;
        }

        public Object getConnection() {
            return connection;
        }

        public void setConnection(Object connection) {
            this.connection = connection;
        }

        public boolean isCompleted() {
            return isCompleted;
        }

        public void setCompleted(boolean completed) {
            isCompleted = completed;
        }

        public StringBuilder getPartialContent() {
            return partialContent;
        }

    }

    /** 创建新的流状态
     *
     * @param sessionId 会话ID
     * @param connection 连接对象
     * @return 流状态 */
    public static StreamState createState(String sessionId, Object connection) {
        StreamState newState = new StreamState(connection);
        streamStates.put(sessionId, newState);
        return newState;
    }

    /** 获取流状态
     *
     * @param sessionId 会话ID
     * @return 流状态 */
    public static StreamState getState(String sessionId) {
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
        StreamState oldState = streamStates.get(sessionId);
        if (oldState != null && oldState.isActive && !oldState.isCompleted) {
            oldState.isActive = false;
            if (oldState.connection != null) {
                transport.completeConnection((T) oldState.connection);
                System.out.println("旧连接已被清理");
            }
        }
    }
}
