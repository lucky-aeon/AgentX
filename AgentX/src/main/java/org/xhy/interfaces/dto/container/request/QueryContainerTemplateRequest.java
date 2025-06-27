package org.xhy.interfaces.dto.container.request;

import org.xhy.interfaces.dto.Page;

/** 查询容器模板请求 */
public class QueryContainerTemplateRequest extends Page {
    
    /** 搜索关键词 */
    private String keyword;
    
    /** 模板类型 */
    private String type;
    
    /** 是否启用 */
    private Boolean enabled;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}