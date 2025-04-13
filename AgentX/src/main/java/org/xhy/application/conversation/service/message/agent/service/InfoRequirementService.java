package org.xhy.application.conversation.service.message.agent.service;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.conversation.service.handler.content.ChatContext;
import org.xhy.application.conversation.service.message.agent.analysis.dto.InfoRequirementDTO;
import org.xhy.application.conversation.service.message.agent.template.AgentPromptTemplates;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowContext;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.utils.ModelResponseToJsonUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InfoRequirementService {

    private static final Logger log = LoggerFactory.getLogger(InfoRequirementService.class);
    
    // 存储等待用户输入的Future，以sessionId为键
    private static final Map<String, CompletableFuture<String>> WAITING_FUTURES = new ConcurrentHashMap<>();
    // 存储被阻塞的工作流上下文
    private static final Map<String, AgentWorkflowContext> BLOCKING_INFO = new ConcurrentHashMap<>();
    // 最大尝试次数限制
    private static final int MAX_INFO_CHECK_ATTEMPTS = 3;


    private final LLMServiceFactory llmServiceFactory;
    private final MessageDomainService messageDomainService;

    protected InfoRequirementService(
            LLMServiceFactory llmServiceFactory,
            MessageDomainService messageDomainService) {
        this.llmServiceFactory = llmServiceFactory;
        this.messageDomainService = messageDomainService;
    }

    /**
     * 处理用户提供的补充信息
     * @param sessionId 会话ID
     * @param userInput 用户输入
     */
    public void handleUserInput(String sessionId, String userInput) {
        // 获取被阻塞的上下文
        AgentWorkflowContext agentWorkflowContext = BLOCKING_INFO.get(sessionId);
        if (agentWorkflowContext == null) {
            return;
        }

        // 获取等待的Future并完成它，这会自动触发后续处理
        CompletableFuture<String> waitingFuture = WAITING_FUTURES.get(sessionId);
        if (waitingFuture != null) {
            // 注意：我们不在这里设置用户消息，让thenCompose回调中设置
            // 这样保证在处理链中正确传递用户输入
            waitingFuture.complete(userInput);
        }
    }

    /**
     * 获取被阻塞的工作流上下文
     * @param sessionId 会话ID
     * @return 工作流上下文
     */
    public AgentWorkflowContext getBlockingInfo(String sessionId) {
        return BLOCKING_INFO.get(sessionId);
    }

    /**
     * 检查会话是否在等待用户输入
     * @param sessionId 会话ID
     * @return 是否在等待
     */
    public boolean isWaitingForInput(String sessionId) {
        return WAITING_FUTURES.containsKey(sessionId);
    }

    /**
     * 检查信息完整性并等待用户输入（如需要）
     * 此方法结合了初始检查和后续检查，增加了尝试次数限制
     * 
     * @param context 工作流上下文
     * @return 带有信息完整性状态的CompletableFuture
     */
    public CompletableFuture<Boolean> checkInfoAndWaitIfNeeded(AgentWorkflowContext<?> context) {
        // 调用实际实现，设置初始尝试次数为0
        return checkInfoCompleteness(context, 0);
    }
    
    /**
     * 检查信息完整性的实际实现，带有尝试次数控制
     * 
     * @param context 工作流上下文
     * @param attemptCount 当前尝试次数
     * @return 带有信息完整性状态的CompletableFuture
     */
    private CompletableFuture<Boolean> checkInfoCompleteness(
            AgentWorkflowContext<?> context,
            int attemptCount) {
        
        String sessionId = context.getChatContext().getSessionId();
        String userMessage = context.getChatContext().getUserMessage();
        
        // 检查是否超过最大尝试次数
        MessageEntity llmMessageEntity = context.getLlmMessageEntity();
        if (attemptCount > MAX_INFO_CHECK_ATTEMPTS) {
            log.info("会话[{}]已达到最大信息补充尝试次数({}次)，将基于当前信息继续处理", 
                    sessionId, MAX_INFO_CHECK_ATTEMPTS);
            
            // 清理等待状态
            WAITING_FUTURES.remove(sessionId);
            BLOCKING_INFO.remove(sessionId);
            
            // 告知用户已达最大尝试次数，继续处理
            context.sendEndMessage("已尝试多次获取信息，将基于当前提供的信息继续处理。", MessageType.TEXT);

            // 返回true，表示继续处理（尽管信息可能不完整）
            return CompletableFuture.completedFuture(true);
        }
        
        try {
            // 获取模型客户端
            ChatLanguageModel chatLanguageModel = getStrandClient(context);
            
            // 构建请求
            ChatRequest request = buildRequest(context);
            request.messages().add(new SystemMessage(AgentPromptTemplates.getInfoAnalysisPrompt()));
            ChatResponse chat = chatLanguageModel.chat(request);
            String text = chat.aiMessage().text();
            
            log.debug("会话[{}]信息完整性检查结果: {}", sessionId, text);
            
            InfoRequirementDTO infoRequirementDTO = ModelResponseToJsonUtils.toJson(text, InfoRequirementDTO.class);

            if (infoRequirementDTO == null){
                context.sendEndMessage("出现了点错误，请重试", MessageType.TEXT);
                return CompletableFuture.completedFuture(false);
            }

            // 信息完整，可以继续处理
            if (infoRequirementDTO.isInfoComplete()) {
                log.info("会话[{}]信息完整，可以继续处理", sessionId);
                
                // 清理等待状态
                WAITING_FUTURES.remove(sessionId);
                BLOCKING_INFO.remove(sessionId);
                
                return CompletableFuture.completedFuture(true);
            }
            
            // 信息不完整，需要提示用户补充
            log.info("会话[{}]信息不完整，尝试次数:{}/{}，等待用户补充", 
                    sessionId, attemptCount + 1, MAX_INFO_CHECK_ATTEMPTS);
            
            String missingInfoPrompt = infoRequirementDTO.getMissingInfoPrompt();
            
            // 向用户发送提示
            context.sendEndMessage(missingInfoPrompt, MessageType.TEXT);
            
            // 保存消息
            llmMessageEntity.setContent(missingInfoPrompt);

            List<MessageEntity> messageEntityList = new ArrayList<>();
            // 保存原始用户消息，以便在消息记录中显示
            if (attemptCount == 0) {
                context.getChatContext().setUserMessage(userMessage);
            }else {
                // 保存用户消息
                MessageEntity userMessageEntity = context.getUserMessageEntity();
                userMessageEntity.setContent(userMessage);
                messageEntityList.add(userMessageEntity);
            }
            messageEntityList.add(llmMessageEntity);
            // 保存消息记录
            messageDomainService.saveMessageAndUpdateContext(messageEntityList,context.getChatContext().getContextEntity());
            context.getChatContext().getMessageHistory().add(llmMessageEntity);

            
            // 创建等待用户输入的Future
            CompletableFuture<String> waitForUserInput = new CompletableFuture<>();
            
            // 保存等待Future和上下文
            WAITING_FUTURES.put(sessionId, waitForUserInput);
            BLOCKING_INFO.put(sessionId, context);
            
            // 返回转换后的Future，当用户提供输入时会自动继续处理
            return waitForUserInput.thenCompose(input -> {
                // 【关键】手动更新上下文中的用户消息为新提供的补充信息
                // 这样下一次检查时模型才能评估到这个新信息
                context.getChatContext().setUserMessage(input);
                
                log.info("会话[{}]收到用户补充信息: {}", sessionId, input);
                
                // 递归调用自身，尝试次数+1
                return checkInfoCompleteness(context, attemptCount + 1);
            });
            
        } catch (Exception e) {
            log.error("会话[{}]信息完整性检查异常", sessionId, e);
            context.handleError(e);
            
            // 清理等待状态
            WAITING_FUTURES.remove(sessionId);
            BLOCKING_INFO.remove(sessionId);
            
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 构建请求
     */
    private <T> ChatRequest buildRequest(AgentWorkflowContext<T> context) {
        ChatContext chatContext = context.getChatContext();
        ChatRequest.Builder builder = chatContext.prepareChatRequest();
        return builder.build();
    }
    
    /**
     * 获取Strand客户端
     */
    protected <T> ChatLanguageModel getStrandClient(AgentWorkflowContext<T> context) {
        return llmServiceFactory.getStrandClient(
                context.getChatContext().getProvider(),
                context.getChatContext().getModel());
    }
}
