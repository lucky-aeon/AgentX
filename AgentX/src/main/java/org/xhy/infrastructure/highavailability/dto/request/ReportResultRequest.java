package org.xhy.infrastructure.highavailability.dto.request;

import java.util.Map;

/** 上报调用结果请求
 * 
 * @author xhy
 * @since 1.0.0 */
public class ReportResultRequest {

    /** 用户ID，可选 */
    private String userId;

    /** API实例ID，必填 */
    private String instanceId;

    /** 业务ID，必填 */
    private String businessId;

    /** 调用是否成功，必填 */
    private Boolean success;

    /** 调用延迟（毫秒），必填 */
    private Long latencyMs;

    /** 错误信息，失败时可选 */
    private String errorMessage;

    /** 错误类型，失败时可选 */
    private String errorType;

    /** 使用指标，可选 */
    private Map<String, Object> usageMetrics;

    /** 调用时间戳，必填 */
    private Long callTimestamp;

    public ReportResultRequest() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public Map<String, Object> getUsageMetrics() {
        return usageMetrics;
    }

    public void setUsageMetrics(Map<String, Object> usageMetrics) {
        this.usageMetrics = usageMetrics;
    }

    public Long getCallTimestamp() {
        return callTimestamp;
    }

    public void setCallTimestamp(Long callTimestamp) {
        this.callTimestamp = callTimestamp;
    }
}