package org.xhy.domain.conversation.handler;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.domain.task.constant.TaskStatus;
import org.xhy.domain.task.model.TaskEntity;
import org.xhy.domain.task.service.TaskDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;
import org.xhy.application.task.dto.TaskDTO;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Agent消息处理器
 * 用于支持工具调用的对话模式
 * 实现任务拆分、执行和结果汇总的流程
 */
@Component(value = "agentMessageHandler")
public class AgentMessageHandler extends ChatMessageHandler {

    private final TaskDomainService taskDomainService;

    // 任务拆分提示词 测试过程用的
    String decompositionTestPrompt =
            "你是一个专业的任务规划专家，请根据用户的需求，将复杂任务分解为合理的子任务序列,只拆分为3条子任务。请分解为合理的子任务序列，直接以数字编号的形式列出，无需额外解释";

    // 任务拆分提示词
    String decompositionPrompt =
            "你是一个专业的任务规划专家，请根据用户的需求，将复杂任务分解为合理的子任务序列。" +
                    "在分解任务时，请考虑以下几点：" +
                    "\n1. 充分理解用户的真实需求和背景，挖掘潜在的子任务" +
                    "\n2. 子任务应该覆盖问题解决的整个过程，确保完整性" +
                    "\n3. 根据任务的复杂度，决定合适的子任务粒度和数量" +
                    "\n4. 子任务应按照合理的顺序排列，确保执行的流畅性" +
                    "\n5. 子任务描述应面向用户，清晰易懂，避免技术术语" +
                    "\n6. 创造性地考虑用户可能忽略的方面，提供全面的规划" +
                    "\n\n以下是用户的需求：" +
                    "\n\n请分解为合理的子任务序列，直接以数字编号的形式列出，无需额外解释。";

    // 任务结果汇总提示词
    private final String summaryPrompt = "你是一个高效的任务结果整合专家。我已经完成了以下子任务，请根据这些子任务的结果，提供一个全面、连贯且条理清晰的总结。" +
            "总结应该：" +
            "\n1. 融合所有子任务的关键信息" +
            "\n2. 消除重复内容" +
            "\n3. 使用简洁明了的语言" +
            "\n4. 保持专业性和准确性" +
            "\n5. 按逻辑顺序组织内容" +
            "\n\n以下是各子任务及其结果：\n%s" +
            "\n\n请提供一个全面的总结:";

    // Agent接口定义
    public interface Agent {
        AiMessage chat(String prompt);
    }

    public AgentMessageHandler(
            ConversationDomainService conversationDomainService,
            ContextDomainService contextDomainService,
            LLMServiceFactory llmServiceFactory, 
            TaskDomainService taskDomainService) {
        super(conversationDomainService, contextDomainService, llmServiceFactory);
        this.taskDomainService = taskDomainService;
    }

    private List<String> getTools(){
        return Arrays.asList("http://124.220.234.136:8006/time/sse");
    }

