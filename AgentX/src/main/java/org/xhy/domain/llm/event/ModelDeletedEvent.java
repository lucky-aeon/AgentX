package org.xhy.domain.llm.event;

/** 模型删除事件
 * 
 * @author xhy
 * @since 1.0.0 */
public class ModelDeletedEvent extends ModelDomainEvent {

    public ModelDeletedEvent(String modelId, String userId) {
        super(modelId, userId);
    }
}