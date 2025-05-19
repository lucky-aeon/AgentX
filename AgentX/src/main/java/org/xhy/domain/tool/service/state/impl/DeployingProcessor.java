package org.xhy.domain.tool.service.state.impl;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.state.ToolStateProcessor;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.utils.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 工具部署处理器
 */
public class DeployingProcessor implements ToolStateProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(DeployingProcessor.class);
    private static final String DEPLOY_URL = "http://127.0.0.1:8005/deploy?api_key=123456";
    
    @Override
    public ToolStatus getStatus() {
        return ToolStatus.DEPLOYING;
    }
    
    @Override
    public void process(ToolEntity tool) {
        try {
            // 获取安装命令
            if (tool.getInstallCommand() == null || tool.getInstallCommand().isEmpty()) {
                throw new BusinessException("安装命令为空");
            }
            
            // 将安装命令转换为JSON字符串
            String requestBody = JsonUtils.toJsonString(tool.getInstallCommand());
            
            // 发送部署请求并获取响应
            processDeployRequest(requestBody);
            logger.info("工具部署成功，工具ID: {}", tool.getId());
        } catch (Exception e) {
            throw new BusinessException("部署工具失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送部署请求
     * 
     * @param requestBody 请求体JSON字符串
     * @return 响应结果Map
     * @throws Exception 请求异常
     */
    private void processDeployRequest(String requestBody) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 创建POST请求
            HttpPost httpPost = new HttpPost(DEPLOY_URL);
            
            // 设置请求头
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "*/*");
            httpPost.setHeader("Host", "127.0.0.1:8005");
            httpPost.setHeader("Connection", "keep-alive");
            
            // 设置请求体
            StringEntity entity = new StringEntity(requestBody, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            
            // 发送请求并获取响应
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                
                // 读取响应体
                HttpEntity responseEntity = response.getEntity();
                String responseBody = responseEntity != null ? 
                        EntityUtils.toString(responseEntity) : "{}";
                
                // 检查响应状态码
                if (statusCode < 200 || statusCode >= 300) {
                    throw new BusinessException("部署工具失败，状态码: " + statusCode + 
                            ", 错误信息: " + responseBody);
                }
                
                // 解析响应JSON为Map
                Map responseMap = JsonUtils.parseObject(responseBody, Map.class);
                if (responseMap == null) {
                    throw new BusinessException("解析响应失败，响应内容: " + responseBody);
                }


                // 判断响应状态
                String status = (String) responseMap.get("status");
                if (!"success".equals(status)) {
                    throw new BusinessException("部署工具失败，返回状态: " + status);
                }
            }
        }
    }
    
    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.FETCHING_TOOLS;
    }
} 