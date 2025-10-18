package org.xhy.application.conversation.service.message;

import dev.langchain4j.rag.content.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.application.billing.service.BillingService;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.application.conversation.service.handler.context.TracingChatContext;
import org.xhy.application.conversation.service.message.builtin.BuiltInToolRegistry;
import org.xhy.application.conversation.service.ChatSessionManager;
import org.xhy.application.trace.collector.TraceCollector;
import org.xhy.domain.agent.model.AgentEntity;
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
import org.xhy.domain.user.service.AccountDomainService;
import org.xhy.domain.user.service.UserSettingsDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolProvider;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** 带追踪功能的消息处理器基类 在关键节点集成链路追踪逻辑
 * 
 * 线程上下文传递说明： - 使用 InheritableThreadLocal 将追踪上下文传递到子线程 - 适用于直接创建子线程的场景（如 tokenStream 回调）
 * 
 * 重要警告 - 线程池环境： 如果项目中引入了线程池（如 @Async、ThreadPoolExecutor、CompletableFuture 等）， InheritableThreadLocal 会导致线程复用时的上下文污染问题。
 * 
 * 线程池场景解决方案： 请使用阿里巴巴的 TransmittableThreadLocal (TTL) 替代： 1. 添加依赖：com.alibaba:transmittable-thread-local 2. 将
 * InheritableThreadLocal 替换为 TransmittableThreadLocal 3. 使用 TtlExecutors.getTtlExecutor() 包装线程池
 * 参考文档：https://github.com/alibaba/transmittable-thread-local
 *
 * 但是目前使用了 langchan4j 的 tokenStream，内置的线程池，不方便改，就算了 */
