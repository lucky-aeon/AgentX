package org.xhy.domain.llm.event;

import org.xhy.domain.llm.model.ModelEntity;

/** 模型状态变更事件 当模型状态发生变更时触发
 * 
 * @author xhy
 * @since 1.0.0 */
public class ModelStatusChangedEvent extends ModelDomainEvent {

    /** 变更后的模型实体 */
    private final ModelEntity model;

    /** 新状态，true=启用，false=禁用 */
    private final boolean enabled;

    /** 状态变更原因 */
    private final String reason;

    public ModelStatusChangedEvent(String modelId, String userId, ModelEntity model, boolean enabled, String reason) {
        super(modelId, userId);
        this.model = model;
        this.enabled = enabled;
        this.reason = reason;
    }

    public ModelStatusChangedEvent(String modelId, String userId, ModelEntity model, boolean enabled) {
        this(modelId, userId, model, enabled, null);
    }

    public ModelEntity getModel() {
        return model;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getReason() {
        return reason;
    }

    /** 是否为启用事件 */
    public boolean isActivation() {
        return enabled;
    }

    /** 是否为禁用事件 */
    public boolean isDeactivation() {
        return !enabled;
    }
}