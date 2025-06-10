package org.xhy.infrastructure.highavailability.client;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.infrastructure.config.HighAvailabilityProperties;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.highavailability.dto.request.ApiInstanceBatchCreateRequest;
import org.xhy.infrastructure.highavailability.dto.request.ApiInstanceCreateRequest;
import org.xhy.infrastructure.highavailability.dto.request.ApiInstanceUpdateRequest;
import org.xhy.infrastructure.highavailability.dto.request.ProjectCreateRequest;
import org.xhy.infrastructure.highavailability.dto.request.ReportResultRequest;
import org.xhy.infrastructure.highavailability.dto.request.SelectInstanceRequest;
import org.xhy.infrastructure.highavailability.dto.response.ApiInstanceDTO;
import org.xhy.infrastructure.highavailability.dto.response.GatewayResult;
import org.xhy.infrastructure.utils.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 高可用网关HTTP客户端
 * 负责与高可用网关进行HTTP通信
 * 
 * @author xhy
 * @since 1.0.0
 */
@Component
public class HighAvailabilityGatewayClient {

    private static final Logger logger = LoggerFactory.getLogger(HighAvailabilityGatewayClient.class);

    private final HighAvailabilityProperties properties;
    private final CloseableHttpClient httpClient;

    public HighAvailabilityGatewayClient(HighAvailabilityProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClients.createDefault();
    }

