package org.xhy.interfaces.dto;

public class Page {

    private Long page = 1L;

    private Long pageSize  = 15L;

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }
}
