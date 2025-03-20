package org.xhy.interfaces.api.conversation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.ChatResponse;
import org.xhy.application.conversation.dto.StreamChatRequest;
import org.xhy.application.conversation.dto.StreamChatResponse;
import org.xhy.application.conversation.service.ConversationAppService;
import org.xhy.interfaces.api.common.Result;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 对话控制器
 */
@RestController
@RequestMapping("/conversation")
public class ConversationController {

    private final Logger logger = LoggerFactory.getLogger(ConversationController.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Resource
    private ConversationAppService conversationAppService;

    /**
     * 普通聊天接口
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody @Validated ChatRequest request) {
        logger.info("收到聊天请求: {}, 服务商: {}, 模型: {}",
                request.getMessage(),
                request.getProvider() != null ? request.getProvider() : "默认",
                request.getModel() != null ? request.getModel() : "默认");

        // 如果请求没有指定服务商，默认使用SiliconFlow
        if (request.getProvider() == null || request.getProvider().isEmpty()) {
            request.setProvider("siliconflow");
        }

        try {
            ChatResponse response = conversationAppService.chat(request);
            return Result.success(response);
        } catch (Exception e) {
            logger.error("处理聊天请求异常", e);
            return Result.serverError("处理请求失败: " + e.getMessage());
        }
    }

    /**
     * 流式聊天接口，使用SSE (Server-Sent Events) - POST方式
     *
     * @param request 流式聊天请求
     * @return SSE流式响应
     */
    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@RequestBody @Validated StreamChatRequest request) {
        logger.info("收到流式聊天请求(POST): {}, 服务商: {}, 模型: {}",
                request.getMessage(),
                request.getProvider() != null ? request.getProvider() : "默认",
                request.getModel() != null ? request.getModel() : "默认");

        // 如果请求没有指定服务商，默认使用SiliconFlow
        if (request.getProvider() == null || request.getProvider().isEmpty()) {
            request.setProvider("siliconflow");
        }

        // 创建SseEmitter，超时时间设置为5分钟
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        // 设置超时回调
        emitter.onTimeout(() -> {
            logger.warn("流式聊天请求超时：{}", request.getMessage());
        });

        // 设置完成回调
        emitter.onCompletion(() -> {
            logger.info("流式聊天请求完成：{}", request.getMessage());
        });

        // 设置错误回调
        emitter.onError(ex -> {
            logger.error("流式聊天请求错误", ex);
        });

        // 创建新线程处理流式响应
        executorService.execute(() -> {
            try {
                // 使用新的真正流式实现
                conversationAppService.chatStream(request, (response, isLast) -> {
                    try {
                        // 发送每个响应块到客户端
                        emitter.send(response);

                        // 如果是最后一个响应块，完成请求
                        if (isLast) {
                            emitter.complete();
                        }
                    } catch (IOException e) {
                        logger.error("发送流式响应块时出错", e);
                        emitter.completeWithError(e);
                    }
                });
            } catch (Exception e) {
                logger.error("处理流式聊天请求发生异常", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 流式聊天接口，使用SSE (Server-Sent Events) - GET方式
     * 为前端EventSource提供支持，因为EventSource只支持GET请求
     *
     * @param message  消息内容
     * @param provider 服务商
     * @param model    模型
     * @return SSE流式响应
     */
    @GetMapping("/chat/stream")
    public SseEmitter chatStreamGet(
            @RequestParam("message") String message,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "sessionId", required = false) String sessionId) {

        logger.info("收到流式聊天请求(GET): {}, 服务商: {}, 模型: {}",
                message,
                provider != null ? provider : "默认",
                model != null ? model : "默认");

        // 创建请求对象
        StreamChatRequest request = new StreamChatRequest();
        request.setMessage(message);
        request.setProvider(provider != null ? provider : "siliconflow");
        request.setModel(model);
        request.setSessionId(sessionId);

        // 调用POST方法处理
        return chatStream(request);
    }
}
