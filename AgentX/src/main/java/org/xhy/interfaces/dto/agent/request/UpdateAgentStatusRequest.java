package org.xhy.interfaces.dto.agent.request;

import org.xhy.infrastructure.utils.ValidationUtils;

/** 更新Agent状态的请求对象 */
public class UpdateAgentStatusRequest {

    private Boolean enabled;

    // 构造方法
    public UpdateAgentStatusRequest() {
    }

    public UpdateAgentStatusRequest(Boolean enabled) {
        this.enabled = enabled;
    }

    /** 校验请求参数 */
    public void validate() {
        ValidationUtils.notNull(enabled, "enabled");
    }

    // Getter和Setter
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
