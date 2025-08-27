package org.xhy.application.knowledgeGraph.dto;

import java.time.LocalDateTime;

/**
 * 图数据摄取响应DTO
 * 用于返回数据摄取操作的结果信息
 * 
 * @author zang
 */
public class GraphIngestionResponse {

    /**
     * 操作是否成功
     */
    private boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 文档ID
     */
    private String documentId;

    /**
     * 成功处理的实体数量
     */
    private int entitiesProcessed;

    /**
     * 成功处理的关系数量
     */
    private int relationshipsProcessed;

    /**
     * 处理开始时间
     */
    private LocalDateTime processedAt;

    /**
     * 处理耗时（毫秒）
     */
    private long processingTimeMs;

    public GraphIngestionResponse() {
        this.processedAt = LocalDateTime.now();
    }

    public GraphIngestionResponse(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public GraphIngestionResponse(boolean success, String message, String documentId, 
                                int entitiesProcessed, int relationshipsProcessed) {
        this(success, message);
        this.documentId = documentId;
        this.entitiesProcessed = entitiesProcessed;
        this.relationshipsProcessed = relationshipsProcessed;
    }

    /**
     * 创建成功响应（带自定义消息）
     */
    public static GraphIngestionResponse success(String documentId, int entitiesProcessed, int relationshipsProcessed, String message) {
        GraphIngestionResponse response = new GraphIngestionResponse(true, message, 
                documentId, entitiesProcessed, relationshipsProcessed);
        return response;
    }

    /**
     * 创建成功响应
     */
    public static GraphIngestionResponse success(String documentId, int entitiesProcessed, int relationshipsProcessed) {
        GraphIngestionResponse response = new GraphIngestionResponse(true, "图数据摄取成功", 
                documentId, entitiesProcessed, relationshipsProcessed);
        return response;
    }

    /**
     * 创建失败响应
     */
    public static GraphIngestionResponse failure(String message) {
        return new GraphIngestionResponse(false, message);
    }

    /**
     * 创建失败响应（带文档ID）
     */
    public static GraphIngestionResponse failure(String documentId, String message) {
        GraphIngestionResponse response = new GraphIngestionResponse(false, message);
        response.setDocumentId(documentId);
        return response;
    }

    /**
     * 创建部分处理响应（用于分页处理中间状态）
     */
    public static GraphIngestionResponse partial(String documentId, int entitiesProcessed, int relationshipsProcessed, String message) {
        GraphIngestionResponse response = new GraphIngestionResponse(true, message, 
                documentId, entitiesProcessed, relationshipsProcessed);
        return response;
    }

    /**
     * 创建错误响应
     */
    public static GraphIngestionResponse error(String documentId, String message) {
        GraphIngestionResponse response = new GraphIngestionResponse(false, message);
        response.setDocumentId(documentId);
        return response;
    }

    /**
     * 设置处理耗时
     */
    public void setProcessingTime(long startTimeMs) {
        this.processingTimeMs = System.currentTimeMillis() - startTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public int getEntitiesProcessed() {
        return entitiesProcessed;
    }

    public void setEntitiesProcessed(int entitiesProcessed) {
        this.entitiesProcessed = entitiesProcessed;
    }

    public int getRelationshipsProcessed() {
        return relationshipsProcessed;
    }

    public void setRelationshipsProcessed(int relationshipsProcessed) {
        this.relationshipsProcessed = relationshipsProcessed;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    @Override
    public String toString() {
        return "GraphIngestionResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", documentId='" + documentId + '\'' +
                ", entitiesProcessed=" + entitiesProcessed +
                ", relationshipsProcessed=" + relationshipsProcessed +
                ", processedAt=" + processedAt +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
}