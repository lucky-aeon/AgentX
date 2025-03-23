package org.xhy.interfaces.api.portal.conversation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.service.ConversationAppService;
import org.xhy.application.conversation.service.MessageAppService;
import org.xhy.application.conversation.service.SessionAppService;
import org.xhy.domain.conversation.model.MessageDTO;
import org.xhy.domain.conversation.model.SessionDTO;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.conversation.*;

import java.util.List;

/**
 * 前台用户会话管理API控制器
 */
@RestController
@RequestMapping("/conversation")
public class PortalSessionController {

    private final SessionAppService sessionAppService;
    private final MessageAppService messageAppService;
    private final ConversationAppService conversationAppService;

    /**
     * 构造函数注入
     */
    public PortalSessionController(SessionAppService sessionAppService,
            MessageAppService messageAppService,
            ConversationAppService conversationAppService) {
        this.sessionAppService = sessionAppService;
        this.messageAppService = messageAppService;
        this.conversationAppService = conversationAppService;
    }

    /**
     * 创建新会话
     */
    @PostMapping("/session")
    public Result<SessionDTO> createSession(@RequestBody CreateSessionRequest request) {
        SessionDTO session = sessionAppService.createSession(
                request.getTitle(), 
                request.getUserId(), 
                request.getDescription());
        return Result.success(session);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/session")
    public Result<List<SessionDTO>> getSessions(@RequestParam String userId,
            @RequestParam(required = false) Boolean archived) {
        List<SessionDTO> sessions;
        if (archived != null) {
            if (archived) {
                sessions = sessionAppService.getUserArchivedSessions(userId);
            } else {
                sessions = sessionAppService.getUserActiveSessions(userId);
            }
        } else {
            sessions = sessionAppService.getUserSessions(userId);
        }
        return Result.success(sessions);
    }

    /**
     * 获取单个会话
     */
    @GetMapping("/session/{sessionId}")
    public Result<SessionDTO> getSession(@PathVariable String sessionId) {
        return Result.success(sessionAppService.getSession(sessionId));
    }

    /**
     * 更新会话
     */
    @PutMapping("/session/{sessionId}")
    public Result<SessionDTO> updateSession(@PathVariable String sessionId,
            @RequestBody UpdateSessionRequest request) {
        return Result.success(sessionAppService.updateSession(
                sessionId, 
                request.getTitle(), 
                request.getDescription()));
    }

    /**
     * 归档会话
     */
    @PutMapping("/session/{sessionId}/archive")
    public Result<SessionDTO> archiveSession(@PathVariable String sessionId) {
        return Result.success(sessionAppService.archiveSession(sessionId));
    }

    /**
     * 恢复归档会话
     */
    @PutMapping("/session/{sessionId}/unarchive")
    public Result<SessionDTO> unarchiveSession(@PathVariable String sessionId) {
        return Result.success(sessionAppService.unarchiveSession(sessionId));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        sessionAppService.deleteSession(sessionId);
        return Result.success(null);
    }

    /**
     * 获取会话消息
     */
    @GetMapping("/session/{sessionId}/messages")
    public Result<List<MessageDTO>> getSessionMessages(@PathVariable String sessionId) {
        return Result.success(messageAppService.getSessionMessages(sessionId));
    }

    /**
     * 发送消息并获取流式回复
     */
    @PostMapping("/chat/{sessionId}")
    public SseEmitter chat(@PathVariable String sessionId, @RequestBody SendMessageRequest request) {
        return conversationAppService.chat("ae37ce1eba445259cc55c6740105c688", request.getContent());
    }

    /**
     * 创建会话并发送第一条消息
     */
    @PostMapping("/session/create-and-chat")
    public SseEmitter createAndChat(@RequestBody CreateAndChatRequest request) {
        return conversationAppService.createSessionAndChat(
                request.getTitle(), 
                request.getUserId(), 
                request.getContent());
    }

    /**
     * 发送消息并获取同步回复(非流式)
     */
    @PostMapping("/chat/{sessionId}/sync")
    public ResponseEntity<MessageDTO> chatSync(@PathVariable String sessionId, @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(conversationAppService.chatSync(sessionId, request.getContent()));
    }

    /**
     * 清除会话上下文
     */
    @PostMapping("/session/{sessionId}/clear-context")
    public ResponseEntity<Void> clearContext(@PathVariable String sessionId) {
        conversationAppService.clearContext(sessionId);
        return ResponseEntity.ok().build();
    }
} 