package org.xhy.interfaces.dto.apikey.request;

/** 查询API密钥请求对象 */
public class QueryApiKeyRequest {

    /** API Key名称/描述模糊查询 */
    private String name;

    /** 状态筛选：TRUE-启用，FALSE-禁用，null-全部 */
    private Boolean status;

    /** 关联的Agent ID */
    private String agentId;

    public QueryApiKeyRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
}