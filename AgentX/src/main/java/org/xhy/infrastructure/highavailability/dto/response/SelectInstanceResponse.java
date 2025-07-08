package org.xhy.infrastructure.highavailability.dto.response;

/** 选择实例响应对象
 * 
 * @author xhy
 * @since 1.0.0 */
public class SelectInstanceResponse {

    /** 选择的实例ID */
    private String instanceId;

    /** 业务ID */
    private String businessId;

    /** 选择原因/策略信息 */
    private String reason;

    /** 是否成功 */
    private Boolean success;

    /** 错误信息（如果失败） */
    private String errorMessage;

    public SelectInstanceResponse() {
    }

    public SelectInstanceResponse(String instanceId, String businessId, String reason) {
        this.instanceId = instanceId;
        this.businessId = businessId;
        this.reason = reason;
        this.success = true;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}