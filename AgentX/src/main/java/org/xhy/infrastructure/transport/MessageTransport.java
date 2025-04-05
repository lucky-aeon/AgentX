package org.xhy.infrastructure.transport;

/**
 * 消息传输接口，用于抽象不同的消息传输方式(SSE、WebSocket等)
 */
public interface MessageTransport<T> {
    /**
     * 创建连接
     * @param timeout 超时时间(毫秒)
     * @return 连接对象
     */
    T createConnection(long timeout);
    
    /**
     * 发送消息
     * @param connection 连接对象
     * @param content 消息内容
     * @param isDone 是否完成
     * @param provider 服务商名称
     * @param model 模型名称
     */
    void sendMessage(T connection, String content, boolean isDone, String provider, String model);
    
    /**
     * 完成连接
     * @param connection 连接对象
     */
    void completeConnection(T connection);
    
    /**
     * 处理错误
     * @param connection 连接对象
     * @param error 错误对象
     */
    void handleError(T connection, Throwable error);
} 