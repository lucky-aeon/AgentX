package org.xhy.infrastructure.highavailability.gateway;

import org.springframework.stereotype.Component;
import org.xhy.domain.highavailability.gateway.HighAvailabilityGateway;
import org.xhy.infrastructure.highavailability.client.HighAvailabilityGatewayClient;
import org.xhy.infrastructure.highavailability.dto.request.ApiInstanceCreateRequest;
import org.xhy.infrastructure.highavailability.dto.request.ApiInstanceUpdateRequest;
import org.xhy.infrastructure.highavailability.dto.request.ProjectCreateRequest;
import org.xhy.infrastructure.highavailability.dto.request.ReportResultRequest;
import org.xhy.infrastructure.highavailability.dto.request.SelectInstanceRequest;
import org.xhy.infrastructure.highavailability.dto.request.ApiInstanceBatchDeleteRequest;
import org.xhy.infrastructure.highavailability.dto.response.ApiInstanceDTO;

import java.util.List;

/**
 * 高可用网关基础设施实现
 * 负责所有技术细节，包括HTTP调用、序列化、网络异常处理等
 * 
 * @author xhy
 * @since 1.0.0
 */
@Component
public class HighAvailabilityGatewayImpl implements HighAvailabilityGateway {

    private final HighAvailabilityGatewayClient gatewayClient;

    public HighAvailabilityGatewayImpl(HighAvailabilityGatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @Override
    public ApiInstanceDTO selectBestInstance(SelectInstanceRequest request) {
        return gatewayClient.selectBestInstance(request);
    }

    @Override
    public void createApiInstance(ApiInstanceCreateRequest request) {
        gatewayClient.createApiInstance(request);
    }

    @Override
    public void deleteApiInstance(String type, String businessId) {
        gatewayClient.deleteApiInstance(type, businessId);
    }

    @Override
    public void updateApiInstance(String type, String businessId, ApiInstanceUpdateRequest request) {
        gatewayClient.updateApiInstance(type, businessId, request);
    }

    @Override
    public void reportResult(ReportResultRequest request) {
        gatewayClient.reportResult(request);
    }

    @Override
    public void createProject(ProjectCreateRequest request) {
        gatewayClient.createProject(request);
    }

    @Override
    public void batchCreateApiInstances(List<ApiInstanceCreateRequest> requests) {
        gatewayClient.batchCreateApiInstances(requests);
    }

    @Override
    public void activateApiInstance(String type, String businessId) {
        gatewayClient.activateApiInstance(type, businessId);
    }

    @Override
    public void deactivateApiInstance(String type, String businessId) {
        gatewayClient.deactivateApiInstance(type, businessId);
    }

    @Override
    public void batchDeleteApiInstances(List<ApiInstanceBatchDeleteRequest.ApiInstanceDeleteItem> instances) {
        gatewayClient.batchDeleteApiInstances(instances);
    }
} 