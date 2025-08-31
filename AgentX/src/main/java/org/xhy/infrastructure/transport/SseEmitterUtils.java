package org.xhy.infrastructure.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/** SseEmitter工具类 - 极简版本，提供基本的安全发送功能 */
public class SseEmitterUtils {

    private static final Logger logger = LoggerFactory.getLogger(SseEmitterUtils.class);

    /** 安全发送消息，优雅处理连接异常
     * @param emitter SSE发送器
     * @param data 要发送的数据
     * @return 是否成功发送 */
    public static boolean safeSend(SseEmitter emitter, Object data) {
        if (emitter == null) {
            logger.debug("SSE连接为null，跳过发送");
            return false;
        }

        try {
            emitter.send(data);
            return true;
        } catch (IllegalStateException e) {
            // 连接已关闭，这是正常情况
            logger.debug("SSE连接已关闭，跳过消息发送: {}", e.getMessage());
            return false;
        } catch (IOException e) {
            // 网络问题，这也是正常情况
            logger.debug("SSE网络异常，跳过消息发送: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            // 其他异常，记录但不抛出
            logger.debug("SSE消息发送异常: {}", e.getMessage());
            return false;
        }
    }

    /** 安全完成SSE连接
     * @param emitter SSE发送器
     * @return 是否成功完成 */
    public static boolean safeComplete(SseEmitter emitter) {
        if (emitter == null) {
            logger.debug("SSE连接为null，跳过完成操作");
            return false;
        }

        try {
            emitter.complete();
            return true;
        } catch (IllegalStateException e) {
            // 连接已关闭，正常情况
            logger.debug("SSE连接已完成或已关闭: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            // 其他异常，记录但不抛出
            logger.debug("完成SSE连接时异常: {}", e.getMessage());
            return false;
        }
    }
}