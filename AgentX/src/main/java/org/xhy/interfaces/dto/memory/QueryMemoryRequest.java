package org.xhy.interfaces.dto.memory;

/** 查询记忆的请求参数（分页 + 过滤） */
public class QueryMemoryRequest {
    private Integer page = 1;
    private Integer pageSize = 20;
    private String type; // 可选：PROFILE/TASK/FACT/EPISODIC

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}

