package org.xhy.domain.agent.constant;

/**
 * Agent类型枚举
 */
public enum AgentType {
    
    /**
     * 聊天助手
     */
    CHAT_ASSISTANT(1, "聊天助手"),
    
    /**
     * 功能性Agent
     */
    FUNCTIONAL_AGENT(2, "功能性Agent");
    
    private final Integer code;
    private final String description;
    
    AgentType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据代码获取枚举值
     */
    public static AgentType fromCode(Integer code) {
        if (code == null) {
            return CHAT_ASSISTANT;
        }
        
        for (AgentType type : AgentType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        
        return CHAT_ASSISTANT;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
} 