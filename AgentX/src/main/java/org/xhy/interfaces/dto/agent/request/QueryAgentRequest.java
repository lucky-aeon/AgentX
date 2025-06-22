package org.xhy.interfaces.dto.agent.request;

import org.xhy.interfaces.dto.Page;

public class QueryAgentRequest extends Page {

    private String keyword;
    private Boolean enabled;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}