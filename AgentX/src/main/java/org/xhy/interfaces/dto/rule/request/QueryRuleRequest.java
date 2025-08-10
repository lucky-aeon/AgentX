package org.xhy.interfaces.dto.rule.request;

import org.xhy.interfaces.dto.Page;

/** 查询规则请求 */
public class QueryRuleRequest extends Page {

    /** 处理器标识过滤 */
    private String handlerKey;

    /** 关键词搜索（规则名称或描述） */
    private String keyword;

    public QueryRuleRequest() {
    }

    public String getHandlerKey() {
        return handlerKey;
    }

    public void setHandlerKey(String handlerKey) {
        this.handlerKey = handlerKey;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}