package org.xhy.application.conversation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.ChatResponse;
import org.xhy.application.conversation.dto.StreamChatRequest;
import org.xhy.application.conversation.dto.StreamChatResponse;
import org.xhy.domain.llm.model.LlmRequest;
import org.xhy.domain.llm.model.LlmResponse;
import org.xhy.domain.llm.service.LlmService;
import org.xhy.infrastructure.integration.llm.siliconflow.SiliconFlowLlmService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

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
     * 处理流式聊天请求
     *
     * @param request 流式聊天请求
     * @return 流式聊天响应列表
     */
    public List<StreamChatResponse> chatStreamList(StreamChatRequest request) {
        logger.info("接收到流式聊天请求: {}", request.getMessage());

        List<StreamChatResponse> responses = new ArrayList<>();

        LlmService llmService = getLlmService(request.getProvider());

        LlmRequest llmRequest = new LlmRequest();
        llmRequest.addUserMessage(request.getMessage());

        // 确保设置流式参数为true
        llmRequest.setStream(true);

        if (request.getModel() != null && !request.getModel().isEmpty()) {
            logger.info("用户指定模型: {}", request.getModel());
            llmRequest.setModel(request.getModel());
        } else {
            logger.info("使用默认模型: {}", llmService.getDefaultModel());
        }

        try {
            // 使用LLM服务的流式接口获取文本块
            List<String> chunks = llmService.chatStreamList(llmRequest);

            // 转换为流式响应
            for (int i = 0; i < chunks.size(); i++) {
                StreamChatResponse response = new StreamChatResponse();
                response.setContent(chunks.get(i));
                response.setDone(i == chunks.size() - 1); // 最后一个标记为完成
                response.setProvider(llmService.getProviderName());
                response.setModel(llmRequest.getModel() != null ? llmRequest.getModel() : llmService.getDefaultModel());
                response.setSessionId(request.getSessionId());

                responses.add(response);
            }

            // 如果没有块或列表为空，添加一个完成块
            if (responses.isEmpty()) {
                responses.add(createFinalResponse(request, llmService));
            }

        } catch (Exception e) {
            logger.error("处理流式聊天请求异常", e);
            // 发生异常时，返回一个错误响应
            StreamChatResponse errorResponse = new StreamChatResponse();
            errorResponse.setContent("处理请求时发生错误: " + e.getMessage());
            errorResponse.setDone(true);
            errorResponse.setProvider(llmService.getProviderName());
            errorResponse
                    .setModel(llmRequest.getModel() != null ? llmRequest.getModel() : llmService.getDefaultModel());
            errorResponse.setSessionId(request.getSessionId());

            responses.add(errorResponse);
        }

        return responses;
    }

    /**
     * 创建最终响应块（表示流结束）
     */
    private StreamChatResponse createFinalResponse(StreamChatRequest request, LlmService llmService) {
        StreamChatResponse finalResponse = new StreamChatResponse();
        finalResponse.setContent("");
        finalResponse.setDone(true); // 标记为最后一个块
        finalResponse.setProvider(llmService.getProviderName());
        finalResponse.setModel(request.getModel() != null ? request.getModel() : llmService.getDefaultModel());
        finalResponse.setSessionId(request.getSessionId());
        return finalResponse;
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

    /**
     * 处理流式聊天请求，使用回调处理响应
     *
     * @param request         流式聊天请求
     * @param responseHandler 响应处理回调
     */
    public void chatStream(StreamChatRequest request, BiConsumer<StreamChatResponse, Boolean> responseHandler) {
        logger.info("接收到真实流式聊天请求: {}", request.getMessage());

        LlmService llmService = getLlmService(request.getProvider());

        LlmRequest llmRequest = new LlmRequest();
        llmRequest.addUserMessage(request.getMessage());

        // 确保设置流式参数为true
        llmRequest.setStream(true);

        if (request.getModel() != null && !request.getModel().isEmpty()) {
            logger.info("用户指定模型: {}", request.getModel());
            llmRequest.setModel(request.getModel());
        } else {
            logger.info("使用默认模型: {}", llmService.getDefaultModel());
        }

        try {
            // 检查LLM服务是否为SiliconFlowLlmService以使用其回调接口
            if (llmService instanceof SiliconFlowLlmService) {
                logger.info("使用SiliconFlow的真实流式响应");
                SiliconFlowLlmService siliconFlowService = (SiliconFlowLlmService) llmService;

                // 使用回调接口
                siliconFlowService.streamChat(llmRequest, (chunk, isLast) -> {
                    StreamChatResponse response = new StreamChatResponse();
                    response.setContent(chunk);
                    response.setDone(isLast);
                    response.setProvider(llmService.getProviderName());
                    response.setModel(
                            llmRequest.getModel() != null ? llmRequest.getModel() : llmService.getDefaultModel());
                    response.setSessionId(request.getSessionId());

                    // 调用响应处理回调
                    responseHandler.accept(response, isLast);
                });
            } else {
                // 对于不支持回调的LLM服务，使用原来的方式
                logger.info("服务商不支持真实流式，使用传统分块方式");
                List<String> chunks = llmService.chatStreamList(llmRequest);

                // 转换为流式响应
                for (int i = 0; i < chunks.size(); i++) {
                    boolean isLast = (i == chunks.size() - 1);

                    StreamChatResponse response = new StreamChatResponse();
                    response.setContent(chunks.get(i));
                    response.setDone(isLast);
                    response.setProvider(llmService.getProviderName());
                    response.setModel(
                            llmRequest.getModel() != null ? llmRequest.getModel() : llmService.getDefaultModel());
                    response.setSessionId(request.getSessionId());

                    // 调用响应处理回调
                    responseHandler.accept(response, isLast);
                }
            }

        } catch (Exception e) {
            logger.error("处理流式聊天请求异常", e);
            // 发生异常时，返回一个错误响应
            StreamChatResponse errorResponse = new StreamChatResponse();
            errorResponse.setContent("处理请求时发生错误: " + e.getMessage());
            errorResponse.setDone(true);
            errorResponse.setProvider(llmService.getProviderName());
            errorResponse
                    .setModel(llmRequest.getModel() != null ? llmRequest.getModel() : llmService.getDefaultModel());
            errorResponse.setSessionId(request.getSessionId());

            // 调用响应处理回调，并标记为最后一个
            responseHandler.accept(errorResponse, true);
        }
    }
}
