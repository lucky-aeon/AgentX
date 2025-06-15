package org.xhy.interfaces.api.external;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.agent.service.AgentSessionAppService;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.SessionDTO;
import org.xhy.application.conversation.service.ConversationAppService;
import org.xhy.application.llm.dto.ModelDTO;
import org.xhy.application.llm.service.LLMAppService;
import org.xhy.infrastructure.auth.ExternalApiContext;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.external.request.ExternalChatRequest;
import org.xhy.interfaces.dto.external.request.ExternalCreateSessionRequest;

import java.util.List;

/** 外部API控制器 提供给外部系统的API接口，使用API Key进行身份验证 */
@RestController
@RequestMapping("/v1")
public class LLMAPIController {

    private final ConversationAppService conversationAppService;
    private final AgentSessionAppService agentSessionAppService;
    private final LLMAppService llmAppService;

    public LLMAPIController(ConversationAppService conversationAppService,
                           AgentSessionAppService agentSessionAppService,
                           LLMAppService llmAppService) {
        this.conversationAppService = conversationAppService;
        this.agentSessionAppService = agentSessionAppService;
        this.llmAppService = llmAppService;
    }

    /** 发起对话
     * @param request 聊天请求
     * @return SSE流 */
    @PostMapping("/chat/completions")
    public SseEmitter chat(@RequestBody @Validated ExternalChatRequest request) {
        String userId = ExternalApiContext.getUserId();
        String agentId = ExternalApiContext.getAgentId();

        // 异常分支：如果指定了模型但无权限使用
        if (request.getModel() != null && !llmAppService.canUserUseModel(request.getModel(), userId)) {
            throw new BusinessException("无权限使用指定模型: " + request.getModel());
        }

        // 主流程：构建请求并处理对话
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMessage(request.getMessage());
        chatRequest.setSessionId(request.getSessionId());
        chatRequest.setFileUrls(request.getFiles());

        // 支持外部API指定模型
        return conversationAppService.chatWithModel(chatRequest, userId, request.getModel());
    }

    /** 获取可用模型列表
     * @return 模型列表 */
    @GetMapping("/models")
    public Result<List<ModelDTO>> getAvailableModels() {
        String userId = ExternalApiContext.getUserId();
        return Result.success(llmAppService.getAvailableModelsForUser(userId));
    }

    /** 获取会话列表
     * @return 会话列表 */
    @GetMapping("/sessions")
    public Result<List<SessionDTO>> getSessions() {
        String userId = ExternalApiContext.getUserId();
        String agentId = ExternalApiContext.getAgentId();
        return Result.success(agentSessionAppService.getAgentSessionList(userId, agentId));
    }

    /** 创建新会话
     * @param request 创建会话请求
     * @return 会话信息 */
    @PostMapping("/sessions")
    public Result<SessionDTO> createSession(@RequestBody ExternalCreateSessionRequest request) {
        String userId = ExternalApiContext.getUserId();
        String agentId = ExternalApiContext.getAgentId();
        
        SessionDTO session = agentSessionAppService.createSession(userId, agentId);
        
        // 如果指定了标题，更新标题
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            agentSessionAppService.updateSession(session.getId(), userId, request.getTitle().trim());
            session.setTitle(request.getTitle().trim());
        }
        
        return Result.success(session);
    }

    /** 删除会话
     * @param id 会话ID
     * @return 操作结果 */
    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable String id) {
        String userId = ExternalApiContext.getUserId();
        agentSessionAppService.deleteSession(id, userId);
        return Result.success();
    }
}
