package org.xhy.infrastructure.integration.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.llm.model.LlmMessage;
import org.xhy.domain.llm.model.LlmRequest;
import org.xhy.domain.llm.model.LlmResponse;
import org.xhy.domain.llm.service.LlmService;

/**
 * 抽象LLM服务实现
 */
public abstract class AbstractLlmService implements LlmService {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * 服务商名称
     */
    protected final String providerName;
    
    /**
     * API URL
     */
    protected final String apiUrl;
    
    /**
     * API Key
     */
    protected final String apiKey;
    
    /**
     * 默认模型
     */
    protected final String defaultModel;
    
    /**
     * 超时时间(毫秒)
     */
    protected final int timeout;
    
    public AbstractLlmService(String providerName, String apiUrl, String apiKey, String defaultModel, int timeout) {
        this.providerName = providerName;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.timeout = timeout;
    }
    
    @Override
    public String simpleChat(String text) {
        LlmRequest request = new LlmRequest();
        request.setModel(getDefaultModel());
        request.addUserMessage(text);
        
        LlmResponse response = chat(request);
        return response.getContent();
    }
    
    @Override
    public String getProviderName() {
        return providerName;
    }
    
    @Override
    public String getDefaultModel() {
        return defaultModel;
    }
    
    /**
     * 准备请求体，将通用请求转为服务商特定格式
     *
     * @param request 通用请求
     * @return 服务商特定格式的请求体
     */
    protected abstract String prepareRequestBody(LlmRequest request);
    
    /**
     * 解析响应，将服务商特定格式转为通用响应
     *
     * @param responseBody 服务商响应体
     * @return 通用响应
     */
    protected abstract LlmResponse parseResponse(String responseBody);
}
