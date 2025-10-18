package org.xhy.application.conversation.service.message.builtin.multiagent;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.application.conversation.dto.ChatRequest;
import org.xhy.application.conversation.dto.ChatResponse;
import org.xhy.application.conversation.service.ChatSessionManager;
import org.xhy.application.conversation.service.ConversationAppService;
import org.xhy.application.conversation.service.message.builtin.AbstractBuiltInToolProvider;
import org.xhy.application.conversation.service.message.builtin.BuiltInTool;
import org.xhy.application.conversation.service.message.builtin.ToolDefinition;
import org.xhy.application.conversation.service.message.util.ChatContextHolder;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.conversation.model.MessageEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** 多智能体（子Agent）内置工具提供者
 *
 * 为当前Agent的 linkedAgentIds 生成一组工具：call_<agentName>(message: string)
 * 执行阶段当前返回占位文本，后续将接入同会话流式子调用与回显。
 */
@BuiltInTool(name = "multi_agent", description = "调用关联的子Agent执行子任务", priority = 20)
public class MultiAgentBuiltInToolProvider extends AbstractBuiltInToolProvider {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentBuiltInToolProvider.class);

    private final AgentDomainService agentDomainService;
    private final ConversationAppService conversationAppService;
    private final ChatSessionManager chatSessionManager;
    private final org.xhy.domain.conversation.service.MessageDomainService messageDomainService;

    // 工具名到子AgentId的映射（用于执行时反查）
    private final java.util.concurrent.ConcurrentHashMap<String, String> toolToAgentId = new java.util.concurrent.ConcurrentHashMap<>();

    public MultiAgentBuiltInToolProvider(AgentDomainService agentDomainService,
                                         ConversationAppService conversationAppService,
                                         ChatSessionManager chatSessionManager,
                                         org.xhy.domain.conversation.service.MessageDomainService messageDomainService) {
        this.agentDomainService = agentDomainService;
        this.conversationAppService = conversationAppService;
        this.chatSessionManager = chatSessionManager;
        this.messageDomainService = messageDomainService;
    }

    @Override
    public List<ToolDefinition> defineTools(AgentEntity agent) {
        List<String> linkedIds = agent.getLinkedAgentIds();
        if (linkedIds == null || linkedIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ToolDefinition> defs = new ArrayList<>();
        for (String id : linkedIds) {
            try {
                AgentEntity linked = agentDomainService.getAgentById(id);
                if (linked == null || !Boolean.TRUE.equals(linked.getEnabled())) {
                    continue;
                }
                String toolName = Optional.ofNullable(linked.getName()).orElse(linked.getId());
                String desc = linked.getDescription();
                defs.add(ToolDefinition.builder().name(toolName).description(desc)
                        .addRequiredStringParameter("message", "要发送给子Agent的消息内容").build());
                toolToAgentId.put(toolName, linked.getId());
            } catch (Exception e) {
                log.warn("创建子Agent工具失败，id: {}，原因: {}", id, e.getMessage());
            }
        }
        return defs;
    }

    @Override
    protected String doExecute(String toolName, JsonNode arguments, AgentEntity agent, Object memoryId) {
        try {
            String message = getRequiredStringParameter(arguments, "message");
            String targetAgentId = toolToAgentId.get(toolName);
            if (targetAgentId == null) {
                return formatError("未找到对应的子Agent: " + toolName);
            }

            // 获取当前会话上下文（同会话）
            var ctx = ChatContextHolder.get();
            if (ctx == null) {
                return formatError("当前对话上下文不可用，无法调用子Agent");
            }

            // 将该子Agent工具标记为“抑制主时间线工具展示”
            try {
                ctx.getSuppressedToolNames().add(toolName);
            } catch (Exception ignore) {
                // ignore
            }

            ChatRequest subReq = new ChatRequest();
            subReq.setSessionId(ctx.getSessionId());
            subReq.setMessage(message);

            // 尝试获取外层SSE连接，仅发送一次“子 Agent 调用”提示
            SseEmitter emitter = chatSessionManager.getEmitter(ctx.getSessionId());
            if (emitter != null) {
                // 发送开始事件（仅一次，不持久化，不转发子Agent流）
                String startText = "子 Agent 调用：" + toolName.replaceFirst("^call_", "");
                chatSessionManager.send(ctx.getSessionId(), AgentChatResponse
                        .build(startText, MessageType.SUB_AGENT_CALL_START));
                // 持久化一条元消息，保证刷新后也可见
                try {
                    MessageEntity meta = new MessageEntity();
                    meta.setRole(Role.ASSISTANT);
                    meta.setSessionId(ctx.getSessionId());
                    meta.setContent(startText);
                    meta.setMessageType(MessageType.SUB_AGENT_CALL_START);
                    messageDomainService.saveMessageAndUpdateContext(java.util.Collections.singletonList(meta),
                            ctx.getContextEntity());
                } catch (Exception ignore) {}
                // 同步执行子Agent（抑制消息持久化与回显），仅返回文本供主Agent使用
                ChatResponse resp = conversationAppService.chatSyncWithAgent(subReq, ctx.getUserId(), targetAgentId, true);
                String text = resp != null && resp.getContent() != null ? resp.getContent() : "";
                return text.isEmpty() ? "(子Agent无输出)" : text;
            } else {
                // 无外层SSE，持久化一条元消息（保证刷新可见），再同步调用子Agent（抑制持久化）
                try {
                    String startText = "子 Agent 调用：" + toolName.replaceFirst("^call_", "");
                    MessageEntity meta = new MessageEntity();
                    meta.setRole(Role.ASSISTANT);
                    meta.setSessionId(ctx.getSessionId());
                    meta.setContent(startText);
                    meta.setMessageType(MessageType.SUB_AGENT_CALL_START);
                    messageDomainService.saveMessageAndUpdateContext(java.util.Collections.singletonList(meta),
                            ctx.getContextEntity());
                } catch (Exception ignore) {}
                ChatResponse resp = conversationAppService.chatSyncWithAgent(subReq, ctx.getUserId(), targetAgentId, true);
                String text = resp != null ? (resp.getContent() == null ? "" : resp.getContent()) : "";
                return text.isEmpty() ? "(子Agent无输出)" : text;
            }
        } catch (IllegalArgumentException e) {
            return formatError(e.getMessage());
        } catch (Exception e) {
            log.error("子Agent工具执行失败: {}", e.getMessage(), e);
            return formatError("子Agent调用失败: " + e.getMessage());
        }
    }
}
