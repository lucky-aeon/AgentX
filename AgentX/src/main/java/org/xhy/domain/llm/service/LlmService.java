package org.xhy.domain.llm.service;

import org.xhy.domain.llm.model.LlmRequest;
import org.xhy.domain.llm.model.LlmResponse;

import java.util.List;

/**
 * LLM服务接口
 */
public interface LlmService {
    
    /**
     * 发送请求到LLM服务商
     *
     * @param request LLM请求
     * @return LLM响应
     */
    LlmResponse chat(LlmRequest request);
    
    /**
     * 发送流式请求到LLM服务商，返回文本块列表
     *
     * @param request LLM请求
     * @return 文本块列表
     */
    List<String> chatStreamList(LlmRequest request);
    
    /**
     * 发送简单的文本请求
     *
     * @param text 用户输入文本
     * @return 生成的响应内容
     */
    String simpleChat(String text);
    
    /**
     * 获取服务商名称
     *
     * @return 服务商名称
     */
    String getProviderName();
    
    /**
     * 获取默认模型名称
     *
     * @return 默认模型名称
     */
    String getDefaultModel();
}
