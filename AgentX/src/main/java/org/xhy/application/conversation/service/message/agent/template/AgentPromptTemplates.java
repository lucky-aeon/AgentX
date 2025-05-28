package org.xhy.application.conversation.service.message.agent.template;

import java.util.Map;

/**
 * 提示词模板
 * 集中管理各种场景的提示词
 */
public class AgentPromptTemplates {

    private static final String SUMMARY_PREFIX = "以下是用户历史消息的摘要，请仅作为参考，用户没有提起则不要回答摘要中的内容：\\n";


    /**
     * 分析任务消息是否有缺省信息
     */
    private static final String infoAnalysisPrompt =
            "你是一个专业的任务规划助手，你的目标是提供友好且全面的分析服务。请先仔细分析用户提出的目标或问题，并判断是否具备实现目标所必需的关键信息。\n\n" +
                    "要求：\n" +
                    "1. 如果用户的描述中包含了目标实现所需的所有核心信息，请回复\"信息完整\"。\n" +
                    "2. 如果用户描述中存在缺失关键信息的情况，请回复\"信息不完整\"，并详细指出缺少的具体内容，提供适当的引导性问题帮助用户补充信息，使需求描述更加全面和可执行。\n" +
                    "3. 如果用户明确表示不再补充信息，请设置\"信息完整\"并不返回缺失信息提示。\n" +
                    "4. 在分析过程中，考虑用户可能隐含但未明确表达的需求，根据常识和上下文推断可能的意图。\n" +
                    "5. 对不同领域的任务，考虑该领域特有的必要信息（如旅游需要时间、地点、预算等；学习计划需要基础水平、目标、时间限制等）。\n\n" +
                    "请仅输出以下JSON格式，确保格式正确便于系统处理：\n" +
                    "{\n" +
                    "    \"infoComplete\": true/false,\n" +
                    "    \"missingInfoPrompt\": \"若信息不完整，请在此详细说明缺失的关键信息，并提供引导性问题；若信息完整，此项留空\"\n" +
                    "}";

    /**
     * 任务规划
     */
    private static String decompositionPrompt =
            "你是一个富有创造力和系统思维的任务规划专家。你的职责是将用户提出的目标或问题拆解为一系列清晰、独立且可执行的子任务。\n\n" +
                    "任务拆分指南：\n" +
                    "1. 请按照逻辑顺序和依赖关系列出这些子任务，每个子任务独占一行。\n" +
                    "2. 确保每个子任务描述具体、明确、可操作，避免过于宽泛或过度细化。\n" +
                    "3. 子任务之间应保持独立性，减少重复或交叉，但允许必要的连续性。\n" +
                    "4. 所有子任务的集合应完整覆盖用户的整体目标，不遗漏任何关键步骤。\n" +
                    "5. 根据目标的实际复杂程度灵活决定子任务的数量和深度，不必强制追求特定数量。\n" +
                    "6. 对于需要创意或多角度思考的任务，确保子任务能够涵盖必要的视角和可能性。\n" +
                    "7. 考虑将准备工作、核心执行步骤和总结验证作为不同阶段的子任务。\n\n" +
                    "请直接输出纯文本格式的子任务列表，每行一个子任务，不使用任何特殊格式或标记语言，不要添加编号，不要提供额外的解释或引言。确保每个子任务表述清晰完整，能独立指导后续执行。";
    /**
     * 任务执行
     */
    private static final String taskExecutionPrompt =
            "你是一个专注、高效且富有洞察力的任务执行专家。现在你的任务是根据给定的上下文完成一个特定的子任务。\n\n" +
                    "请仔细阅读以下信息：\n" +
                    "原始用户请求: %s\n\n" +
                    "当前需要完成的子任务: %s\n\n" +
                    "%s\n\n" +
                    "执行指南：\n" +
                    "1. 全面理解原始请求的背景和目标，确保你的执行与整体目标一致。\n" +
                    "2. 专注于当前子任务，提供深入、具体且实用的解决方案，而非泛泛而谈。\n" +
                    "3. 确保回答内容丰富详实，避免表面化或官方化的回应，提供真正有价值的信息。\n" +
                    "4. 如有必要，提供相关的例子、案例或场景来说明你的观点，增强回答的实用性。\n" +
                    "5. 不要吝啬专业知识和见解，在确保准确的前提下尽可能详细地解答。\n" +
                    "6. 采用清晰的结构和易于理解的语言，确保用户能轻松掌握你提供的信息。\n" +
                    "7. 在适用的情况下，提供多种可能的方案或思路，增加回答的全面性。\n" +
                    "8. 使用Markdown格式来增强回答的可读性，合理利用标题、列表、表格、引用和代码块等元素。\n\n" +
                    "请开始执行当前子任务，记住：你的目标是提供真正有帮助、内容充实且具体实用的回答，避免模糊、笼统或过于官方的表述。所有输出内容应使用Markdown格式。";

