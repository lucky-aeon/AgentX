package org.xhy.domain.llm.model;

/**
 * 高可用选择结果
 * 
 * @author xhy
 * @since 1.0.0
 */
public class HighAvailabilityResult {

    /**
     * 选择的Provider
     */
    private ProviderEntity provider;

    /**
     * 选择的Model（可能有不同的部署名称）
     */
    private ModelEntity model;

    /**
     * 实例ID（用于结果上报）
     */
    private String instanceId;

    public HighAvailabilityResult() {
    }

    public HighAvailabilityResult(ProviderEntity provider, ModelEntity model, String instanceId) {
        this.provider = provider;
        this.model = model;
        this.instanceId = instanceId;
    }

    public ProviderEntity getProvider() {
        return provider;
    }

    public void setProvider(ProviderEntity provider) {
        this.provider = provider;
    }

    public ModelEntity getModel() {
        return model;
    }

    public void setModel(ModelEntity model) {
        this.model = model;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
} 