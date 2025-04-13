package org.xhy.interfaces.dto.plugins;

import org.xhy.domain.plugins.constant.PluginStatus;

import jakarta.validation.constraints.Min;

/**
 * 获取插件列表请求参数
 */
public class GetListReq {
    /**
     * 插件名称
     */
    private String name;
    
    /**
     * 插件状态
     */
    private PluginStatus status;
    
    /**
     * 页码
     */
    @Min(1)
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    @Min(1)
    private Integer size = 10;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PluginStatus getStatus() {
        return status;
    }

    public void setStatus(PluginStatus status) {
        this.status = status;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
