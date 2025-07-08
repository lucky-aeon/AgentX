package org.xhy.application.conversation.service.message.agent.handler;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.handler.content.ChatContext;
import org.xhy.application.conversation.service.message.agent.analysis.dto.AnalyzerMessageDTO;
import org.xhy.application.conversation.service.message.agent.event.AgentWorkflowEvent;
import org.xhy.application.conversation.service.message.agent.manager.TaskManager;
import org.xhy.application.conversation.service.message.agent.service.InfoRequirementService;
import org.xhy.application.conversation.service.message.agent.template.AgentPromptTemplates;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowContext;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowState;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.utils.ModelResponseToJsonUtils;

import java.util.Collections;
import java.util.List;


/**
 * 分析用户消息是普通消息还是任务消息
 * 发送给大模型的消息会拥有俩个 systemPrompt
 */
@Component
public class AnalyserMessageHandler extends AbstractAgentHandler {

    private static final String extraAnalyzerMessageKey = "analyzerMessage";
    
    protected AnalyserMessageHandler(
            LLMServiceFactory llmServiceFactory,
            TaskManager taskManager,
            ContextDomainService contextDomainService,
            InfoRequirementService infoRequirementService,
            MessageDomainService messageDomainService) {
        super(llmServiceFactory, taskManager,  contextDomainService,messageDomainService);
    }

    @Override
    protected boolean shouldHandle(AgentWorkflowEvent event) {
        return event.getToState() == AgentWorkflowState.ANALYSER_MESSAGE;
    }

    @Override
    protected void transitionToNextState(AgentWorkflowContext<?> context) {
        AnalyzerMessageDTO analyzerMessageDTO = (AnalyzerMessageDTO) context.getExtraData(extraAnalyzerMessageKey);
        // 问答消息直接 break，任务消息转向任务拆分
        if (analyzerMessageDTO.getIsQuestion()){
            this.setBreak(analyzerMessageDTO.getIsQuestion());
        } else {
            context.transitionTo(AgentWorkflowState.TASK_SPLITTING);
        }
    }

    /**
     * 保存消息策略：
     * 如果是问答消息，则都要保存
     * 如果是任务消息，则放行，后续会通过任务拆分进行保存
     * 问题：
     * 如果是任务消息，但是被识别到缺少信息，则就没有保存消息了，如何解决？
     * @param <T>
     */
    @Override
    @SuppressWarnings("unchecked")
    protected <T> void processEvent(AgentWorkflowContext<?> contextObj) {
        AgentWorkflowContext<T> context = (AgentWorkflowContext<T>) contextObj;

        String userMessage = contextObj.getChatContext().getUserMessage();
        try {
            // 获取流式模型客户端
            ChatModel strandClient = getStrandClient(context);

            // 构建请求
            ChatRequest request = buildRequest(context);
            ChatResponse chat = strandClient.chat(request);
            String text = chat.aiMessage().text();
            AnalyzerMessageDTO analyzerMessageDTO = ModelResponseToJsonUtils.toJson(text, AnalyzerMessageDTO.class);
            context.addExtraData(extraAnalyzerMessageKey, analyzerMessageDTO);
            context.getChatContext().setUserMessage(userMessage);

            // 是问答消息则返回大模型的输出
            if (analyzerMessageDTO.getIsQuestion()) {
                context.sendEndMessage(analyzerMessageDTO.getReply(), MessageType.TEXT);
                // 保存消息
                context.getLlmMessageEntity().setContent(analyzerMessageDTO.getReply());
                saveMessageAndUpdateContext(Collections.singletonList(context.getLlmMessageEntity()),context.getChatContext());
                // 关闭连接
                context.completeConnection();
                return;
            }
            saveMessageAndUpdateContext(Collections.singletonList(context.getUserMessageEntity()),context.getChatContext());
        } catch (Exception e) {
            context.handleError(e);
        }
    }

    /**
     * 构建任务拆分请求
     */
    private <T> ChatRequest buildRequest(AgentWorkflowContext<T> context) {
        ChatContext chatContext = context.getChatContext();
        String userMessage = chatContext.getUserMessage();
        chatContext.setUserMessage(AgentPromptTemplates.getAnalyserMessagePrompt(userMessage));
        return chatContext.prepareChatRequest().build();
    }
}
