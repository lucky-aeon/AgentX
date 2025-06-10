package org.xhy.infrastructure.highavailability.dto.request;

import java.util.List;

/**
 * API实例批量创建请求
 * 
 * @author xhy
 * @since 1.0.0
 */
public class ApiInstanceBatchCreateRequest {

    /**
     * 批量创建的API实例列表
     */
    private List<ApiInstanceCreateRequest> instances;

    public ApiInstanceBatchCreateRequest() {
    }

    public ApiInstanceBatchCreateRequest(List<ApiInstanceCreateRequest> instances) {
        this.instances = instances;
    }

    public List<ApiInstanceCreateRequest> getInstances() {
        return instances;
    }

    public void setInstances(List<ApiInstanceCreateRequest> instances) {
        this.instances = instances;
    }
} 