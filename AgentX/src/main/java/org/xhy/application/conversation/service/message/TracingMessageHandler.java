package org.xhy.application.conversation.service.message;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolProvider;
import org.xhy.application.conversation.service.handler.Agent;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.application.conversation.service.handler.context.TracingChatContext;
import org.xhy.application.conversation.service.message.agent.tool.RagToolManager;
import org.xhy.application.trace.collector.TraceCollector;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.domain.conversation.service.SessionDomainService;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.trace.constant.ExecutionPhase;
import org.xhy.domain.trace.model.ModelCallInfo;
import org.xhy.domain.trace.model.ToolCallInfo;
import org.xhy.domain.trace.model.TraceContext;
import org.xhy.domain.user.service.UserSettingsDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** 带追踪功能的消息处理器基类 在关键节点集成链路追踪逻辑 */
public abstract class TracingMessageHandler extends AbstractMessageHandler {

    protected final TraceCollector traceCollector;

    public TracingMessageHandler(LLMServiceFactory llmServiceFactory, MessageDomainService messageDomainService,
            HighAvailabilityDomainService highAvailabilityDomainService, SessionDomainService sessionDomainService,
            UserSettingsDomainService userSettingsDomainService, LLMDomainService llmDomainService,
            RagToolManager ragToolManager, TraceCollector traceCollector) {
        super(llmServiceFactory, messageDomainService, highAvailabilityDomainService, sessionDomainService,
                userSettingsDomainService, llmDomainService, ragToolManager);
        this.traceCollector = traceCollector;
    }

    /** 重写chat方法，增加追踪逻辑 */
    @Override
    public <T> T chat(ChatContext chatContext, MessageTransport<T> transport) {
        // 开始执行追踪并转换为TracingChatContext
        TraceContext traceContext = startTracing(chatContext);
        TracingChatContext tracingContext = TracingChatContext.from(chatContext);
        tracingContext.setTraceContext(traceContext);

        try {
            // 执行原有的chat逻辑
            T result = super.chat(tracingContext, transport);

            // 记录执行成功
            traceCollector.recordSuccess(traceContext);

            return result;
        } catch (Exception e) {
            // 记录执行失败
            traceCollector.recordFailure(traceContext, ExecutionPhase.MODEL_CALL, e);
            throw e;
        }
    }

    /** 重写同步聊天处理，增加详细追踪 */
    @Override
    protected <T> void processSyncChat(ChatContext chatContext, T connection, MessageTransport<T> transport,
            MessageEntity userEntity, MessageEntity llmEntity,
            dev.langchain4j.memory.chat.MessageWindowChatMemory memory, ToolProvider toolProvider) {

        TraceContext traceContext = getTraceContext(chatContext);

        try {
            // 1. 获取同步LLM客户端
            var syncClient = llmServiceFactory.getStrandClient(chatContext.getProvider(), chatContext.getModel());

            // 2. 保存用户消息
            messageDomainService.saveMessageAndUpdateContext(java.util.Collections.singletonList(userEntity),
                    chatContext.getContextEntity());

            // 3. 记录调用开始时间
            long startTime = System.currentTimeMillis();

            java.util.List<ChatMessage> messages = memory.messages();
            messages.add(new dev.langchain4j.data.message.UserMessage(chatContext.getUserMessage()));
            ChatResponse chatResponse = syncClient.chat(messages);

            // 4. 计算调用时间和构建模型调用信息
            long endTime = System.currentTimeMillis();
            int callTime = (int) (endTime - startTime);

            ModelCallInfo modelCallInfo = ModelCallInfo.builder().modelId(chatContext.getModel().getModelId())
                    .providerName(chatContext.getProvider().getName())
                    .inputTokens(chatResponse.tokenUsage().inputTokenCount())
                    .outputTokens(chatResponse.tokenUsage().outputTokenCount()).callTime(callTime).success(true)
                    .build();

            // 5. 记录模型调用
            String responseText = chatResponse.aiMessage().text();
            traceCollector.recordModelCall(traceContext, responseText, modelCallInfo);

            // 6. 处理响应 - 设置消息内容
            llmEntity.setContent(responseText);
            llmEntity.setTokenCount(chatResponse.tokenUsage().outputTokenCount());
            userEntity.setTokenCount(chatResponse.tokenUsage().inputTokenCount());

            // 7. 保存消息
            messageDomainService.updateMessage(userEntity);
            messageDomainService.saveMessageAndUpdateContext(java.util.Collections.singletonList(llmEntity),
                    chatContext.getContextEntity());

            // 8. 发送完整响应
            var response = new org.xhy.application.conversation.dto.AgentChatResponse(responseText, true);
            response.setMessageType(MessageType.TEXT);
            transport.sendEndMessage(connection, response);

            // 9. 上报调用成功结果
            highAvailabilityDomainService.reportCallResult(chatContext.getInstanceId(), chatContext.getModel().getId(),
                    true, callTime, null);

        } catch (Exception e) {
            // 创建失败的模型调用信息
            ModelCallInfo failedModelCallInfo = ModelCallInfo.builder().modelId(chatContext.getModel().getModelId())
                    .providerName(chatContext.getProvider().getName()).success(false).errorMessage(e.getMessage())
                    .build();

            // 记录失败的模型调用
            traceCollector.recordModelCall(traceContext, null, failedModelCallInfo);

            // 错误处理
            var errorResponse = org.xhy.application.conversation.dto.AgentChatResponse.buildEndMessage(e.getMessage(),
                    MessageType.TEXT);
            transport.sendMessage(connection, errorResponse);

            long latency = System.currentTimeMillis() - System.currentTimeMillis();
            highAvailabilityDomainService.reportCallResult(chatContext.getInstanceId(), chatContext.getModel().getId(),
                    false, latency, e.getMessage());
        }
    }

