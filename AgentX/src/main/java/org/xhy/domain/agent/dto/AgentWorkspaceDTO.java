package org.xhy.domain.agent.dto;

public class AgentWorkspaceDTO {

    private String agentId;

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
