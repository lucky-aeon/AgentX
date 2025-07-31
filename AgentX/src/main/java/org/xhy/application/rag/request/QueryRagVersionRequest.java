package org.xhy.application.rag.request;

import org.xhy.interfaces.dto.Page;

/** 查询RAG版本请求
 * @author xhy
 * @date 2025-07-18 <br/>
 */
public class QueryRagVersionRequest extends Page {

    /** 搜索关键词 */
    private String keyword;

    /** 状态筛选 */
    private Integer status;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}