    /** 重写流式聊天处理的TokenStream，增加追踪逻辑 */
    protected <T> void processChat(Agent agent, T connection, MessageTransport<T> transport, ChatContext chatContext,
            MessageEntity userEntity, MessageEntity llmEntity) {

        TraceContext traceContext = getTraceContext(chatContext);

        messageDomainService.saveMessageAndUpdateContext(java.util.Collections.singletonList(userEntity),
                chatContext.getContextEntity());

        java.util.concurrent.atomic.AtomicReference<StringBuilder> messageBuilder = new java.util.concurrent.atomic.AtomicReference<>(
                new StringBuilder());
        TokenStream tokenStream = agent.chat(chatContext.getUserMessage());

        // 记录调用开始时间
        long startTime = System.currentTimeMillis();

        tokenStream.onError(throwable -> {
            // 记录失败的模型调用
            ModelCallInfo failedModelCallInfo = ModelCallInfo.builder().modelId(chatContext.getModel().getModelId())
                    .providerName(chatContext.getProvider().getName()).success(false)
                    .errorMessage(throwable.getMessage()).build();

            traceCollector.recordModelCall(traceContext, null, failedModelCallInfo);

            transport.sendMessage(connection, org.xhy.application.conversation.dto.AgentChatResponse
                    .buildEndMessage(throwable.getMessage(), MessageType.TEXT));

            // 上报调用失败结果
            long latency = System.currentTimeMillis() - startTime;
            highAvailabilityDomainService.reportCallResult(chatContext.getInstanceId(), chatContext.getModel().getId(),
                    false, latency, throwable.getMessage());
        });

        // 部分响应处理
        tokenStream.onPartialResponse(reply -> {
            messageBuilder.get().append(reply);
            // 删除换行后消息为空字符串
            if (messageBuilder.get().toString().trim().isEmpty()) {
                return;
            }
            transport.sendMessage(connection,
                    org.xhy.application.conversation.dto.AgentChatResponse.build(reply, MessageType.TEXT));
        });

        // 完整响应处理
        tokenStream.onCompleteResponse(chatResponse -> {
            // 计算调用时间和构建模型调用信息
            long endTime = System.currentTimeMillis();
            int callTime = (int) (endTime - startTime);

            ModelCallInfo modelCallInfo = ModelCallInfo.builder().modelId(chatContext.getModel().getModelId())
                    .providerName(chatContext.getProvider().getName())
                    .inputTokens(chatResponse.tokenUsage().inputTokenCount())
                    .outputTokens(chatResponse.tokenUsage().outputTokenCount()).callTime(callTime).success(true)
                    .build();

            // 记录模型调用
            traceCollector.recordModelCall(traceContext, chatResponse.aiMessage().text(), modelCallInfo);

            // 更新token信息
            llmEntity.setTokenCount(chatResponse.tokenUsage().outputTokenCount());
            llmEntity.setContent(chatResponse.aiMessage().text());

            userEntity.setTokenCount(chatResponse.tokenUsage().inputTokenCount());
            messageDomainService.updateMessage(userEntity);

            // 保存AI消息
            messageDomainService.saveMessageAndUpdateContext(java.util.Collections.singletonList(llmEntity),
                    chatContext.getContextEntity());

            // 发送结束消息
            transport.sendEndMessage(connection,
                    org.xhy.application.conversation.dto.AgentChatResponse.buildEndMessage(MessageType.TEXT));

            // 上报调用成功结果
            highAvailabilityDomainService.reportCallResult(chatContext.getInstanceId(), chatContext.getModel().getId(),
                    true, callTime, null);
            smartRenameSession(chatContext);
        });

        // 工具执行处理
        tokenStream.onToolExecuted(toolExecution -> {
            // 记录工具调用开始时间
            long toolStartTime = System.currentTimeMillis();

            if (!messageBuilder.get().isEmpty()) {
                transport.sendMessage(connection,
                        org.xhy.application.conversation.dto.AgentChatResponse.buildEndMessage(MessageType.TEXT));
                llmEntity.setContent(messageBuilder.toString());
                messageDomainService.saveMessageAndUpdateContext(java.util.Collections.singletonList(llmEntity),
                        chatContext.getContextEntity());
                messageBuilder.set(new StringBuilder());
            }

            // 计算工具执行时间
            long toolEndTime = System.currentTimeMillis();
            int toolExecutionTime = (int) (toolEndTime - toolStartTime);

            // 构建工具调用信息
            ToolCallInfo toolCallInfo = ToolCallInfo.builder().toolName(toolExecution.request().name())
                    .requestArgs(formatToolArguments(toolExecution.request())).responseData(toolExecution.result())
                    .executionTime(toolExecutionTime).success(true).build();

            // 记录工具调用
            traceCollector.recordToolCall(traceContext, toolCallInfo);

            String message = "执行工具：" + toolExecution.request().name();
            MessageEntity toolMessage = createLlmMessage(chatContext);
            toolMessage.setMessageType(MessageType.TOOL_CALL);
            toolMessage.setContent(message);
            messageDomainService.saveMessageAndUpdateContext(java.util.Collections.singletonList(toolMessage),
                    chatContext.getContextEntity());

            transport.sendMessage(connection, org.xhy.application.conversation.dto.AgentChatResponse
                    .buildEndMessage(message, MessageType.TOOL_CALL));
        });

        // 启动流处理
        tokenStream.start();
    }

    /** 开始追踪 */
    private TraceContext startTracing(ChatContext chatContext) {
        return traceCollector.startExecution(getUserId(chatContext), chatContext.getSessionId(),
                chatContext.getAgent().getId(), chatContext.getUserMessage(), MessageType.TEXT.name());
    }

    /** 从ChatContext中获取TraceContext */
    private TraceContext getTraceContext(ChatContext chatContext) {
        // 这里假设ChatContext中已经设置了TraceContext
        // 如果没有，则创建一个禁用的上下文
        if (chatContext instanceof TracingChatContext) {
            return ((TracingChatContext) chatContext).getTraceContext();
        }
        return TraceContext.createDisabled();
    }

    /** 从ChatContext中获取用户ID */
    private String getUserId(ChatContext chatContext) {
        return chatContext.getUserId();
    }

    /** 格式化工具参数为JSON字符串 */
    private String formatToolArguments(ToolExecutionRequest request) {
        try {
            return request.arguments();
        } catch (Exception e) {
            return "{}";
        }
    }

}