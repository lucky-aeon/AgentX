package org.xhy.infrastructure.rag.translator;

import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.util.ast.Node;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.strategy.context.ProcessingContext;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

/** 表格翻译器
 * 
 * 将表格内容翻译为自然语言描述，便于RAG检索
 * 
 * @author claude */
@Component
public class TableTranslator implements NodeTranslator {

    private static final Logger log = LoggerFactory.getLogger(TableTranslator.class);

    @Override
    public boolean canTranslate(Node node) {
        return node instanceof TableBlock;
    }

    @Override
    public String translate(Node node, ProcessingContext context) {
        try {
            // 基于AST节点准确提取表格信息
            String originalMarkdown = node.getChars().toString();
            String tableContent = originalMarkdown.trim();

            // 检查是否有可用的LLM配置
            if (context.getLlmConfig() == null) {
                log.warn("No LLM config available for table analysis, using fallback translation");
                return generateFallbackTableDescription(tableContent);
            }

            // 使用LLM分析表格内容
            String tableAnalysis = analyzeTableWithLLM(tableContent, context);

            // 增强内容：保留原始表格 + 添加LLM分析
            String enhancedContent = String.format("%s\n\n表格分析：%s", originalMarkdown, tableAnalysis);

            log.debug("Enhanced table: original_length={}, enhanced_length={}", originalMarkdown.length(),
                    enhancedContent.length());

            return enhancedContent;

        } catch (Exception e) {
            log.error("Failed to translate table content: {}", e.getMessage(), e);
            return node.getChars().toString(); // 出错时返回原内容
        }
    }

    @Override
    public int getPriority() {
        return 15; // 表格处理优先级稍低于代码
    }

    /** 使用LLM分析表格内容 */
    private String analyzeTableWithLLM(String tableContent, ProcessingContext context) {
        try {
            ChatModel chatModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, context.getLlmConfig());

            String prompt = buildTableAnalysisPrompt(tableContent);

            UserMessage message = UserMessage.from(prompt);
            ChatResponse response = chatModel.chat(message);

            String analysis = response.aiMessage().text().trim();
            log.debug("Generated table analysis: {}", analysis);

            return analysis;

        } catch (Exception e) {
            log.warn("Failed to analyze table with LLM: {}", e.getMessage());
            return generateFallbackTableDescription(tableContent);
        }
    }

    /** 构建表格分析提示词 */
    private String buildTableAnalysisPrompt(String tableContent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下Markdown表格的内容，用中文描述表格的结构、主要信息和数据特点，便于搜索和理解。\n\n");
        prompt.append("表格内容：\n");
        prompt.append(tableContent);
        prompt.append("\n\n");
        prompt.append("请按以下格式分析：\n");
        prompt.append("表格结构：[表格有多少行多少列，包含哪些字段]\n");
        prompt.append("主要内容：[表格记录的核心信息]\n");
        prompt.append("数据特点：[数据的分布、趋势或重要发现]\n");
        prompt.append("关键词：[便于搜索的关键词]");

        return prompt.toString();
    }

    /** 生成回退描述（LLM不可用时） */
    private String generateFallbackTableDescription(String tableContent) {
        StringBuilder description = new StringBuilder();

        // 分析表格基本信息
        String[] lines = tableContent.split("\n");
        int totalLines = lines.length;
        int dataRows = 0;
        int columns = 0;

        // 简单解析表格结构
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("|") && line.endsWith("|")) {
                if (line.contains("---")) {
                    continue; // 跳过分隔行
                }
                dataRows++;
                if (columns == 0) {
                    // 计算列数
                    columns = line.split("\\|").length - 2; // 减去首尾空字符串
                }
            }
        }

        description.append("表格包含");
        if (dataRows > 0) {
            description.append(dataRows).append("行数据");
        }
        if (columns > 0) {
            description.append("，").append(columns).append("列信息");
        }

        // 检测表格内容特征
        String lowerContent = tableContent.toLowerCase();
        if (lowerContent.contains("name") || lowerContent.contains("姓名")) {
            description.append("，包含人员信息");
        }
        if (lowerContent.contains("date") || lowerContent.contains("时间") || lowerContent.contains("日期")) {
            description.append("，包含时间数据");
        }
        if (lowerContent.contains("price") || lowerContent.contains("金额") || lowerContent.contains("￥")
                || lowerContent.contains("$")) {
            description.append("，包含价格信息");
        }

        return description.toString();
    }
}