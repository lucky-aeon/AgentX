package org.xhy.infrastructure.llm.service;

/**
 * OpenAI LLM请求的实现类
 */
public class OpenAILLMRequest implements LLMRequest {
    
    private final Object request;
    
    /**
     * 构造函数
     * 
     * @param request OpenAI请求对象
     */
    public OpenAILLMRequest(Object request) {
        this.request = request;
    }
    
    @Override
    public Object getUnderlyingRequest() {
        return request;
    }
} 