package org.xhy.interfaces.dto.product.request;

import org.xhy.interfaces.dto.Page;

/** 查询商品请求 */
public class QueryProductRequest extends Page {

    /** 商品类型过滤 */
    private String type;

    /** 状态过滤 */
    private Integer status;

    /** 关键词搜索（商品名称） */
    private String keyword;

    public QueryProductRequest() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}