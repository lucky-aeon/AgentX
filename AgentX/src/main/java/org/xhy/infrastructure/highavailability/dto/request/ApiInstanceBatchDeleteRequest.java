package org.xhy.infrastructure.highavailability.dto.request;

import java.util.List;

/** API实例批量删除请求
 * 
 * @author xhy
 * @since 1.0.0 */
public class ApiInstanceBatchDeleteRequest {

    /** 批量删除的API实例标识列表 */
    private List<ApiInstanceDeleteItem> instances;

    public ApiInstanceBatchDeleteRequest() {
    }

    public ApiInstanceBatchDeleteRequest(List<ApiInstanceDeleteItem> instances) {
        this.instances = instances;
    }

    public List<ApiInstanceDeleteItem> getInstances() {
        return instances;
    }

    public void setInstances(List<ApiInstanceDeleteItem> instances) {
        this.instances = instances;
    }

    /** API实例删除项 */
    public static class ApiInstanceDeleteItem {

        private String apiType;

        private String businessId;

        public ApiInstanceDeleteItem() {
        }

        public ApiInstanceDeleteItem(String apiType, String businessId) {
            this.apiType = apiType;
            this.businessId = businessId;
        }

        public String getApiType() {
            return apiType;
        }

        public void setApiType(String apiType) {
            this.apiType = apiType;
        }

        public String getBusinessId() {
            return businessId;
        }

        public void setBusinessId(String businessId) {
            this.businessId = businessId;
        }
    }
}