    /**
     * 选择最佳API实例
     */
    public ApiInstanceDTO selectBestInstance(SelectInstanceRequest request) {
        if (!properties.isEnabled()) {
            throw new BusinessException("高可用功能未启用");
        }

        try {
            String url = properties.getGatewayUrl() + "/gateway/select-instance";
            
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("api-key", properties.getApiKey());
            
            String jsonRequest = JsonUtils.toJsonString(request);
            httpPost.setEntity(new StringEntity(jsonRequest, StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                if (response.getStatusLine().getStatusCode() != 200) {
                    logger.error("选择实例失败，响应码: {}, 响应体: {}", 
                        response.getStatusLine().getStatusCode(), responseBody);
                    throw new BusinessException("选择实例失败: " + responseBody);
                }
                
                // 在客户端层解析响应
                GatewayResult<?> rawResult = JsonUtils.parseObject(responseBody, GatewayResult.class);
                
                if (rawResult == null || !rawResult.isSuccess() || rawResult.getData() == null) {
                    String errorMsg = rawResult != null ? rawResult.getMessage() : "解析响应失败";
                    logger.error("网关返回失败: {}", errorMsg);
                    throw new BusinessException("网关返回失败: " + errorMsg);
                }
                
                // 将data部分转换为ApiInstanceDTO
                String dataJson = JsonUtils.toJsonString(rawResult.getData());
                ApiInstanceDTO selectedInstance = JsonUtils.parseObject(dataJson, ApiInstanceDTO.class);
                
                if (selectedInstance == null) {
                    logger.error("解析API实例信息失败");
                    throw new BusinessException("解析API实例信息失败");
                }
                
                logger.info("成功选择实例: businessId={}, instanceId={}", 
                    selectedInstance.getBusinessId(), selectedInstance.getId());
                return selectedInstance;
            }
            
        } catch (Exception e) {
            logger.error("选择API实例失败", e);
            throw new BusinessException("选择API实例失败", e);
        }
    }

    /**
     * 上报调用结果
     */
    public void reportResult(ReportResultRequest request) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            String url = properties.getGatewayUrl() + "/gateway/report-result";
            
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("api-key", properties.getApiKey());
            
            String jsonRequest = JsonUtils.toJsonString(request);
            httpPost.setEntity(new StringEntity(jsonRequest, StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    logger.warn("上报调用结果失败，响应码: {}, 响应体: {}", 
                        response.getStatusLine().getStatusCode(), responseBody);
                }
            }
            
        } catch (Exception e) {
            logger.error("上报调用结果失败", e);
            // 上报失败不抛异常，避免影响主流程
        }
    }

    /**
     * 创建API实例
     */
    public void createApiInstance(ApiInstanceCreateRequest request) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            String url = properties.getGatewayUrl() + "/instances";
            
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("api-key", properties.getApiKey());
            
            String jsonRequest = JsonUtils.toJsonString(request);
            httpPost.setEntity(new StringEntity(jsonRequest, StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    logger.error("创建API实例失败，响应码: {}, 响应体: {}", 
                        response.getStatusLine().getStatusCode(), responseBody);
                    throw new BusinessException("创建API实例失败: " + responseBody);
                }
            }
            
        } catch (Exception e) {
            logger.error("创建API实例失败", e);
            throw new BusinessException("创建API实例失败", e);
        }
    }

    /**
     * 更新API实例
     * 修复：使用正确的路径参数和请求体类型
     */
    public void updateApiInstance(String apiType, String businessId, ApiInstanceUpdateRequest request) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            String url = String.format("%s/instances/%s/%s", 
                properties.getGatewayUrl(), apiType, businessId);
            
            HttpPut httpPut = new HttpPut(url);
            httpPut.setHeader("Content-Type", "application/json");
            httpPut.setHeader("api-key", properties.getApiKey());
            
            String jsonRequest = JsonUtils.toJsonString(request);
            httpPut.setEntity(new StringEntity(jsonRequest, StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    logger.error("更新API实例失败，响应码: {}, 响应体: {}", 
                        response.getStatusLine().getStatusCode(), responseBody);
                }
            }
            
        } catch (Exception e) {
            logger.error("更新API实例失败", e);
        }
    }

    /**
     * 删除API实例
     * 修复：使用正确的路径参数顺序
     */
    public void deleteApiInstance(String apiType, String businessId) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            String url = String.format("%s/instances/%s/%s",
                properties.getGatewayUrl(), apiType, businessId);
            
            HttpDelete httpDelete = new HttpDelete(url);
            httpDelete.setHeader("api-key", properties.getApiKey());
            
            try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    logger.error("删除API实例失败，响应码: {}, 响应体: {}", 
                        response.getStatusLine().getStatusCode(), responseBody);
                }
            }
            
        } catch (Exception e) {
            logger.error("删除API实例失败", e);
        }
    }

    /**
     * 启用API实例
     * 新增：使API实例可以参与负载均衡
     */
    public void activateApiInstance(String apiType, String businessId) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            String url = String.format("%s/instances/%s/%s/activate",
                properties.getGatewayUrl(), apiType, businessId);
            
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("api-key", properties.getApiKey());
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    logger.error("启用API实例失败，响应码: {}, 响应体: {}", 
                        response.getStatusLine().getStatusCode(), responseBody);
                } else {
                    logger.info("API实例启用成功，apiType: {}, businessId: {}", apiType, businessId);
                }
            }
            
        } catch (Exception e) {
            logger.error("启用API实例失败", e);
        }
    }

    /**
     * 禁用API实例
     * 新增：暂停API实例参与负载均衡
     */
    public void deactivateApiInstance(String apiType, String businessId) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            String url = String.format("%s/instances/%s/%s/deactivate",
                properties.getGatewayUrl(), apiType, businessId);
            
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("api-key", properties.getApiKey());
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    logger.error("禁用API实例失败，响应码: {}, 响应体: {}", 
                        response.getStatusLine().getStatusCode(), responseBody);
                } else {
                    logger.info("API实例禁用成功，apiType: {}, businessId: {}", apiType, businessId);
                }
            }
            
        } catch (Exception e) {
            logger.error("禁用API实例失败", e);
        }
    }



    /**
     * 创建项目
     */
    public void createProject(ProjectCreateRequest request) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            String url = properties.getGatewayUrl() + "/projects";
            
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("api-key", properties.getApiKey());
            
            String jsonRequest = JsonUtils.toJsonString(request);
            httpPost.setEntity(new StringEntity(jsonRequest, StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    logger.warn("创建项目失败，响应码: {}, 响应体: {}", 
                        response.getStatusLine().getStatusCode(), responseBody);
                }
            }
            
        } catch (Exception e) {
            logger.error("创建项目失败", e);
        }
    }

    /**
     * 批量创建API实例
     */
    public void batchCreateApiInstances(List<ApiInstanceCreateRequest> instances) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            String url = properties.getGatewayUrl() + "/instances/batch";
            
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("api-key", properties.getApiKey());
            
            ApiInstanceBatchCreateRequest batchRequest = new ApiInstanceBatchCreateRequest(instances);
            String jsonRequest = JsonUtils.toJsonString(batchRequest);
            httpPost.setEntity(new StringEntity(jsonRequest, StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    logger.error("批量创建API实例失败，响应码: {}, 响应体: {}", 
                        response.getStatusLine().getStatusCode(), responseBody);
                    throw new BusinessException("批量创建API实例失败: " + responseBody);
                }
                
                logger.info("批量创建API实例成功，实例数量: {}", instances.size());
            }
            
        } catch (Exception e) {
            logger.error("批量创建API实例失败", e);
            throw new BusinessException("批量创建API实例失败", e);
        }
    }
} 