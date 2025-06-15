package org.xhy.application.apikey.dto;

/**
 * API Key验证结果
 */
public class ApiKeyValidationResult {
    
    private final boolean valid;
    private final String userId;
    private final String agentId;
    private final String message;
    
    private ApiKeyValidationResult(boolean valid, String userId, String agentId, String message) {
        this.valid = valid;
        this.userId = userId;
        this.agentId = agentId;
        this.message = message;
    }
    
    /**
     * 创建验证成功结果
     */
    public static ApiKeyValidationResult success(String userId, String agentId) {
        return new ApiKeyValidationResult(true, userId, agentId, "验证成功");
    }
    
    /**
     * 创建验证失败结果
     */
    public static ApiKeyValidationResult failure(String message) {
        return new ApiKeyValidationResult(false, null, null, message);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getAgentId() {
        return agentId;
    }
    
    public String getMessage() {
        return message;
    }
} 