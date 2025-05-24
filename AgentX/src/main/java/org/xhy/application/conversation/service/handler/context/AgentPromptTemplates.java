package org.xhy.application.conversation.service.handler.context;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 提示词模板 集中管理各种场景的提示词 */
public class AgentPromptTemplates {

    private static final String SUMMARY_PREFIX = "以下是用户历史消息的摘要，请仅作为参考，用户没有提起则不要回答摘要中的内容：\\n";

    /** 获取摘要算法的提示词 */
    public static String getSummaryPrefix() {
        return SUMMARY_PREFIX;
    }

    /** 根据预设的工具参数生成系统提示词中关于“可直接调用工具”的部分。
     *
     * @param toolPresetParams 一个 Map 结构，表示预设的工具及其参数。 外层 key 是 mcpServerName（例如 "surge"）， 内层 key 是工具名称（例如
     *     "surge_login"）， 最内层是工具的参数 Map。
     * @return 包含预设工具列表的字符串，如果 toolPresetParams 为空或null，则返回空字符串。 */
    public static String generatePresetToolPrompt(Map<String, Map<String, Map<String, String>>> toolPresetParams) {
        StringBuilder promptBuilder = new StringBuilder();

        // 确保 toolPresetParams 不为 null 且不为空
        if (toolPresetParams != null && !toolPresetParams.isEmpty()) {
            Set<String> presetToolNames = new HashSet<>();

            // 遍历 mcpServerName 层 (例如 "surge")
            for (Map.Entry<String, Map<String, Map<String, String>>> entry1 : toolPresetParams.entrySet()) {
                Map<String, Map<String, String>> innerMap = entry1.getValue(); // 获取工具名称层

                // 遍历工具名称层 (例如 "surge_login")
                for (Map.Entry<String, Map<String, String>> entry2 : innerMap.entrySet()) {
                    String toolName = entry2.getKey(); // 获取工具名称
                    presetToolNames.add(toolName); // 添加到集合中
                }
            }

            // 如果有预设工具，则构建提示词
            if (!presetToolNames.isEmpty()) {
                promptBuilder.append("你被赋予了访问多种工具的能力，其中一些工具已预设了必要的参数，因此在调用它们时**无需向用户询问任何信息**。\n\n");
                promptBuilder.append("以下是你可以直接调用的工具列表：\n");

                for (String toolName : presetToolNames) {
                    promptBuilder.append("- ").append(toolName).append("\n");
                }

                promptBuilder.append("\n当需要使用上述工具时，请直接执行工具调用，系统会自动填充所需参数。\n");
            }
        }
        return promptBuilder.toString();
    }

}