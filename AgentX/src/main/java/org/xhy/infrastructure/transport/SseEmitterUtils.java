package org.xhy.infrastructure.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** SseEmitter工具类 提供安全的SSE连接操作方法，避免"ResponseBodyEmitter has already completed"错误 */
public class SseEmitterUtils {

    private static final Logger logger = LoggerFactory.getLogger(SseEmitterUtils.class);

    /** 检查SseEmitter是否已完成
     * @param emitter SSE发送器
     * @return true表示已完成，false表示未完成 */
    public static boolean isEmitterCompleted(SseEmitter emitter) {
        if (emitter == null) {
            return true;
        }

        try {
            // 使用反射检查ResponseBodyEmitter的内部状态
            java.lang.reflect.Field completeField = emitter.getClass().getSuperclass().getDeclaredField("complete");
            completeField.setAccessible(true);
            return (Boolean) completeField.get(emitter);
        } catch (Exception e) {
            // 如果反射失败，采用保守策略，认为未完成
            logger.trace("无法通过反射检查SSE状态，假设未完成: {}", e.getMessage());
            return false;
        }
    }

    /** 安全发送消息，避免向已完成的连接发送
     * @param emitter SSE发送器
     * @param data 要发送的数据
     * @return 是否成功发送 */
    public static boolean safeSend(SseEmitter emitter, Object data) {
        if (emitter == null) {
            logger.debug("SSE连接为null，跳过发送");
            return false;
        }

        try {
            if (isEmitterCompleted(emitter)) {
                logger.debug("SSE连接已完成，跳过消息发送");
                return false;
            }

            emitter.send(data);
            return true;
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("already completed")) {
                logger.debug("SSE连接已完成，忽略发送操作: {}", e.getMessage());
                return false;
            } else {
                logger.warn("发送SSE消息时发生IllegalStateException: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            logger.error("发送SSE消息失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /** 安全完成SSE连接，避免重复完成
     * @param emitter SSE发送器
     * @return 是否成功完成 */
    public static boolean safeComplete(SseEmitter emitter) {
        if (emitter == null) {
            logger.debug("SSE连接为null，跳过完成操作");
            return false;
        }

        try {
            if (isEmitterCompleted(emitter)) {
                logger.debug("SSE连接已完成，跳过完成操作");
                return false;
            }

            emitter.complete();
            return true;
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("already completed")) {
                logger.debug("SSE连接已完成，忽略完成操作: {}", e.getMessage());
                return false;
            } else {
                logger.warn("完成SSE连接时发生异常: {}", e.getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.warn("完成SSE连接时发生未知异常: {}", e.getMessage());
            return false;
        }
    }

    /** 检查连接是否活跃（未完成且未出错）
     * @param emitter SSE发送器
     * @return 是否活跃 */
    public static boolean isEmitterActive(SseEmitter emitter) {
        return emitter != null && !isEmitterCompleted(emitter);
    }
}