    @Override
    public <T> T handleChat(ChatEnvironment environment, MessageTransport<T> messageTransport) {
        // 创建用户消息实体
        MessageEntity userMessageEntity = this.createUserMessage(environment);

        // 创建LLM消息实体
        MessageEntity llmMessageEntity = createLlmMessage(environment);

        // 创建连接
        T connection = messageTransport.createConnection(CONNECTION_TIMEOUT);

        // 获取LLM客户端
        StreamingChatLanguageModel streamingClient = llmServiceFactory.getStreamingClient(
                environment.getProvider(), environment.getModel());

        // 第一步：创建父任务
        TaskEntity parentTask = new TaskEntity();
        parentTask.setTaskName(environment.getUserMessage());
        parentTask.setSessionId(environment.getSessionId());
        parentTask.setUserId(environment.getUserId());
        parentTask.setParentTaskId("0");
        parentTask.setProgress(0);
        parentTask.setStatus(TaskStatus.IN_PROGRESS);

        // 保存父任务
        taskDomainService.addTask(parentTask);
        
        // 第二步：任务拆分（流式响应）
        ChatRequest splitTaskRequest = prepareSplitTaskRequest(environment);
        List<String> tasks = new ArrayList<>();
        Map<String, TaskEntity> subTaskMap = new HashMap<>();
        
        // 创建一个信号量用于同步
        CountDownLatch taskDescLatch = new CountDownLatch(1);
        
        getTaskDesc(streamingClient, splitTaskRequest, connection, messageTransport, 
                environment, userMessageEntity, llmMessageEntity, parentTask, subTaskMap, tasks, taskDescLatch);
        
        // 等待任务拆分完成
        try {
            boolean completed = taskDescLatch.await(300, TimeUnit.SECONDS); // 设置合理的超时时间
            if (!completed) {
                // 等待超时，可能需要处理
                // 向前端发送超时消息
                AgentChatResponse timeoutResponse = AgentChatResponse.build(
                        "任务拆分超时", true, MessageType.TEXT);
                messageTransport.sendMessage(connection, timeoutResponse);
                return connection;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 向前端发送中断消息
            AgentChatResponse interruptResponse = AgentChatResponse.build(
                    "任务拆分被中断", true, MessageType.TEXT);
            messageTransport.sendMessage(connection, interruptResponse);
            return connection;
        }
        
        // 保存用户消息和LLM拆分结果消息
        conversationDomainService.insertBathMessage(Arrays.asList(userMessageEntity, llmMessageEntity));
        
        // 更新上下文
        List<String> activeMessages = environment.getContextEntity().getActiveMessages();
        activeMessages.add(userMessageEntity.getId());
        activeMessages.add(llmMessageEntity.getId());
        contextDomainService.insertOrUpdate(environment.getContextEntity());
        
        // 创建一个线程池来异步执行任务
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // 异步执行子任务和汇总
        executor.submit(() -> {
            try {
                // 第三步：配置MCP工具
                List<String> toolUrls = getTools();
                ToolProvider toolProvider = null;
                
                if (!toolUrls.isEmpty()) {
                    List<McpClient> mcpClients = new ArrayList<>();
                    
                    for (String toolUrl : toolUrls) {
                        McpTransport transport = new HttpMcpTransport.Builder()
                                .sseUrl(toolUrl)
                                .logRequests(true)
                                .logResponses(true)
                                .build();
                        
                        McpClient mcpClient = new DefaultMcpClient.Builder()
                                .transport(transport)
                                .build();
                        
                        mcpClients.add(mcpClient);
                    }
                    
                    toolProvider = McpToolProvider.builder()
                            .mcpClients(mcpClients)
                            .build();
                }
                
                // 第四步：执行子任务
                Map<String, String> subTaskResults = new LinkedHashMap<>();
                AtomicInteger completedTasks = new AtomicInteger(0);
                int totalTasks = tasks.size();
                
                for (int i = 0; i < tasks.size(); i++) {
                    String task = tasks.get(i);
                    TaskEntity subTask = subTaskMap.get(task);
                    
                    // 更新任务状态为进行中
                    subTask.updateStatus(TaskStatus.IN_PROGRESS);
                    taskDomainService.updateTask(subTask);

                    // 保存执行消息
                    MessageEntity taskCallMessageEntity = createLlmMessage(environment);
                    taskCallMessageEntity.setMessageType(MessageType.TASK_EXEC);
                    taskCallMessageEntity.setContent("执行任务："+task);
                    taskCallMessageEntity.setTokenCount(0);
                    conversationDomainService.saveMessage(taskCallMessageEntity);


                    // 通知前端当前执行的任务
                    AgentChatResponse taskExecNotification = AgentChatResponse.build(
                            task, false, MessageType.TASK_EXEC);
                    taskExecNotification.setTaskId(subTask.getId());
                    messageTransport.sendMessage(connection, taskExecNotification);
                    
                    // 构建子任务上下文（包含前面子任务的结果）
                    StringBuilder contextBuilder = new StringBuilder();
                    contextBuilder.append("当前任务: ").append(task).append("\n\n");
                    
                    if (!subTaskResults.isEmpty()) {
                        contextBuilder.append("前面完成的子任务及结果:\n");
                        for (Map.Entry<String, String> entry : subTaskResults.entrySet()) {
                            contextBuilder.append("- 任务: ").append(entry.getKey())
                                    .append("\n  结果: ").append(entry.getValue())
                                    .append("\n");
                        }
                        contextBuilder.append("\n");
                    }
                    
                    String taskWithContext = contextBuilder.toString();
                    String taskResult;
                    
                    // 执行子任务，始终带上工具提供者
                    ChatLanguageModel model = llmServiceFactory.getStrandClient(
                            environment.getProvider(), environment.getModel());
                    
                    Agent agent = AiServices.builder(Agent.class)
                            .chatLanguageModel(model)
                            .toolProvider(toolProvider)
                            .build();
                    
                    AiMessage aiMessage = agent.chat(taskWithContext);
                    
                    // 处理工具调用
                    if (aiMessage.hasToolExecutionRequests()) {
                        // 创建工具调用消息实体
                        MessageEntity toolCallMessageEntity = createLlmMessage(environment);
                        toolCallMessageEntity.setMessageType(MessageType.TOOL_CALL);
                        StringBuilder toolCallsContent = new StringBuilder("工具调用:\n");
                        
                        aiMessage.toolExecutionRequests().forEach(toolExecutionRequest -> {
                            String toolName = toolExecutionRequest.name();
                            toolCallsContent.append("- ").append(toolName).append("\n");
                            
                            AgentChatResponse toolCallResponse = AgentChatResponse.build(
                                    toolName, false, MessageType.TOOL_CALL);
                            toolCallResponse.setTaskId(subTask.getId());
                            messageTransport.sendMessage(connection, toolCallResponse);
                        });
                        
                        // 设置工具调用内容并保存
                        toolCallMessageEntity.setContent(toolCallsContent.toString());
                        toolCallMessageEntity.setTokenCount(0);
                        conversationDomainService.saveMessage(toolCallMessageEntity);
                        
                        // 更新上下文
                        activeMessages.add(toolCallMessageEntity.getId());
                    }
                    
                    taskResult = aiMessage.text();
                    
                    // 子任务结果不再保存到消息表，因为这是过程消息
                    
                    // 保存子任务结果
                    subTaskResults.put(task, taskResult);
                    subTask.setTaskResult(taskResult);
                    
                    // 更新子任务状态为已完成
                    subTask.updateStatus(TaskStatus.COMPLETED);
                    taskDomainService.updateTask(subTask);
                    
                    // 更新父任务进度
                    int completedCount = completedTasks.incrementAndGet();
                    int progress = (int) ((completedCount / (double) totalTasks) * 100);
                    parentTask.updateProgress(progress);
                    taskDomainService.updateTask(parentTask);
                    
                    // 通知前端任务进度
                    AgentChatResponse progressNotification = AgentChatResponse.build(
                            String.valueOf(progress), false, MessageType.TASK_STATUS);
                    progressNotification.setTaskId(parentTask.getId());
                    messageTransport.sendMessage(connection, progressNotification);
                }

                // 第五步：总结结果（流式响应）
                StringBuilder taskSummaryBuilder = new StringBuilder();
                for (Map.Entry<String, String> entry : subTaskResults.entrySet()) {
                    taskSummaryBuilder.append("任务: ").append(entry.getKey())
                            .append("\n结果: ").append(entry.getValue())
                            .append("\n\n");
                }

                String taskSummary = taskSummaryBuilder.toString();
                System.out.println("结果："+taskSummaryBuilder);

                MessageEntity summaryMessageEntity = createLlmMessage(environment);
                
                summarizeResults(streamingClient, environment, taskSummary, connection, messageTransport, 
                        summaryMessageEntity, parentTask);
                
                // 保存总结消息
                conversationDomainService.saveMessage(summaryMessageEntity);
                
                // 更新上下文
                activeMessages.add(summaryMessageEntity.getId());
                contextDomainService.insertOrUpdate(environment.getContextEntity());
            } catch (Exception e) {
                // 处理异步执行过程中的异常
                String errorMessage = "任务执行过程中发生错误: " + e.getMessage();
                AgentChatResponse errorResponse = AgentChatResponse.build(
                        errorMessage, true, MessageType.TEXT);
                messageTransport.sendMessage(connection, errorResponse);
            } finally {
                // 关闭线程池
                executor.shutdown();
            }
        });
        // 立即返回连接，主线程不等待任务完成
        return connection;
    }

    /**
     * 将任务文本拆分为子任务列表
     */
    private List<String> splitTask(String task) {
        return Arrays.stream(task.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 获取任务描述并流式响应
     */
    private <T> void getTaskDesc(
            StreamingChatLanguageModel llmClient,
            ChatRequest llmRequest,
            T connection,
            MessageTransport<T> transport,
            ChatEnvironment environment,
            MessageEntity userMessageEntity,
            MessageEntity llmMessageEntity,
            TaskEntity parentTask,
            Map<String, TaskEntity> subTaskMap,
            List<String> tasks,
            CountDownLatch taskDescLatch) {

        llmClient.doChat(llmRequest, new StreamingChatResponseHandler() {
            StringBuilder fullResponse = new StringBuilder();
            
            @Override
            public void onPartialResponse(String partialResponse) {
                fullResponse.append(partialResponse);
                
                // 发送流式响应给前端
                AgentChatResponse response = AgentChatResponse.build(
                        partialResponse, false, MessageType.TEXT);
                transport.sendMessage(connection, response);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    // 设置token使用情况
                    TokenUsage tokenUsage = completeResponse.metadata().tokenUsage();
    
                    // 设置用户消息token数
                    Integer inputTokenCount = tokenUsage.inputTokenCount();
                    userMessageEntity.setTokenCount(inputTokenCount);
    
                    // 设置LLM消息内容和token数
                    Integer outputTokenCount = tokenUsage.outputTokenCount();
                    llmMessageEntity.setTokenCount(outputTokenCount);
                    
                    // 保存完整响应
                    String taskDesc = completeResponse.aiMessage().text();
                    llmMessageEntity.setContent(taskDesc);
                    
                    // 拆分子任务并创建任务实体
                    List<String> subTasks = splitTask(taskDesc);
                    tasks.addAll(subTasks); // 保存到外部列表中
                    List<TaskDTO> taskDTOList = new ArrayList<>();
                    
                    for (String task : subTasks) {
                        TaskEntity subTask = new TaskEntity();
                        subTask.setTaskName(task);
                        subTask.setSessionId(environment.getSessionId());
                        subTask.setUserId(environment.getUserId());
                        subTask.setParentTaskId(parentTask.getId());
                        subTask.updateStatus(TaskStatus.WAITING);
                        
                        // 保存子任务
                        taskDomainService.addTask(subTask);
                        
                        // 构建任务DTO
                        TaskDTO taskDTO = new TaskDTO();
                        taskDTO.setId(subTask.getId());
                        taskDTO.setTaskName(task);
                        taskDTO.setStatus(subTask.getStatus().name());
                        taskDTO.setProgress(subTask.getProgress());
                        taskDTO.setParentTaskId(parentTask.getId());
                        
                        taskDTOList.add(taskDTO);
                        
                        // 保存到Map中供后续使用
                        subTaskMap.put(task, subTask);
                    }
                    
                    // 构建包含任务列表的响应
                    AgentChatResponse tasksResponse = AgentChatResponse.build(
                            "", false, MessageType.TASK_IDS);
                    tasksResponse.setTaskId(parentTask.getId());
                    tasksResponse.setTasks(taskDTOList);
                    
                    // 发送任务列表响应
                    transport.sendMessage(connection, tasksResponse);
                    
                    // 发送完成消息
                    AgentChatResponse finishResponse = AgentChatResponse.build("", true, MessageType.TEXT);
                    transport.sendMessage(connection, finishResponse);
                } finally {
                    // 完成任务拆分，释放锁
                    taskDescLatch.countDown();
                }
            }

            @Override
            public void onError(Throwable error) {
                try {
                    transport.handleError(connection, error);
                } finally {
                    // 发生错误时也要释放锁，避免主线程永远等待
                    taskDescLatch.countDown();
                }
            }
        });
    }
    
    /**
     * 汇总子任务结果并流式响应
     */
    private <T> void summarizeResults(
            StreamingChatLanguageModel llmClient,
            ChatEnvironment environment,
            String taskResults,
            T connection,
            MessageTransport<T> transport,
            MessageEntity summaryMessageEntity,
            TaskEntity parentTask) {
            
        // 构建汇总请求
        ChatRequest.Builder requestBuilder = new ChatRequest.Builder();
        List<ChatMessage> messages = new ArrayList<>();
        
        // 添加系统提示词
        messages.add(new SystemMessage(String.format(summaryPrompt, taskResults)));
        
        // 添加用户消息
        messages.add(new UserMessage("请基于上述子任务结果提供总结"));
        
        // 构建请求参数
        OpenAiChatRequestParameters.Builder parameters = new OpenAiChatRequestParameters.Builder();
        parameters.modelName(environment.getModel().getModelId());
        parameters.topP(environment.getLlmModelConfig().getTopP())
                .temperature(environment.getLlmModelConfig().getTemperature());
                
        requestBuilder.messages(messages);
        requestBuilder.parameters(parameters.build());
        
        // 流式响应汇总结果
        llmClient.doChat(requestBuilder.build(), new StreamingChatResponseHandler() {
            StringBuilder fullSummary = new StringBuilder();
            
            @Override
            public void onPartialResponse(String partialResponse) {
                fullSummary.append(partialResponse);
                
                // 发送流式响应给前端
                AgentChatResponse response = AgentChatResponse.build(
                        partialResponse, false, MessageType.TEXT);
                transport.sendMessage(connection, response);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                // 设置LLM消息内容和token数
                TokenUsage tokenUsage = completeResponse.metadata().tokenUsage();
                Integer outputTokenCount = tokenUsage.outputTokenCount();
                
                String summary = completeResponse.aiMessage().text();
                summaryMessageEntity.setContent(summary);
                summaryMessageEntity.setTokenCount(outputTokenCount);
                
                // 更新父任务为完成状态
                parentTask.updateStatus(TaskStatus.COMPLETED);
                parentTask.setTaskResult(summary);
                taskDomainService.updateTask(parentTask);
                
                // 发送完成消息
                AgentChatResponse finishResponse = AgentChatResponse.build("", true, MessageType.TEXT);
                transport.sendMessage(connection, finishResponse);
                transport.completeConnection(connection);
            }

            @Override
            public void onError(Throwable error) {
                transport.handleError(connection, error);
            }
        });
    }

    /**
     * 准备任务拆分请求
     */
    protected ChatRequest prepareSplitTaskRequest(ChatEnvironment environment) {
        // 构建聊天消息列表
        List<ChatMessage> chatMessages = new ArrayList<>();
        ChatRequest.Builder chatRequestBuilder = new ChatRequest.Builder();

        // 添加任务拆分系统提示词
        String prompt = String.format(decompositionTestPrompt, environment.getUserMessage());
        chatMessages.add(new SystemMessage(prompt));

        // 添加当前用户消息
        chatMessages.add(new UserMessage(environment.getUserMessage()));

        // 构建请求参数
        OpenAiChatRequestParameters.Builder parameters = new OpenAiChatRequestParameters.Builder();
        parameters.modelName(environment.getModel().getModelId());
        parameters.topP(environment.getLlmModelConfig().getTopP())
                .temperature(environment.getLlmModelConfig().getTemperature());

        // 设置消息和参数
        chatRequestBuilder.messages(chatMessages);
        chatRequestBuilder.parameters(parameters.build());

        return chatRequestBuilder.build();
    }

    /**
     * 准备子任务请求
     */
    protected ChatRequest prepareTaskRequest(ChatEnvironment environment, String taskWithContext) {
        // 构建聊天消息列表
        List<ChatMessage> chatMessages = new ArrayList<>();
        ChatRequest.Builder chatRequestBuilder = new ChatRequest.Builder();

        // 添加系统提示词
        chatMessages.add(new SystemMessage("你是一个高效的任务执行助手，请基于给定的上下文和当前任务，完成当前任务。如果需要使用前面子任务的结果，请充分利用。"));

        // 添加当前任务描述
        chatMessages.add(new UserMessage(taskWithContext));

        // 构建请求参数
        OpenAiChatRequestParameters.Builder parameters = new OpenAiChatRequestParameters.Builder();
        parameters.modelName(environment.getModel().getModelId());
        parameters.topP(environment.getLlmModelConfig().getTopP())
                .temperature(environment.getLlmModelConfig().getTemperature());

        // 设置消息和参数
        chatRequestBuilder.messages(chatMessages);
        chatRequestBuilder.parameters(parameters.build());

        return chatRequestBuilder.build();
    }
}