public abstract class TracingMessageHandler extends AbstractMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(TracingMessageHandler.class);

    protected final TraceCollector traceCollector;

    /** 当前请求的追踪上下文 - 使用InheritableThreadLocal支持子线程继承 */
    private static final InheritableThreadLocal<TraceContext> currentTraceContext = new InheritableThreadLocal<>();

    public TracingMessageHandler(LLMServiceFactory llmServiceFactory, MessageDomainService messageDomainService,
            HighAvailabilityDomainService highAvailabilityDomainService, SessionDomainService sessionDomainService,
            UserSettingsDomainService userSettingsDomainService, LLMDomainService llmDomainService,
            BuiltInToolRegistry builtInToolRegistry, BillingService billingService,
            AccountDomainService accountDomainService, ChatSessionManager chatSessionManager,
            TraceCollector traceCollector) {
        super(llmServiceFactory, messageDomainService, highAvailabilityDomainService, sessionDomainService,
                userSettingsDomainService, llmDomainService, builtInToolRegistry, billingService, accountDomainService,
                chatSessionManager);
        this.traceCollector = traceCollector;
    }

    @Override
    protected void onChatStart(ChatContext chatContext) {

        try {
            // 获取或开始会话级别的执行追踪
            TraceContext traceContext = traceCollector.getOrStartExecution(chatContext.getUserId(),
                    chatContext.getSessionId(), chatContext.getAgent().getId(), chatContext.getUserMessage(),
                    MessageType.TEXT.name());

            // 将追踪上下文保存到InheritableThreadLocal中
            currentTraceContext.set(traceContext);

            // 如果chatContext是TracingChatContext，设置追踪上下文
            if (chatContext instanceof TracingChatContext) {
                chatContext.setTraceContext(traceContext);
            }
        } catch (Exception e) {
            logger.error("❌ [TRACE-DEBUG] 启动对话追踪失败: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void onUserMessageProcessed(ChatContext chatContext, MessageEntity userMessage) {
        // 用户消息已经在 startExecution 中记录，此处可以记录额外信息
        TraceContext traceContext = getCurrentTraceContext();
        if (traceContext != null && traceContext.isTraceEnabled()) {
            logger.debug("用户消息已处理 - TraceId: {}, 消息长度: {}", traceContext.getTraceId(),
                    userMessage.getContent().length());
        }
    }

    @Override
    protected void onModelCallCompleted(ChatContext chatContext, ChatResponse chatResponse,
            ModelCallInfo modelCallInfo) {
        TraceContext traceContext = getCurrentTraceContext();
        if (traceContext != null && traceContext.isTraceEnabled()) {
            try {
                // 更新用户消息的Token数量（两阶段处理的第二阶段）
                if (modelCallInfo.getInputTokens() != null) {
                    traceCollector.updateUserMessageTokens(traceContext, modelCallInfo.getInputTokens());
                }

                // 记录模型调用和AI响应
                String aiResponse = chatResponse.aiMessage().text();
                traceCollector.recordModelCall(traceContext, aiResponse, modelCallInfo);

                logger.debug("模型调用完成 - TraceId: {}, 输入Token: {}, 输出Token: {}", traceContext.getTraceId(),
                        modelCallInfo.getInputTokens(), modelCallInfo.getOutputTokens());
            } catch (Exception e) {
                logger.warn("记录模型调用信息失败: {}", e.getMessage());
            }
        }
    }

    @Override
    protected void onToolCallCompleted(ChatContext chatContext, ToolCallInfo toolCallInfo) {
        TraceContext traceContext = getCurrentTraceContext();
        if (traceContext != null && traceContext.isTraceEnabled()) {
            try {
                // 记录工具调用
                traceCollector.recordToolCall(traceContext, toolCallInfo);

                logger.debug("工具调用完成 - TraceId: {}, 工具名称: {}", traceContext.getTraceId(), toolCallInfo.getToolName());
            } catch (Exception e) {
                logger.warn("记录工具调用信息失败: {}", e.getMessage());
            }
        }
    }

    @Override
    protected void onChatCompleted(ChatContext chatContext, boolean success, String errorMessage) {
        // 先调用父类钩子（执行记忆抽取与写入等通用逻辑）
        super.onChatCompleted(chatContext, success, errorMessage);
        TraceContext traceContext = getCurrentTraceContext();
        if (traceContext != null && traceContext.isTraceEnabled()) {
            try {
                if (success) {
                    traceCollector.recordSuccess(traceContext);
                    logger.debug("对话完成 - TraceId: {}, 状态: 成功", traceContext.getTraceId());
                } else {
                    traceCollector.recordFailure(traceContext, ExecutionPhase.RESULT_PROCESSING, errorMessage);
                    logger.debug("对话完成 - TraceId: {}, 状态: 失败, 错误: {}", traceContext.getTraceId(), errorMessage);
                }
            } catch (Exception e) {
                logger.warn("完成对话追踪失败: {}", e.getMessage());
            } finally {
                // 清理ThreadLocal，防止内存泄漏
                currentTraceContext.remove();
            }
        } else {
            // 即使没有追踪上下文，也要清理ThreadLocal
            currentTraceContext.remove();
        }
    }

    @Override
    protected void onChatError(ChatContext chatContext, ExecutionPhase errorPhase, Throwable throwable) {
        TraceContext traceContext = getCurrentTraceContext();
        if (traceContext != null && traceContext.isTraceEnabled()) {
            try {
                // 记录汇总表的失败状态（现有逻辑）
                traceCollector.recordFailure(traceContext, errorPhase, throwable);

                // 记录异常详情到详细记录表（新增逻辑）
                traceCollector.recordErrorDetail(traceContext, errorPhase, throwable);

                logger.debug("对话异常 - TraceId: {}, 阶段: {}, 异常: {}", traceContext.getTraceId(),
                        errorPhase.getDescription(), throwable.getMessage());
            } catch (Exception e) {
                logger.warn("记录对话异常失败: {}", e.getMessage());
            }
        }
    }

    /** 获取当前线程的追踪上下文
     * 
     * @return 追踪上下文，可能为null */
    protected TraceContext getCurrentTraceContext() {
        return currentTraceContext.get();
    }

    /** 将ChatContext包装为TracingChatContext
     * 
     * @param chatContext 原始上下文
     * @return 追踪上下文 */
    protected TracingChatContext wrapWithTracingContext(ChatContext chatContext) {
        if (chatContext instanceof TracingChatContext) {
            return (TracingChatContext) chatContext;
        }

        TracingChatContext tracingContext = TracingChatContext.from(chatContext);
        TraceContext traceContext = getCurrentTraceContext();
        if (traceContext != null) {
            tracingContext.setTraceContext(traceContext);
        }
        return tracingContext;
    }

    @Override
    protected Agent buildStreamingAgent(StreamingChatModel model, MessageWindowChatMemory memory,
            ToolProvider toolProvider, AgentEntity agent, org.xhy.application.conversation.service.handler.context.ChatContext chatContext) {

        // 调用父类方法，获取原始 Agent
        Agent originalAgent = super.buildStreamingAgent(model, memory, toolProvider, agent, chatContext);

        // 捕获当前线程的 TraceContext
        TraceContext currentTrace = getCurrentTraceContext();

        // 返回包装后的 Agent
        return new TracingAgentWrapper(originalAgent, currentTrace);
    }

    /** 带追踪功能的 Agent 包装器 */
    private class TracingAgentWrapper implements Agent {
        private final Agent originalAgent;
        private final TraceContext capturedTraceContext;

        public TracingAgentWrapper(Agent originalAgent, TraceContext traceContext) {
            this.originalAgent = originalAgent;
            this.capturedTraceContext = traceContext;
        }

        @Override
        public TokenStream chat(String message) {
            // 调用原始 Agent 的 chat 方法
            TokenStream originalTokenStream = originalAgent.chat(message);

            // 返回包装后的 TokenStream
            return new TracingTokenStreamWrapper(originalTokenStream, capturedTraceContext);
        }
    }

    /** 带追踪功能的 TokenStream 包装器 */
    private class TracingTokenStreamWrapper implements TokenStream {
        private final TokenStream originalStream;
        private final TraceContext capturedTraceContext;

        public TracingTokenStreamWrapper(TokenStream originalStream, TraceContext traceContext) {
            this.originalStream = originalStream;
            this.capturedTraceContext = traceContext;
        }

        @Override
        public TokenStream onCompleteResponse(Consumer<ChatResponse> responseHandler) {
            // 包装原始的 responseHandler
            Consumer<ChatResponse> wrappedHandler = response -> {
                // 在回调开始时设置 TraceContext
                if (capturedTraceContext != null) {
                    currentTraceContext.set(capturedTraceContext);
                }
                try {
                    // 调用原始处理器
                    responseHandler.accept(response);
                } finally {
                    // 清理 ThreadLocal
                    currentTraceContext.remove();
                }
            };

            // 调用原始 TokenStream 的方法
            return originalStream.onCompleteResponse(wrappedHandler);
        }

        @Override
        public TokenStream onPartialReasoning(Consumer<String> consumer) {
            return null;
        }

        @Override
        public TokenStream onCompleteReasoning(Consumer<String> consumer) {
            return null;
        }

        @Override
        public TokenStream onReasoningDetected(BiFunction<String, Object, Boolean> biFunction, String s) {
            return null;
        }

        @Override
        public TokenStream onToolExecuted(Consumer<ToolExecution> toolExecutionHandler) {
            // 类似的包装逻辑
            Consumer<ToolExecution> wrappedHandler = toolExecution -> {
                if (capturedTraceContext != null) {
                    currentTraceContext.set(capturedTraceContext);
                }
                try {
                    toolExecutionHandler.accept(toolExecution);
                } finally {
                    currentTraceContext.remove();
                }
            };

            return originalStream.onToolExecuted(wrappedHandler);
        }

        @Override
        public TokenStream onError(Consumer<Throwable> errorHandler) {
            Consumer<Throwable> wrappedHandler = throwable -> {
                if (capturedTraceContext != null) {
                    currentTraceContext.set(capturedTraceContext);
                }
                try {
                    errorHandler.accept(throwable);
                } finally {
                    currentTraceContext.remove();
                }
            };

            return originalStream.onError(wrappedHandler);
        }

        @Override
        public TokenStream ignoreErrors() {
            return null;
        }

        @Override
        public TokenStream onPartialResponse(Consumer<String> partialResponseHandler) {
            Consumer<String> wrappedHandler = partialResponse -> {
                if (capturedTraceContext != null) {
                    currentTraceContext.set(capturedTraceContext);
                }
                try {
                    partialResponseHandler.accept(partialResponse);
                } finally {
                    currentTraceContext.remove();
                }
            };

            return originalStream.onPartialResponse(wrappedHandler);
        }

        @Override
        public TokenStream onRetrieved(Consumer<List<Content>> consumer) {
            return null;
        }

        @Override
        public void start() {
            originalStream.start();
        }
    }
}
