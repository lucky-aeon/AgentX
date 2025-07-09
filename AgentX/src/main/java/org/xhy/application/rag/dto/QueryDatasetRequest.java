package org.xhy.application.rag.dto;

/**
 * 数据集查询请求
 * @author shilong.zang
 * @date 2024-12-09
 */
public class QueryDatasetRequest {

    /**
     * 页码，默认1
     */
    private Integer page = 1;

    /**
     * 每页大小，默认15
     */
    private Integer pageSize = 15;

    /**
     * 搜索关键词
     */
    private String keyword;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}