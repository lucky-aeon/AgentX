package org.xhy.interfaces.api.conversation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.ChatResponse;
import org.xhy.application.conversation.service.ConversationService;
import org.xhy.interfaces.api.common.Result;

import javax.annotation.Resource;

/**
 * 对话控制器
 */
@RestController
@RequestMapping("/conversation")
public class ConversationController {
    
    private final Logger logger = LoggerFactory.getLogger(ConversationController.class);
    
    @Resource
    private ConversationService conversationService;
    
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
            ChatResponse response = conversationService.chat(request);
            return Result.success(response);
        } catch (Exception e) {
            logger.error("处理聊天请求异常", e);
            return Result.serverError("处理请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 健康检查接口
     *
     * @return 状态信息
     */
    @GetMapping("/health")
    public Result<Object> health() {
        return Result.success("服务正常运行中");
    }
}
