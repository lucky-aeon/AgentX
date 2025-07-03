package org.xhy.interfaces.dto.container.request;

import org.xhy.domain.container.constant.ContainerStatus;
import org.xhy.domain.container.constant.ContainerType;
import org.xhy.interfaces.dto.Page;

/** 查询容器请求 */
public class QueryContainerRequest extends Page {

    /** 搜索关键词 */
    private String keyword;

    /** 容器状态 */
    private ContainerStatus status;

    /** 容器类型 */
    private ContainerType type;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public ContainerStatus getStatus() {
        return status;
    }

    public void setStatus(ContainerStatus status) {
        this.status = status;
    }

    public ContainerType getType() {
        return type;
    }

    public void setType(ContainerType type) {
        this.type = type;
    }
}