package org.xhy.interfaces.dto.apikey.request;

import jakarta.validation.constraints.NotNull;

/** 更新API密钥状态请求对象 */
public class UpdateApiKeyStatusRequest {

    /** 状态：TRUE-启用，FALSE-禁用 */
    @NotNull(message = "状态不可为空")
    private Boolean status;

    public UpdateApiKeyStatusRequest() {
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}