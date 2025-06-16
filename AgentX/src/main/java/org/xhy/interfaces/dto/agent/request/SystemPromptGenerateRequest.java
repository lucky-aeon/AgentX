package org.xhy.interfaces.dto.agent.request;

import java.util.List;

/**
 * 系统提示词生成请求
 */
public class SystemPromptGenerateRequest {

    /** Agent名称 */
    private String agentName;

    /** Agent描述 */
    private String agentDescription;

    /** 工具ID列表 */
    private List<String> toolIds;

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentDescription() {
        return agentDescription;
    }

    public void setAgentDescription(String agentDescription) {
        this.agentDescription = agentDescription;
    }

    public List<String> getToolIds() {
        return toolIds;
    }

    public void setToolIds(List<String> toolIds) {
        this.toolIds = toolIds;
    }
} 