package org.xhy.domain.llm.service;

import org.xhy.domain.conversation.service.ContextProcessor;
import org.xhy.domain.llm.model.LLMRequest;

/** LLM请求服务接口 定义在领域层，由基础设施层实现 */
public interface LLMRequestService {

    /** 构建LLM请求
     *
     * @param contextResult 上下文处理结果
     * @param userMessage 用户消息
     * @param systemPrompt 系统提示语
     * @param modelId 模型ID
     * @param temperature 温度参数
     * @param topP topP参数
     * @return 构建好的领域请求对象 */
    LLMRequest buildRequest(ContextProcessor.ContextResult contextResult, String userMessage, String systemPrompt,
            String modelId, float temperature, float topP);
}