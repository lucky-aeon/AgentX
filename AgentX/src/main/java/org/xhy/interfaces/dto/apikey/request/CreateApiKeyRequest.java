package org.xhy.interfaces.dto.apikey.request;

import jakarta.validation.constraints.NotBlank;

/** 创建API密钥请求对象 */
public class CreateApiKeyRequest {

    /** 关联的Agent ID */
    @NotBlank(message = "Agent ID不可为空")
    private String agentId;

    /** API Key名称/描述 */
    @NotBlank(message = "API Key名称不可为空")
    private String name;

    public CreateApiKeyRequest() {
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}