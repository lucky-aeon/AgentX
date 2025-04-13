package org.xhy.application.conversation.service.message.agent.handler;


import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.handler.content.ChatContext;
import org.xhy.application.conversation.service.message.agent.analysis.AnalyzerMessage;
import org.xhy.application.conversation.service.message.agent.analysis.dto.AnalyzerMessageDTO;
import org.xhy.application.conversation.service.message.agent.event.AgentWorkflowEvent;
import org.xhy.application.conversation.service.message.agent.manager.TaskManager;
import org.xhy.application.conversation.service.message.agent.template.AgentPromptTemplates;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowContext;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowState;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.utils.ModelResponseToJsonUtils;

import java.util.Arrays;


/**
 * 分析用户消息是普通消息还是任务消息
 * 发送给大模型的消息会拥有俩个 systemPrompt
 */
@Component
public class AnalyserMessageHandler  extends AbstractAgentHandler {

    private static final String extraAnalyzerMessageKey = "analyzerMessage";
    protected AnalyserMessageHandler(LLMServiceFactory llmServiceFactory, TaskManager taskManager, ConversationDomainService conversationDomainService, ContextDomainService contextDomainService) {
        super(llmServiceFactory, taskManager, conversationDomainService, contextDomainService);
    }

    @Override
    protected boolean shouldHandle(AgentWorkflowEvent event) {
        return event.getToState() == AgentWorkflowState.ANALYSER_MESSAGE;
    }

    @Override
    protected void transitionToNextState(AgentWorkflowContext<?> context) {
        AnalyzerMessageDTO analyzerMessageDTO = (AnalyzerMessageDTO) context.getExtraData(extraAnalyzerMessageKey);
         // todo xhy 问答消息直接 break，任务消息给到下一个状态
        if (analyzerMessageDTO.getIsQuestion()){
            this.setBreak(analyzerMessageDTO.getIsQuestion());
        }else {
            context.transitionTo(AgentWorkflowState.TASK_SPLITTING);
        }
    }

    @Override
    protected <T> void processEvent(AgentWorkflowContext<?> contextObj) {
        AgentWorkflowContext<T> context = (AgentWorkflowContext<T>) contextObj;

        String userMessage = contextObj.getChatContext().getUserMessage();
        try {
            // 获取流式模型客户端
            ChatLanguageModel chatLanguageModel = getStrandClient(context);

            // 构建请求
            ChatRequest request = buildRequest(context);
            ChatResponse chat = chatLanguageModel.chat(request);
            String text = chat.aiMessage().text();
            AnalyzerMessageDTO analyzerMessageDTO = ModelResponseToJsonUtils.toJson(text,AnalyzerMessageDTO.class);
            context.addExtraData(extraAnalyzerMessageKey, analyzerMessageDTO);

            // 是问答消息则返回大模型的输出
            if (analyzerMessageDTO.getIsQuestion()){
                context.sendEndMessage(analyzerMessageDTO.getReply(), MessageType.TEXT);
                // 设置消息
                context.getLlmMessageEntity().setContent(analyzerMessageDTO.getReply());
                context.getChatContext().setUserMessage(userMessage);
                conversationDomainService.insertBathMessage(Arrays.asList(
                        context.getUserMessageEntity(),
                        context.getLlmMessageEntity()));
                // 关闭连接
                context.completeConnection();
            }
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
