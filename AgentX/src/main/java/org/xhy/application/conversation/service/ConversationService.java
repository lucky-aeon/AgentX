package org.xhy.application.conversation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.ChatResponse;
import org.xhy.domain.llm.model.LlmRequest;
import org.xhy.domain.llm.model.LlmResponse;
import org.xhy.domain.llm.service.LlmService;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 对话服务
 */
@Service
public class ConversationService {
    
    private final Logger logger = LoggerFactory.getLogger(ConversationService.class);
    
    @Resource
    private LlmService defaultLlmService;
    
    @Resource
    private Map<String, LlmService> llmServiceMap;
    
    /**
     * 处理普通聊天请求
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    public ChatResponse chat(ChatRequest request) {
        logger.info("接收到聊天请求: {}", request.getMessage());
        
        LlmService llmService = getLlmService(request.getProvider());
        
        LlmRequest llmRequest = new LlmRequest();
        llmRequest.addUserMessage(request.getMessage());
        
        if (request.getModel() != null && !request.getModel().isEmpty()) {
            logger.info("用户指定模型: {}", request.getModel());
            llmRequest.setModel(request.getModel());
        } else {
            logger.info("使用默认模型: {}", llmService.getDefaultModel());
        }
        
        LlmResponse llmResponse = llmService.chat(llmRequest);
        
        ChatResponse response = new ChatResponse();
        response.setContent(llmResponse.getContent());
        response.setProvider(llmResponse.getProvider());
        response.setModel(llmResponse.getModel());
        response.setSessionId(request.getSessionId());
        
        return response;
    }
    
    /**
     * 获取对应的LLM服务
     *
     * @param provider 服务商名称
     * @return LLM服务
     */
    private LlmService getLlmService(String provider) {
        if (provider == null || provider.isEmpty()) {
            logger.info("使用默认LLM服务: {}", defaultLlmService.getProviderName());
            return defaultLlmService;
        }
        
        String serviceName = provider.toLowerCase() + "LlmService";
        logger.debug("尝试获取服务: {}", serviceName);
        
        LlmService service = llmServiceMap.get(serviceName);
        
        if (service == null) {
            logger.warn("未找到服务商 [{}] 的实现，使用默认服务商: {}", provider, defaultLlmService.getProviderName());
            return defaultLlmService;
        }
        
        logger.info("使用服务商: {}", service.getProviderName());
        return service;
    }
}
