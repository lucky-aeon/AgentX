package org.xhy.application.conversation.service.message.agent.template;


import java.util.Map;

/**
 * 提示词模板
 * 集中管理各种场景的提示词
 */
public class AgentPromptTemplates {

    private static final String SUMMARY_PREFIX = "以下是用户历史消息的摘要，请仅作为参考，用户没有提起则不要回答摘要中的内容：\\n";


    private static final String infoAnalysisPrompt =
            "你是一个任务规划助手。请先分析用户提出的目标或问题，并判断是否具备实现目标所必需的关键信息。\n" +
                    "要求：\n" +
                    "1. 如果用户的描述中包含了目标实现所需的所有核心信息，请回复“信息完整”。\n" +
                    "2. 如果用户描述中存在缺失关键信息的情况，请回复“信息不完整”，并具体指出缺少的内容，建议用户补充哪些信息以便完整描述需求。\n" +
                    "3. 如果用户明确表示不再补充信息，请设置“信息完整”并不返回缺失信息提示。\n" +
                    "请仅输出以下 JSON 格式：\n" +
                    "{\n" +
                    "    \"infoComplete\": true/false,\n" +
                    "    \"missingInfoPrompt\": \"若信息不完整，请在此说明缺失的关键信息；若信息完整，此项留空\"\n" +
                    "}";

    /**
     * 任务拆分提示词
     */
    private static String decompositionPrompt =
            "你是一个任务规划助手。你的职责是将用户提出的目标或问题拆解为若干清晰、独立的子任务，以方便后续处理。\n" +
                    "请按照合理的逻辑顺序列出这些子任务，每个子任务独占一行。每个子任务都应完整明确，避免与其他子任务重复或交叉。确保所有子任务加起来可以实现用户的整体目标，不遗漏任何关键步骤。如果需要，你可以根据目标的复杂程度自主决定子任务的数量。\n" +
                    "请直接输出子任务列表，不要提供额外的解释或引言。";
    /**
     * 任务执行提示词
     */
    private static final String taskExecutionPrompt =
            "你是一个任务执行助手。现在你的任务是根据给定的上下文完成一个特定的子任务。\n" +
                    "该上下文包括原始用户请求，以及之前已完成的子任务及其结果。\n\n" +
                    "原始用户请求: %s\n\n" +
                    "当前需要完成的子任务: %s\n\n" +
                    "%s" +
                    "请利用以上信息完成当前子任务，并提供详细结果。专注于当前任务，给出完整有用的回答。";

    /**
     * 任务结果汇总提示词
     */
    private static final String summaryPrompt =
            "你是一个总结助手，需要整合所有子任务的结果来回答用户的最终请求。\n" +
                    "你将获得原始用户请求，以及每个子任务及其完成结果。\n" +
                    "请将这些信息进行整合，提供一个针对用户请求的完整且连贯的回答。\n" +
                    "确保回答涵盖所有关键信息，逻辑清晰、表达通顺。\n" +
                    "直接给出最终结果，不需要提及子任务或汇总过程。\n\n" +
                    "子任务执行结果：\n%s";

    private static final String analyserMessagePrompt =
            "你是一个分析消息助手，你需要分析用户的消息是问答消息还是任务消息。\n" +
                    "问答消息是指用户提出的简单问题或对话，例如：\"你好\"、\"你是谁\"、\"今天的天气怎么样\"等，通常不需要进行复杂的处理。对于问答消息，你需要返回标识 `true`，并且在 `reply` 字段返回大模型对用户消息的完整回复。\n" +
                    "任务消息是指用户提出的需要大模型根据上下文进行规划、分析、设计等解决方案的消息，例如：\"帮我规划东莞一日游\"，\"Java八股文该如何学习\"等。对于任务消息，你只需要返回标识 `false`，不需要返回大模型的具体回复内容。\n" +
                    "返回的数据格式是一个 JSON 结构，包含两个字段：\n" +
                    "{\n" +
                    "   \"isQuestion\": boolean,   // 标识消息是否是问答消息，true表示问答消息，false表示任务消息\n" +
                    "   \"reply\": String          // 对于问答消息，返回大模型的完整回复；对于任务消息，留空\n" +
                    "}\n" +
                    "\n" +
                    "以下是一些示例：\n" +
                    "示例1:\n" +
                    "用户消息：\"你好\"\n" +
                    "输出：\n" +
            "{\\\"isQuestion\\\": true, \\\"reply\\\": \\\"你好！我是一个智能助手，我可以帮你解答问题，或者帮你规划任务！你有什么需要吗？\\\"}\n" +
                    "\n" +
                    "示例2:\n" +
                    "用户消息：\"帮我规划东莞一日游。\"\n" +
                    "输出：\n" +
                    "{\\\"isQuestion\\\": false, \\\"reply\\\": \\\"\\\"}\n" +
                    "\n" +
                    "请根据用户的消息，选择适当的回复，并返回格式化的 JSON 结果。"+
                    "用户消息是： %s。";


    public static String getInfoAnalysisPrompt() {
        return infoAnalysisPrompt;
    }

    public static String getAnalyserMessagePrompt(String userMessage) {
        return String.format(analyserMessagePrompt,userMessage);
    }

    /**
     * 获取摘要算法的提示词
     */
    public static String getSummaryPrefix() {
        return SUMMARY_PREFIX;
    }

    /**
     * 获取任务拆分提示词
     */
    public static String getDecompositionPrompt() {
        return decompositionPrompt;
    }

    /**
     * 获取带参数的任务执行提示词
     * 
     * @param userRequest 用户原始请求
     * @param currentTask 当前执行的子任务
     * @param previousTaskResults 之前子任务的结果
     * @return 填充了参数的提示词
     */
    public static String getTaskExecutionPrompt(
            String userRequest, 
            String currentTask, 
            Map<String, String> previousTaskResults) {
        
        StringBuilder previousTasksBuilder = new StringBuilder();
        if (previousTaskResults != null && !previousTaskResults.isEmpty()) {
            previousTasksBuilder.append("已完成的子任务及结果:\n");
            previousTaskResults.forEach((task, result) -> {
                previousTasksBuilder.append("- 任务: ").append(task)
                        .append("\n  结果: ").append(result)
                        .append("\n");
            });
            previousTasksBuilder.append("\n");
        }
        
        // 使用已定义的模板，填充参数
        return String.format(taskExecutionPrompt, 
                userRequest, 
                currentTask,
                previousTasksBuilder);
    }

    /**
     * 获取任务汇总提示词
     * 
     * @param taskResults 任务结果字符串
     * @return 填充了任务结果的提示词
     */
    public static String getSummaryPrompt(String taskResults) {
        return String.format(summaryPrompt, taskResults);
    }
} 