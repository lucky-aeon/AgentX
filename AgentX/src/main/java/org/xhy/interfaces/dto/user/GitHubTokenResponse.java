package org.xhy.interfaces.dto.user;

import com.alibaba.fastjson.annotation.JSONField;

public class GitHubTokenResponse {
    
    @JSONField(name = "access_token")
    private String accessToken;
    
    @JSONField(name = "token_type")
    private String tokenType;
    
    private String scope;
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
} 