package org.xhy.application.conversation.service.message.agent.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

import java.util.List;
import java.util.Map;

/** RAG工具规范定义 定义RAG工具的MCP规范，包括工具名称、描述和参数 */
public class RagToolSpecification {

    /** RAG工具名称 */
    public static final String TOOL_NAME = "knowledge_search";

    /** RAG工具显示名称 */
    public static final String TOOL_DISPLAY_NAME = "知识库搜索";

    /** RAG工具描述 */
    public static final String TOOL_DESCRIPTION = "在配置的知识库中搜索相关信息，用于回答用户问题";

    /** 创建RAG工具规范
     * @param knowledgeBaseNames 知识库名称列表，用于工具描述
     * @return ToolSpecification */
    public static ToolSpecification createToolSpecification(List<String> knowledgeBaseNames) {
        // 构建工具描述，包含可用的知识库信息
        String description = buildToolDescription(knowledgeBaseNames);

        return ToolSpecification.builder().name(TOOL_NAME).description(description).parameters(createParameterSchema())
                .build();
    }

    /** 构建工具描述
     * @param knowledgeBaseNames 知识库名称列表
     * @return 工具描述字符串 */
    private static String buildToolDescription(List<String> knowledgeBaseNames) {
        StringBuilder description = new StringBuilder(TOOL_DESCRIPTION);

        if (knowledgeBaseNames != null && !knowledgeBaseNames.isEmpty()) {
            description.append("。可用的知识库包括：");
            for (int i = 0; i < knowledgeBaseNames.size(); i++) {
                if (i > 0) {
                    description.append("、");
                }
                description.append(knowledgeBaseNames.get(i));
            }
        }

        return description.toString();
    }

    /** 创建参数模式
     * @return JsonObjectSchema */
    private static JsonObjectSchema createParameterSchema() {
        return JsonObjectSchema.builder()
                .addProperties(Map.of("query", JsonStringSchema.builder().description("搜索查询内容，描述用户想要了解的问题或关键词").build(),
                        "maxResults", JsonIntegerSchema.builder().description("最大返回结果数量，默认为10，范围1-20").build(),
                        "minScore", JsonNumberSchema.builder().description("最小相似度阈值，默认为0.5，范围0.0-1.0，值越高结果越精确").build(),
                        "enableRerank", JsonBooleanSchema.builder().description("是否启用重排序优化，默认为true，可提高搜索结果质量").build()))
                .required("query") // 只有query是必需的
                .build();
    }

    /** 获取工具名称
     * @return 工具名称 */
    public static String getToolName() {
        return TOOL_NAME;
    }

    /** 获取工具显示名称
     * @return 工具显示名称 */
    public static String getToolDisplayName() {
        return TOOL_DISPLAY_NAME;
    }

    /** 检查是否为RAG工具
     * @param toolName 工具名称
     * @return 是否为RAG工具 */
    public static boolean isRagTool(String toolName) {
        return TOOL_NAME.equals(toolName);
    }
}
