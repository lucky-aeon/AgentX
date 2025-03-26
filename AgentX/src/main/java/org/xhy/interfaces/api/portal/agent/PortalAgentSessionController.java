package org.xhy.interfaces.api.portal.agent;

import org.springframework.web.bind.annotation.*;
import org.xhy.application.agent.service.AgentSessionAppService;
import org.xhy.application.conversation.dto.StreamChatRequest;
import org.xhy.application.conversation.dto.StreamChatResponse;
import org.xhy.application.conversation.service.ConversationAppService;
import org.xhy.domain.conversation.dto.MessageDTO;
import org.xhy.domain.conversation.dto.SessionDTO;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.conversation.ConversationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/agent/session")
public class PortalAgentSessionController {

    private final Logger logger = LoggerFactory.getLogger(PortalAgentSessionController.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final AgentSessionAppService agentSessionAppService;
    private final ConversationAppService conversationAppService;

    public PortalAgentSessionController(AgentSessionAppService agentSessionAppService,
            ConversationAppService conversationAppService) {
        this.agentSessionAppService = agentSessionAppService;
        this.conversationAppService = conversationAppService;
    }

    /**
     * 获取会话中的消息列表
     */
    @GetMapping("/{sessionId}/messages")
    public Result<List<MessageDTO>> getConversationMessages(@PathVariable String sessionId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(conversationAppService.getConversationMessages(sessionId, userId));
    }

    /**
     * 获取助理会话列表
     */
    @GetMapping("/{agentId}")
    public Result<List<SessionDTO>> getAgentSessionList(@PathVariable String agentId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(agentSessionAppService.getAgentSessionList(userId, agentId));
    }

    /**
     * 创建会话
     */
    @PostMapping("/{agentId}")
    public Result<SessionDTO> createSession(@PathVariable String agentId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(agentSessionAppService.createSession(userId, agentId));
    }

    /**
     * 更新会话
     */
    @PutMapping("/{id}")
    public Result<Void> updateSession(@PathVariable String id, @RequestParam String title) {
        String userId = UserContext.getCurrentUserId();
        agentSessionAppService.updateSession(id, userId, title);
        return Result.success();
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteSession(@PathVariable String id) {
        String userId = UserContext.getCurrentUserId();
        agentSessionAppService.deleteSession(id, userId);
        return Result.success();
    }

    /**
     * 发送消息
     * 
     * @param id      会话id
     * @param request 消息请求
     * @return SSE流式响应
     */
    @PostMapping("/{id}/message")
    public SseEmitter sendMessage(@PathVariable String id, @RequestBody ConversationRequest request) {
        String userId = UserContext.getCurrentUserId();

        // 存储用户消息到数据库
        conversationAppService.sendMessage(id, userId, request.getMessage(), null);

        // 创建SseEmitter，超时时间设置为5分钟
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        // 创建新线程处理流式响应
        executorService.execute(() -> {
            try {
                // 将ConversationRequest转换为StreamChatRequest
                StreamChatRequest streamRequest = new StreamChatRequest();
                streamRequest.setMessage(request.getMessage());
                streamRequest.setSessionId(id);

                // todo xhy 目前先这样写，因为没有对接服务商的领域

                // 调用AgentSessionAppService获取Agent配置信息
//                agentSessionAppService.sendMessage(id, userId, request);

                // 用于收集完整的助理回复内容
                StringBuilder fullAssistantResponse = new StringBuilder();
                String[] provider = { null };
                String[] model = { null };

                // 使用ConversationAppService的流式对话方法
                conversationAppService.chatStream(streamRequest, (response, isLast) -> {
                    try {
                        // 收集助理回复内容
                        fullAssistantResponse.append(response.getContent());

                        // 记录提供商和模型信息
                        if (provider[0] == null) {
                            provider[0] = response.getProvider();
                        }
                        if (model[0] == null) {
                            model[0] = response.getModel();
                        }

                        // 发送每个响应块到客户端
                        emitter.send(response);

                        // 如果是最后一个响应块，存储助理回复并完成请求
                        if (isLast) {
                            // 存储助理的完整回复到数据库
                            // 这里模拟token计数为内容长度，实际应使用真实的token计数
                            int tokenCount = fullAssistantResponse.length() / 4; // 简单估算
                            conversationAppService.saveAssistantMessage(
                                    id,
                                    fullAssistantResponse.toString(),
                                    provider[0],
                                    model[0],
                                    tokenCount);

                            emitter.complete();
                        }
                    } catch (IOException e) {
                        logger.error("发送流式响应块时出错", e);
                        emitter.completeWithError(e);
                    }
                });
            } catch (Exception e) {
                logger.error("处理Agent会话消息请求发生异常", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
