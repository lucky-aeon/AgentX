package org.xhy.infrastructure.integration.llm.siliconflow;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xhy.domain.llm.model.LlmMessage;
import org.xhy.domain.llm.model.LlmRequest;
import org.xhy.domain.llm.model.LlmResponse;
import org.xhy.infrastructure.integration.llm.AbstractLlmService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * SiliconFlow LLM服务实现
 */
@Service
public class SiliconFlowLlmService extends AbstractLlmService {
    
    private final Logger logger = LoggerFactory.getLogger(SiliconFlowLlmService.class);
    
    public SiliconFlowLlmService(
            @Value("${llm.provider.providers.siliconflow.name:SiliconFlow}") String providerName,
            @Value("${llm.provider.providers.siliconflow.api-url:https://api.siliconflow.cn/v1/chat/completions}") String apiUrl,
            @Value("${llm.provider.providers.siliconflow.api-key:}") String apiKey,
            @Value("${llm.provider.providers.siliconflow.model:llama3}") String defaultModel,
            @Value("${llm.provider.providers.siliconflow.timeout:30000}") int timeout) {
        super(providerName, apiUrl, apiKey, defaultModel, timeout);
        
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("SiliconFlow API密钥未配置，请通过环境变量SILICONFLOW_API_KEY设置");
        } else {
            logger.info("初始化SiliconFlow服务，默认模型: {}", defaultModel);
        }
    }
    
    @Override
    public LlmResponse chat(LlmRequest request) {
        if (request.getModel() == null || "default".equals(request.getModel())) {
            logger.info("未指定模型或使用默认模型，使用配置的默认模型: {}", getDefaultModel());
            request.setModel(getDefaultModel());
        }
        
        try {
            logger.info("发送请求到SiliconFlow服务, 模型: {}, 消息数: {}", 
                    request.getModel(), request.getMessages().size());
            
            String requestBody = prepareRequestBody(request);
            String responseBody = sendHttpRequest(requestBody);
            
            logger.debug("SiliconFlow服务响应长度: {}", responseBody.length());
            return parseResponse(responseBody);
        } catch (Exception e) {
            logger.error("调用SiliconFlow服务出错", e);
            LlmResponse errorResponse = new LlmResponse("调用服务时发生错误: " + e.getMessage());
            errorResponse.setProvider(getProviderName());
            errorResponse.setModel(request.getModel());
            return errorResponse;
        }
    }
    
    @Override
    protected String prepareRequestBody(LlmRequest request) {
        JSONObject requestJson = new JSONObject();
        requestJson.put("model", request.getModel());
        
        // 转换消息格式
        JSONArray messagesJson = new JSONArray();
        List<LlmMessage> messages = request.getMessages();
        for (LlmMessage message : messages) {
            JSONObject messageJson = new JSONObject();
            messageJson.put("role", message.getRole());
            messageJson.put("content", message.getContent());
            messagesJson.add(messageJson);
        }
        requestJson.put("messages", messagesJson);
        
        // 添加其他参数
        if (request.getTemperature() != null) {
            requestJson.put("temperature", request.getTemperature());
        } else {
            requestJson.put("temperature", 0.7); // 默认温度
        }
        
        if (request.getMaxTokens() != null) {
            requestJson.put("max_tokens", request.getMaxTokens());
        }
        
        if (request.getStream() != null) {
            requestJson.put("stream", request.getStream());
        } else {
            requestJson.put("stream", false); // 默认非流式
        }
        
        return requestJson.toJSONString();
    }
    
    @Override
    protected LlmResponse parseResponse(String responseBody) {
        JSONObject responseJson = JSON.parseObject(responseBody);
        
        LlmResponse response = new LlmResponse();
        response.setProvider(getProviderName());
        response.setModel(getDefaultModel());
        
        try {
            JSONObject choices = responseJson.getJSONArray("choices").getJSONObject(0);
            JSONObject message = choices.getJSONObject("message");
            String content = message.getString("content");
            
            response.setContent(content);
            response.setFinishReason(choices.getString("finish_reason"));
            
            if (responseJson.containsKey("usage")) {
                JSONObject usage = responseJson.getJSONObject("usage");
                response.setTokenUsage(usage.getInteger("total_tokens"));
                logger.info("请求消耗Token: {}", response.getTokenUsage());
            }
            
            return response;
        } catch (Exception e) {
            logger.error("解析SiliconFlow响应出错", e);
            response.setContent("解析服务响应时发生错误: " + e.getMessage());
            return response;
        }
    }
    
    /**
     * 发送HTTP请求到SiliconFlow服务
     *
     * @param requestBody 请求体
     * @return 响应体
     * @throws IOException 请求异常
     */
    private String sendHttpRequest(String requestBody) throws IOException {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        HttpPost httpPost = new HttpPost(apiUrl);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Authorization", "Bearer " + apiKey);
        
        StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);
        
        logger.debug("发送HTTP请求到: {}", apiUrl);
        
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            logger.debug("HTTP响应状态码: {}", statusCode);
            
            if (statusCode != 200) {
                logger.error("HTTP请求失败，状态码: {}", statusCode);
            }
            
            HttpEntity responseEntity = response.getEntity();
            return EntityUtils.toString(responseEntity);
        } finally {
            httpClient.close();
        }
    }
    
    /**
     * 流式响应处理器接口
     */
    @FunctionalInterface
    public interface StreamResponseHandler {
        /**
         * 处理每个响应块
         * @param chunk 响应文本块
         * @param isLast 是否是最后一个块
         */
        void onChunk(String chunk, boolean isLast);
    }
    
    /**
     * 发送流式请求并通过回调处理每个响应块
     *
     * @param request 请求
     * @param handler 响应处理器
     */
    public void streamChat(LlmRequest request, StreamResponseHandler handler) {
        if (request.getModel() == null || "default".equals(request.getModel())) {
            logger.info("未指定模型或使用默认模型，使用配置的默认模型: {}", getDefaultModel());
            request.setModel(getDefaultModel());
        }
        
        try {
            logger.info("发送流式请求到SiliconFlow服务, 模型: {}, 消息数: {}", 
                    request.getModel(), request.getMessages().size());
            
            // 设置请求为流式
            request.setStream(true);
            
            String requestBody = prepareRequestBody(request);
            
            // 使用流式方式处理响应
            sendStreamHttpRequest(requestBody, handler);
            
        } catch (Exception e) {
            logger.error("调用SiliconFlow流式服务出错", e);
            handler.onChunk("调用流式服务时发生错误: " + e.getMessage(), true);
        }
    }
    
    @Override
    public List<String> chatStreamList(LlmRequest request) {
        if (request.getModel() == null || "default".equals(request.getModel())) {
            logger.info("未指定模型或使用默认模型，使用配置的默认模型: {}", getDefaultModel());
            request.setModel(getDefaultModel());
        }
        
        List<String> chunks = new ArrayList<>();
        
        try {
            logger.info("发送流式请求到SiliconFlow服务, 模型: {}, 消息数: {}", 
                    request.getModel(), request.getMessages().size());
            
            // 设置请求为流式
            request.setStream(true);
            
            String requestBody = prepareRequestBody(request);
            
            // 使用带回调的流式处理方式
            final boolean[] isDone = {false};
            Object lock = new Object();
            
            sendStreamHttpRequest(requestBody, (chunk, isLast) -> {
                chunks.add(chunk);
                
                if (isLast) {
                    synchronized (lock) {
                        isDone[0] = true;
                        lock.notifyAll();
                    }
                }
            });
            
            // 等待所有响应完成
            synchronized (lock) {
                if (!isDone[0]) {
                    try {
                        lock.wait(timeout); // 使用配置的超时时间
                    } catch (InterruptedException e) {
                        logger.warn("等待流式响应完成被中断", e);
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            logger.info("SiliconFlow流式响应完成，共返回 {} 个块", chunks.size());
            return chunks;
            
        } catch (Exception e) {
            logger.error("调用SiliconFlow流式服务出错", e);
            chunks.add("调用流式服务时发生错误: " + e.getMessage());
            return chunks;
        }
    }
    
    /**
     * 发送流式HTTP请求到SiliconFlow服务
     *
     * @param requestBody 请求体
     * @param handler 响应处理器
     * @throws IOException 请求异常
     */
    private void sendStreamHttpRequest(String requestBody, StreamResponseHandler handler) throws IOException {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        HttpPost httpPost = new HttpPost(apiUrl);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Authorization", "Bearer " + apiKey);
        
        StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);
        
        logger.debug("发送流式HTTP请求到: {}", apiUrl);
        
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            logger.debug("HTTP响应状态码: {}", statusCode);
            
            if (statusCode != 200) {
                String errorMessage = "HTTP请求失败，状态码: " + statusCode;
                logger.error(errorMessage);
                handler.onChunk(errorMessage, true);
                return;
            }
            
            HttpEntity responseEntity = response.getEntity();
            
            // 处理流式响应
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseEntity.getContent(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder partialData = new StringBuilder();
                
                while ((line = reader.readLine()) != null) {
                    // 流式响应的每行数据通常以"data: "开头
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6); // 去掉"data: "前缀
                        
                        // 检查是否是流的结束标记
                        if ("[DONE]".equals(data)) {
                            logger.debug("流式响应结束");
                            // 结束标记，调用处理器并标记为最后一个
                            if (partialData.length() > 0) {
                                handler.onChunk(partialData.toString(), true);
                                partialData.setLength(0);
                            } else {
                                handler.onChunk("", true);
                            }
                            break;
                        }
                        
                        try {
                            // 解析JSON数据
                            JSONObject jsonData = JSON.parseObject(data);
                            
                            if (jsonData.containsKey("choices") && !jsonData.getJSONArray("choices").isEmpty()) {
                                JSONObject choice = jsonData.getJSONArray("choices").getJSONObject(0);
                                
                                if (choice.containsKey("delta")) {
                                    JSONObject delta = choice.getJSONObject("delta");
                                    
                                    // 检查是否有content字段
                                    if (delta.containsKey("content")) {
                                        String content = delta.getString("content");
                                        
                                        // 调用处理器处理内容，标记为非最后一个
                                        handler.onChunk(content, false);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("解析流式响应JSON出错", e);
                            handler.onChunk("解析响应出错: " + e.getMessage(), false);
                        }
                    }
                }
            }
        } finally {
            httpClient.close();
        }
    }
} 