package org.xhy.application.conversation.service.message.agent.template;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 提示词模板
 * 集中管理各种场景的提示词
 */
public class AgentPromptTemplates {
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