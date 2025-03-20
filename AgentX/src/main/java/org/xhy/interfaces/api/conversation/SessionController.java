package org.xhy.interfaces.api.conversation;

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

import java.util.List;

/**
 * 会话管理API控制器
 */
@RestController
@RequestMapping("/conversation")
public class SessionController {

    private final SessionAppService sessionAppService;
    private final MessageAppService messageAppService;
    private final ConversationAppService conversationAppService;

    /**
     * 构造函数注入
     */
    public SessionController(SessionAppService sessionAppService,
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
    public Result<SessionDTO> createSession(@RequestParam String title,
            @RequestParam String userId,
            @RequestParam(required = false) String description) {
        SessionDTO session = sessionAppService.createSession(title, userId, description);
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
            @RequestParam String title,
            @RequestParam(required = false) String description) {
        return Result.success(sessionAppService.updateSession(sessionId, title, description));
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
    @GetMapping("/chat/{sessionId}")
    public SseEmitter chat(@PathVariable String sessionId, @RequestParam String content) {
        return conversationAppService.chat("ae37ce1eba445259cc55c6740105c688", content);
    }

    /**
     * 创建会话并发送第一条消息
     */
    @PostMapping("/session/create-and-chat")
    public SseEmitter createAndChat(@RequestParam String title,
            @RequestParam String userId,
            @RequestParam String content) {
        return conversationAppService.createSessionAndChat(title, userId, content);
    }

    /**
     * 发送消息并获取同步回复(非流式)
     */
    @PostMapping("/chat/{sessionId}/sync")
    public ResponseEntity<MessageDTO> chatSync(@PathVariable String sessionId, @RequestParam String content) {
        return ResponseEntity.ok(conversationAppService.chatSync(sessionId, content));
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