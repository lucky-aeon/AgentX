package org.xhy.infrastructure.llm.service;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.stereotype.Component;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.ContextProcessor;
import org.xhy.domain.llm.model.LLMRequest;
import org.xhy.domain.llm.service.LLMRequestService;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI请求服务实现
 * 负责构建LLM请求
 */
@Component
public class OpenAIRequestServiceImpl implements LLMRequestService {

    /**
     * 构建LLM请求
     *
     * @param contextResult   上下文处理结果
     * @param userMessage     用户消息
     * @param systemPrompt    系统提示语
     * @param modelId         模型ID
     * @param temperature     温度参数
     * @param topP            topP参数
     * @return 构建好的领域请求对象
     */
    @Override
    public LLMRequest buildRequest(
            ContextProcessor.ContextResult contextResult,
            String userMessage,
            String systemPrompt,
            String modelId,
            float temperature,
            float topP) {

        // 构建消息列表
        List<LLMRequest.LLMMessage> messages = new ArrayList<>();
        
        // 处理历史消息
        for (MessageEntity messageEntity : contextResult.getMessageEntities()) {
            Role role = messageEntity.getRole();
            String content = messageEntity.getContent();
            
            if (role == Role.USER) {
                messages.add(new LLMRequest.LLMMessage(LLMRequest.MessageType.USER, content));
            } else if (role == Role.SYSTEM) {
                messages.add(new LLMRequest.LLMMessage(LLMRequest.MessageType.ASSISTANT, content));
            }
        }
        
        // 添加摘要消息
        ContextEntity contextEntity = contextResult.getContextEntity();
        if (StringUtils.isNotEmpty(contextEntity.getSummary())) {
            String preStr = "以下消息是用户之前的历史消息精炼成的摘要消息：";
            messages.add(new LLMRequest.LLMMessage(LLMRequest.MessageType.ASSISTANT, preStr + contextEntity.getSummary()));
        }

        // 添加当前用户消息
        messages.add(new LLMRequest.LLMMessage(LLMRequest.MessageType.USER, userMessage));
        
        // 添加系统提示语
        if (StringUtils.isNotEmpty(systemPrompt)) {
            messages.add(new LLMRequest.LLMMessage(LLMRequest.MessageType.SYSTEM, systemPrompt));
        }

        // 构建参数
        LLMRequest.LLMRequestParameters parameters = new LLMRequest.LLMRequestParameters(
                modelId, 
                Double.valueOf(temperature), 
                Double.valueOf(topP)
        );
        
        // 构建并返回请求
        return new LLMRequest.Builder()
                .messages(messages)
                .parameters(parameters)
                .build();
    }
} 