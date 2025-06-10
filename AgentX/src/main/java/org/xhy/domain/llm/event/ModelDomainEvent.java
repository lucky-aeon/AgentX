package org.xhy.domain.llm.event;

import java.time.LocalDateTime;

/**
 * 模型领域事件基类
 * 
 * @author xhy
 * @since 1.0.0
 */
public abstract class ModelDomainEvent {

    /**
     * 模型ID
     */
    private final String modelId;

    /**
     * 用户ID
     */
    private final String userId;

    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredAt;

    public ModelDomainEvent(String modelId, String userId) {
        this.modelId = modelId;
        this.userId = userId;
        this.occurredAt = LocalDateTime.now();
    }

    public String getModelId() {
        return modelId;
    }

    public String getUserId() {
        return userId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
} 