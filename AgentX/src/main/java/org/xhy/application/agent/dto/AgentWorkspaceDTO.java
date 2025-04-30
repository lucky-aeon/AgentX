package org.xhy.application.agent.dto;

public class AgentWorkspaceDTO {
    /** 代理ID */
    private String agentId;
    /** 用户ID */
    private String userId;

    public AgentWorkspaceDTO(String agentId, String userId) {
        this.agentId = agentId;
        this.userId = userId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
