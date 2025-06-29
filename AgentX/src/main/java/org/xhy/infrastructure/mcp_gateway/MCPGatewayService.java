package org.xhy.infrastructure.mcp_gateway;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.tool.model.config.ToolDefinition;
import org.xhy.domain.tool.model.config.ToolSpecificationConverter;
import org.xhy.infrastructure.config.MCPGatewayProperties;
import org.xhy.infrastructure.exception.BusinessException;

import jakarta.annotation.PostConstruct;
import org.xhy.infrastructure.utils.JsonUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** MCP Gateway服务 处理与MCP网关的所有API交互 */
@Service
public class MCPGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(MCPGatewayService.class);

    private final MCPGatewayProperties properties;

    /** 通过构造函数注入配置
     * 
     * @param properties MCP Gateway配置 */
    public MCPGatewayService(MCPGatewayProperties properties) {
        this.properties = properties;
    }

    /** 初始化时验证配置有效性 */
    @PostConstruct
    public void init() {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().trim().isEmpty()) {
            logger.warn("MCP Gateway基础URL未配置 (mcp.gateway.base-url)");
        }

        if (properties.getApiKey() == null || properties.getApiKey().trim().isEmpty()) {
            logger.warn("MCP Gateway API密钥未配置 (mcp.gateway.api-key)");
        }

        logger.info("MCP Gateway服务已初始化，基础URL: {}", properties.getBaseUrl());
    }

    /** 构建用户容器SSE URL（纯技术方法）
     * 
     * @param mcpServerName 工具服务名称
     * @param containerIp 容器IP地址
     * @param containerPort 容器端口
     * @return 用户容器SSE URL */
    public String buildUserContainerUrl(String mcpServerName, String containerIp, Integer containerPort) {
        String containerBaseUrl = "http://" + containerIp + ":" + containerPort;
        return containerBaseUrl + "/" + mcpServerName + "/sse/sse?api_key=" + properties.getApiKey();
    }

    /** 构建全局工具SSE URL（纯技术方法）
     * 
     * @param mcpServerName 工具服务名称
     * @return 全局工具SSE URL */
    public String buildGlobalSSEUrl(String mcpServerName) {
        return properties.getBaseUrl() + "/" + mcpServerName + "/sse/sse?api_key=" + properties.getApiKey();
    }

    /** 部署工具到MCP Gateway
     * 
     * @param installCommand 安装命令
     * @return 部署成功返回true，否则抛出异常
     * @throws BusinessException 如果API调用失败 */
    public boolean deployTool(String installCommand) {
        String url = properties.getBaseUrl() + "/deploy";
        return deployToolToUrl(installCommand, url);
    }

    /** 部署工具到用户容器（方法重载）
     * 
     * @param installCommand 安装命令
     * @param containerIp 容器IP地址
     * @param containerPort 容器端口
     * @return 部署成功返回true，否则抛出异常
     * @throws BusinessException 如果API调用失败 */
    public boolean deployTool(String installCommand, String containerIp, Integer containerPort) {
        String url = "http://" + containerIp + ":" + containerPort + "/deploy";
        return deployToolToUrl(installCommand, url);
    }

    /** 部署工具到指定URL的通用方法 */
    private boolean deployToolToUrl(String installCommand, String url) {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + properties.getApiKey());
            httpPost.setEntity(new StringEntity(installCommand, "UTF-8"));

            logger.info("发送部署请求到: {}", url);
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String responseBody = entity != null ? EntityUtils.toString(entity) : null;

                if (statusCode >= 200 && statusCode < 300 && responseBody != null) {
                    Map result = JsonUtils.parseObject(responseBody, Map.class);
                    logger.info("部署响应: {}", result);
                    return result.containsKey("status") && "success".equals(result.get("status"));
                } else {
                    String errorMsg = String.format("工具部署失败，状态码: %d，响应: %s", statusCode, responseBody);
                    logger.error(errorMsg);
                    throw new BusinessException(errorMsg);
                }
            }
        } catch (IOException e) {
            throw new BusinessException("调用部署API失败: " + e.getMessage(), e);
        }
    }

    /** 从MCP Gateway获取工具列表
     *
     * @param toolName 可选，特定工具名称
     * @return 工具定义列表
     * @throws BusinessException 如果API调用失败 */
    public List<ToolDefinition> listTools(String toolName) throws Exception {
        String url = properties.getBaseUrl() + "/" + toolName + "/sse/sse?api_key=" + properties.getApiKey();
        HttpMcpTransport transport = new HttpMcpTransport.Builder().sseUrl(url).timeout(Duration.ofSeconds(10))
                .logRequests(false).logResponses(true).build();
        McpClient client = new DefaultMcpClient.Builder().transport(transport).build();
        try {
            List<ToolSpecification> toolSpecifications = client.listTools();
            return ToolSpecificationConverter.convert(toolSpecifications);
        } catch (Exception e) {
            logger.error("调用MCP Gateway API失败", e);
            throw new BusinessException("调用MCP Gateway API失败: " + e.getMessage(), e);
        } finally {
            client.close();
        }
    }

    /** 从审核容器获取工具列表
     *
     * @param toolName 工具名称
     * @param containerIp 审核容器IP地址
     * @param containerPort 审核容器端口
     * @return 工具定义列表
     * @throws BusinessException 如果API调用失败 */
    public List<ToolDefinition> listToolsFromReviewContainer(String toolName, String containerIp, Integer containerPort)
            throws Exception {
        String url = "http://" + containerIp + ":" + containerPort + "/" + toolName + "/sse/sse?api_key="
                + properties.getApiKey();

        logger.info("从审核容器获取工具列表: {}", url);

        HttpMcpTransport transport = new HttpMcpTransport.Builder().sseUrl(url).timeout(Duration.ofSeconds(10))
                .logRequests(false).logResponses(true).build();
        McpClient client = new DefaultMcpClient.Builder().transport(transport).build();
        try {
            List<ToolSpecification> toolSpecifications = client.listTools();
            List<ToolDefinition> result = ToolSpecificationConverter.convert(toolSpecifications);

            logger.info("成功从审核容器获取到工具列表，共 {} 个工具定义", result != null ? result.size() : 0);
            return result;

        } catch (Exception e) {
            logger.error("从审核容器调用MCP Gateway API失败: {}:{}", containerIp, containerPort, e);
            throw new BusinessException("从审核容器调用MCP Gateway API失败: " + e.getMessage(), e);
        } finally {
            client.close();
        }
    }

    /** 创建配置了超时的HTTP客户端 */
    private CloseableHttpClient createHttpClient() {
        RequestConfig config = RequestConfig.custom().setConnectTimeout(properties.getConnectTimeout())
                .setSocketTimeout(properties.getReadTimeout()).build();

        return HttpClients.custom().setDefaultRequestConfig(config).build();
    }
}