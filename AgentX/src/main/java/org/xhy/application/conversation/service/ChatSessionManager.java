package org.xhy.application.conversation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.infrastructure.transport.SseEmitterUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** 聊天会话管理器 负责管理正在进行的对话会话，支持会话中断功能 */
@Component
public class ChatSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(ChatSessionManager.class);

    /** 会话信息 */
    public static class SessionInfo {
        private final String sessionId;
        private final SseEmitter emitter;
        private final AtomicBoolean interrupted;
        private final long startTime;

        public SessionInfo(String sessionId, SseEmitter emitter) {
            this.sessionId = sessionId;
            this.emitter = emitter;
            this.interrupted = new AtomicBoolean(false);
            this.startTime = System.currentTimeMillis();
        }

        public String getSessionId() {
            return sessionId;
        }

        public SseEmitter getEmitter() {
            return emitter;
        }

        public boolean isInterrupted() {
            return interrupted.get();
        }

        public void setInterrupted() {
            interrupted.set(true);
        }

        public long getStartTime() {
            return startTime;
        }
    }

    // 使用sessionId作为key，存储正在进行的对话会话
    private final ConcurrentHashMap<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    /** 注册一个新的对话会话
     * @param sessionId 会话ID
     * @param emitter SSE发送器 */
    public void registerSession(String sessionId, SseEmitter emitter) {
        SessionInfo sessionInfo = new SessionInfo(sessionId, emitter);
        activeSessions.put(sessionId, sessionInfo);
        logger.info("注册对话会话: sessionId={}", sessionId);

        // 设置SSE完成和超时回调，自动清理会话
        emitter.onCompletion(() -> {
            removeSession(sessionId);
            logger.info("对话会话完成: sessionId={}", sessionId);
        });

        emitter.onTimeout(() -> {
            removeSession(sessionId);
            logger.warn("对话会话超时: sessionId={}", sessionId);
        });

        emitter.onError((throwable) -> {
            removeSession(sessionId);
            logger.error("对话会话错误: sessionId={}, error={}", sessionId, throwable.getMessage());
        });
    }

    /** 移除对话会话
     * @param sessionId 会话ID */
    public void removeSession(String sessionId) {
        SessionInfo removed = activeSessions.remove(sessionId);
        if (removed != null) {
            long duration = System.currentTimeMillis() - removed.getStartTime();
            logger.info("移除对话会话: sessionId={}, 持续时间={}ms", sessionId, duration);
        }
    }

    /** 中断指定的对话会话
     * @param sessionId 会话ID
     * @return 是否成功中断（true表示会话存在且成功中断，false表示会话不存在） */
    public boolean interruptSession(String sessionId) {
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if (sessionInfo == null) {
            logger.warn("尝试中断不存在的会话: sessionId={}", sessionId);
            return false;
        }

        // 设置中断标志
        sessionInfo.setInterrupted();
        logger.info("设置会话中断标志: sessionId={}", sessionId);

        // 先从活跃会话中移除，避免重复处理
        activeSessions.remove(sessionId);

        try {
            SseEmitter emitter = sessionInfo.getEmitter();

            // 检查emitter是否已经完成
            if (SseEmitterUtils.isEmitterCompleted(emitter)) {
                logger.info("SSE连接已完成，无需再次发送中断消息: sessionId={}", sessionId);
                return true;
            }

            // 尝试发送中断消息给前端
            SseEmitterUtils.safeSend(emitter,
                    SseEmitter.event().name("interrupt").data("{\"interrupted\": true, \"message\": \"对话已被中断\"}"));

            // 安全完成SSE连接
            SseEmitterUtils.safeComplete(emitter);
            logger.info("对话会话已中断: sessionId={}", sessionId);
            return true;

        } catch (Exception e) {
            logger.error("中断会话时发生错误: sessionId={}, error={}", sessionId, e.getMessage());
            return true;
        }
    }

    /** 检查会话是否已被中断
     * @param sessionId 会话ID
     * @return true表示已中断，false表示未中断或会话不存在 */
    public boolean isSessionInterrupted(String sessionId) {
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        return sessionInfo != null && sessionInfo.isInterrupted();
    }

    /** 获取当前活跃会话数量
     * @return 活跃会话数量 */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /** 检查会话是否存在
     * @param sessionId 会话ID
     * @return 会话是否存在 */
    public boolean hasSession(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }
}