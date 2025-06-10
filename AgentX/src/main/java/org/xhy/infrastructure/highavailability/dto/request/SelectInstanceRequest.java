package org.xhy.infrastructure.highavailability.dto.request;

import java.util.List;

/** 选择API实例请求
 * 
 * @author xhy
 * @since 1.0.0 */
public class SelectInstanceRequest {

    /** 用户ID，可选 */
    private String userId;

    /** API标识符，必填 */
    private String apiIdentifier;

    /** API类型，必填 */
    private String apiType;

    /** 亲和性键，用于会话绑定，可选 */
    private String affinityKey;

    /** 亲和性类型，可选
     * 定义亲和性的类型，例如：SESSION、USER、BATCH、REGION等 */
    private String affinityType;

    /** 降级链，按优先级排序的模型ID列表 */
    private List<String> fallbackChain;

    public SelectInstanceRequest() {
    }

    public SelectInstanceRequest(String userId, String apiIdentifier, String apiType) {
        this.userId = userId;
        this.apiIdentifier = apiIdentifier;
        this.apiType = apiType;
    }

    public SelectInstanceRequest(String userId, String apiIdentifier, String apiType, String affinityKey, String affinityType) {
        this.userId = userId;
        this.apiIdentifier = apiIdentifier;
        this.apiType = apiType;
        this.affinityKey = affinityKey;
        this.affinityType = affinityType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getApiIdentifier() {
        return apiIdentifier;
    }

    public void setApiIdentifier(String apiIdentifier) {
        this.apiIdentifier = apiIdentifier;
    }

    public String getApiType() {
        return apiType;
    }

    public void setApiType(String apiType) {
        this.apiType = apiType;
    }

    public String getAffinityKey() {
        return affinityKey;
    }

    public void setAffinityKey(String affinityKey) {
        this.affinityKey = affinityKey;
    }

    public String getAffinityType() {
        return affinityType;
    }

    public void setAffinityType(String affinityType) {
        this.affinityType = affinityType;
    }

    public List<String> getFallbackChain() {
        return fallbackChain;
    }

    public void setFallbackChain(List<String> fallbackChain) {
        this.fallbackChain = fallbackChain;
    }
}