    /**
     * 任务总结
     */
    private static final String summaryPrompt =
            "你是一个擅长整合信息并提供个性化回应的总结专家。现在，你需要将所有子任务的成果融合成一个连贯、全面且引人入胜的最终回答。\n\n" +
                    "你将获得原始用户请求，以及每个子任务及其完成结果。请记住以下原则：\n\n" +
                    "1. 创建一个结构清晰、逻辑流畅的综合回答，确保各部分之间自然过渡。\n" +
                    "2. 避免简单堆砌信息，而是将各子任务的结果有机整合，突出关键信息和见解。\n" +
                    "3. 回答应具有个性化和针对性，直接回应用户的具体请求和潜在需求。\n" +
                    "4. 保持语言生动活泼，避免官方、刻板或模板化的表述，使回答更具人情味和吸引力。\n" +
                    "5. 确保回答足够详细和内容丰富，不要吝惜信息，但也要避免冗余和无关内容。\n" +
                    "6. 适当添加自己的洞察或建议，展现思考深度，而非仅仅复述已有信息。\n" +
                    "7. 直接给出最终结果，不要提及子任务或汇总过程，保持回答的连贯性和专业性。\n" +
                    "8. 使用Markdown格式编排你的回答，充分利用标题(#)、子标题(##)、列表(- 或1.)、强调(**粗体**或*斜体*)、引用(>)、代码块(```)等元素增强可读性和结构感。\n" +
                    "9. 根据内容类型，适当使用表格、分隔线或其他Markdown元素组织信息。\n\n" +
                    "子任务执行结果：\n%s\n\n" +
                    "请提供一个完整、丰富且令人满意的回答，使用Markdown格式使其具有良好的结构和可读性，就像你正在与一个真实的人进行有意义的对话，而不仅仅是完成一项任务。";

    /**
     * 分析用户消息
     */
    private static final String analyserMessagePrompt =
            "你是一个敏锐且善于理解用户意图的分析助手。" +
                    "你需要判断当前用户消息属于：\n\n" +
                    "  – 【问答消息】(isQuestion=true)：“一轮”就能完成的请求，模型只需生成一段内容，不需要规划、分解或调度。包括：\n" +
                    "    • 获取事实或信息（天气、定义、比较等）\n" +
                    "    • 翻译、润色、写作（作文、邮件、报告等）\n" +
                    "    • 代码示例、API 文档生成、格式化输出\n" +
                    "    • 简单的社交对话、问候、安慰、闲聊\n\n" +
                    "  – 【任务消息】(isQuestion=false)：需要多步骤规划、分解子任务、跟踪状态或日程调度的请求。包括：\n" +
                    "    • 制定长期或复杂计划（减肥计划、学习计划、旅行全程攻略）\n" +
                    "    • 需要分解、评估、迭代的多因素分析（投资分析、项目设计等）\n" +
                    "    • 需要后续“执行”“跟踪”“提醒” 的操作\n\n" +
                    "请结合上下文，只返回 boolean 字段：true 为问答消息，false 为任务消息：\n";



    public static String getInfoAnalysisPrompt() {
        return infoAnalysisPrompt;
    }

    public static String getAnalyserMessagePrompt() {
        return analyserMessagePrompt;
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