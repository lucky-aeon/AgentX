package org.xhy.interfaces.api.portal.agent;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.agent.service.AgentSessionAppService;
import org.xhy.application.conversation.dto.AgentPreviewRequest;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.service.ConversationAppService;
import org.xhy.application.conversation.dto.MessageDTO;
import org.xhy.application.conversation.dto.SessionDTO;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.service.message.StreamStateManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Agent会话管理 */
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

    /** 获取会话中的消息列表 */
    @GetMapping("/{sessionId}/messages")
    public Result<List<MessageDTO>> getConversationMessages(@PathVariable String sessionId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(conversationAppService.getConversationMessages(sessionId, userId));
    }

    /** 获取助理会话列表 */
    @GetMapping("/{agentId}")
    public Result<List<SessionDTO>> getAgentSessionList(@PathVariable String agentId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(agentSessionAppService.getAgentSessionList(userId, agentId));
    }

    /** 创建会话 */
    @PostMapping("/{agentId}")
    public Result<SessionDTO> createSession(@PathVariable String agentId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(agentSessionAppService.createSession(userId, agentId));
    }

    /** 更新会话 */
    @PutMapping("/{id}")
    public Result<Void> updateSession(@PathVariable String id, @RequestParam String title) {
        String userId = UserContext.getCurrentUserId();
        agentSessionAppService.updateSession(id, userId, title);
        return Result.success();
    }

    /** 删除会话 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteSession(@PathVariable String id) {
        String userId = UserContext.getCurrentUserId();
        agentSessionAppService.deleteSession(id, userId);
        return Result.success();
    }

    /** 发送消息
     * @param chatRequest 消息对象
     * @return */
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody @Validated ChatRequest chatRequest) {
        return conversationAppService.chat(chatRequest, UserContext.getCurrentUserId());
    }

    /** Agent预览功能 用于在创建/编辑Agent时预览对话效果，无需保存会话
     * @param previewRequest 预览请求对象
     * @return SSE流 */
    @PostMapping("/preview")
    public SseEmitter preview(@RequestBody AgentPreviewRequest previewRequest) {
        String userId = UserContext.getCurrentUserId();
        return conversationAppService.previewAgent(previewRequest, userId);
    }

    /** 主动中断大模型流式回复 */
    @PostMapping("/interrupt/{sessionId}")
    public Result<Void> interruptSession(@PathVariable String sessionId) {
        conversationAppService.interruptSession(sessionId);
        return Result.success();
    }
}
