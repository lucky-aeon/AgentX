package org.xhy.application.container.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.domain.container.service.ContainerDomainService;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;
import java.util.Map;

/** MCP网关工具部署服务 */
@Service
public class McpGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(McpGatewayService.class);

    private final ContainerDomainService containerDomainService;
    private final RestTemplate restTemplate;

    public McpGatewayService(ContainerDomainService containerDomainService, RestTemplate restTemplate) {
        this.containerDomainService = containerDomainService;
        this.restTemplate = restTemplate;
    }

    /** 检查并创建用户容器工作区
     * 
     * @param userId 用户ID
     * @return 容器健康状态检查结果 */
    public ContainerHealthResult checkAndCreateUserContainer(String userId) {
        // 检查用户是否有容器
        ContainerEntity container = containerDomainService.getUserContainer(userId);
        
        if (container == null) {
            return new ContainerHealthResult(false, "用户容器不存在", null, null);
        }

        if (!container.isRunning()) {
            return new ContainerHealthResult(false, "用户容器未运行", 
                                           container.getExternalPort(), container.getIpAddress());
        }

        // 检查MCP网关是否可用
        String gatewayUrl = buildGatewayUrl(container);
        boolean isHealthy = checkMcpGatewayHealth(gatewayUrl);
        
        if (!isHealthy) {
            return new ContainerHealthResult(false, "MCP网关不可用", 
                                           container.getExternalPort(), container.getIpAddress());
        }

        return new ContainerHealthResult(true, "容器工作区健康", 
                                       container.getExternalPort(), container.getIpAddress());
    }

    /** 部署工具到用户容器
     * 
     * @param userId 用户ID
     * @param toolConfigs 工具配置列表
     * @return 部署结果 */
    public ToolDeploymentResult deployToolsToUserContainer(String userId, List<ToolConfig> toolConfigs) {
        ContainerEntity container = containerDomainService.getUserContainer(userId);
        
        if (container == null || !container.isRunning()) {
            throw new BusinessException("用户容器不可用");
        }

        String gatewayUrl = buildGatewayUrl(container);
        
        try {
            // 调用MCP网关的部署API
            String deployUrl = gatewayUrl + "/deploy";
            
            DeployRequest deployRequest = new DeployRequest();
            deployRequest.setTools(toolConfigs);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("User-Agent", "AgentX-Container-Manager");
            
            HttpEntity<DeployRequest> request = new HttpEntity<>(deployRequest, headers);
            
            ResponseEntity<DeployResponse> response = restTemplate.exchange(
                    deployUrl, HttpMethod.POST, request, DeployResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                DeployResponse deployResponse = response.getBody();
                
                logger.info("工具部署成功: 用户={}, 成功={}, 失败={}", 
                           userId, deployResponse.getSuccessCount(), deployResponse.getFailedCount());
                
                return new ToolDeploymentResult(true, "工具部署成功", 
                                              deployResponse.getSuccessCount(), deployResponse.getFailedCount(),
                                              deployResponse.getErrors());
            } else {
                logger.error("工具部署失败: 用户={}, 状态码={}", userId, response.getStatusCode());
                throw new BusinessException("工具部署失败");
            }
            
        } catch (Exception e) {
            logger.error("部署工具到用户容器失败: 用户={}", userId, e);
            throw new BusinessException("部署工具失败: " + e.getMessage());
        }
    }

    /** 获取容器中已部署的工具状态
     * 
     * @param userId 用户ID
     * @return 工具状态列表 */
    public List<ToolStatus> getDeployedToolsStatus(String userId) {
        ContainerEntity container = containerDomainService.getUserContainer(userId);
        
        if (container == null || !container.isRunning()) {
            throw new BusinessException("用户容器不可用");
        }

        String gatewayUrl = buildGatewayUrl(container);
        
        try {
            String statusUrl = gatewayUrl + "/tools/status";
            
            ResponseEntity<ToolStatusResponse> response = restTemplate.getForEntity(
                    statusUrl, ToolStatusResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getTools();
            } else {
                logger.error("获取工具状态失败: 用户={}, 状态码={}", userId, response.getStatusCode());
                throw new BusinessException("获取工具状态失败");
            }
            
        } catch (Exception e) {
            logger.error("获取用户容器工具状态失败: 用户={}", userId, e);
            throw new BusinessException("获取工具状态失败: " + e.getMessage());
        }
    }

    /** 检查MCP网关健康状态 */
    private boolean checkMcpGatewayHealth(String gatewayUrl) {
        try {
            String healthUrl = gatewayUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.debug("MCP网关健康检查失败: {}", gatewayUrl, e);
            return false;
        }
    }

    /** 构建网关URL */
    private String buildGatewayUrl(ContainerEntity container) {
        if (container.getIpAddress() != null) {
            // 使用容器IP地址
            return String.format("http://%s:%d", container.getIpAddress(), container.getInternalPort());
        } else if (container.getExternalPort() != null) {
            // 使用外部端口映射
            return String.format("http://localhost:%d", container.getExternalPort());
        } else {
            throw new BusinessException("容器网络配置不完整");
        }
    }

    /** 容器健康状态检查结果 */
    public static class ContainerHealthResult {
        private final boolean healthy;
        private final String message;
        private final Integer externalPort;
        private final String ipAddress;

        public ContainerHealthResult(boolean healthy, String message, Integer externalPort, String ipAddress) {
            this.healthy = healthy;
            this.message = message;
            this.externalPort = externalPort;
            this.ipAddress = ipAddress;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getMessage() {
            return message;
        }

        public Integer getExternalPort() {
            return externalPort;
        }

        public String getIpAddress() {
            return ipAddress;
        }
    }

    /** 工具部署结果 */
    public static class ToolDeploymentResult {
        private final boolean success;
        private final String message;
        private final int successCount;
        private final int failedCount;
        private final List<String> errors;

        public ToolDeploymentResult(boolean success, String message, int successCount, int failedCount, List<String> errors) {
            this.success = success;
            this.message = message;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.errors = errors;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    /** 工具配置 */
    public static class ToolConfig {
        private String toolId;
        private String name;
        private String version;
        private String type;
        private String mcpServerName;
        private Map<String, Object> config;

        public ToolConfig() {
        }

        public ToolConfig(String toolId, String name, String mcpServerName) {
            this.toolId = toolId;
            this.name = name;
            this.mcpServerName = mcpServerName;
            this.type = "MCP";
        }

        public String getToolId() {
            return toolId;
        }

        public void setToolId(String toolId) {
            this.toolId = toolId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMcpServerName() {
            return mcpServerName;
        }

        public void setMcpServerName(String mcpServerName) {
            this.mcpServerName = mcpServerName;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }
    }

    /** 工具状态 */
    public static class ToolStatus {
        private String name;
        private String version;
        private String status;
        private String message;
        private String mcpServerName;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getMcpServerName() {
            return mcpServerName;
        }

        public void setMcpServerName(String mcpServerName) {
            this.mcpServerName = mcpServerName;
        }
    }

    /** 部署请求 */
    private static class DeployRequest {
        private List<ToolConfig> tools;

        public List<ToolConfig> getTools() {
            return tools;
        }

        public void setTools(List<ToolConfig> tools) {
            this.tools = tools;
        }
    }

    /** 部署响应 */
    private static class DeployResponse {
        private int successCount;
        private int failedCount;
        private List<String> errors;

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(int failedCount) {
            this.failedCount = failedCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }

    /** 工具状态响应 */
    private static class ToolStatusResponse {
        private List<ToolStatus> tools;

        public List<ToolStatus> getTools() {
            return tools;
        }

        public void setTools(List<ToolStatus> tools) {
            this.tools = tools;
        }
    }
}