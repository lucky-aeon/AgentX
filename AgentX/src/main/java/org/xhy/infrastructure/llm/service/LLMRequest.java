package org.xhy.infrastructure.llm.service;

/**
 * LLM请求的通用接口，用于提高类型安全性
 */
public interface LLMRequest {
    
    /**
     * 获取底层的请求对象
     * 
     * @return 底层的请求对象
     */
    Object getUnderlyingRequest();
} 