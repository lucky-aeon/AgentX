package org.xhy.interfaces.dto;

import java.util.List;

/** 分页结果包装类 */
public class PageResult<T> {

    /** 数据记录 */
    private List<T> records;

    /** 总记录数 */
    private Long total;

    /** 每页大小 */
    private Long size;

    /** 当前页码 */
    private Long current;

    /** 总页数 */
    private Long pages;

    public PageResult() {
    }

    public PageResult(Long current, Long size, Long total) {
        this.current = current;
        this.size = size;
        this.total = total;
        this.pages = (total + size - 1) / size;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getCurrent() {
        return current;
    }

    public void setCurrent(Long current) {
        this.current = current;
    }

    public Long getPages() {
        return pages;
    }

    public void setPages(Long pages) {
        this.pages = pages;
    }
}