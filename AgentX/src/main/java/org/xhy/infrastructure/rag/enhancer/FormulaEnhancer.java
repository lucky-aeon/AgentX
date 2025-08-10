package org.xhy.infrastructure.rag.enhancer;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.enhancer.SegmentEnhancer;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.straegy.context.ProcessingContext;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

/** 公式增强器
 * 
 * 职责： - 对数学公式类型的段落进行LLM分析增强 - 将数学公式转换为自然语言描述 - 解释公式的含义和应用场景
 * 
 * @author claude */
@Component
public class FormulaEnhancer implements SegmentEnhancer {

    private static final Logger log = LoggerFactory.getLogger(FormulaEnhancer.class);

    @Override
    public boolean canEnhance(ProcessedSegment segment) {
        return segment.getContent() != null && containsFormula(segment.getContent());
    }

    @Override
    public ProcessedSegment enhance(ProcessedSegment segment, ProcessingContext context) {
        try {
            // 检查是否有可用的LLM配置
            if (context.getLlmConfig() == null) {
                log.warn("No LLM config available for formula analysis, skipping enhancement");
                return segment;
            }

            // 提取公式内容
            String formulaContent = extractFormulaContent(segment.getContent());

            // 使用LLM分析公式
            String formulaAnalysis = analyzeFormulaWithLLM(formulaContent, context);

            // 增强内容：保留原始公式 + 添加LLM解释
            String enhancedContent = String.format("%s\n\n公式解释：%s", segment.getContent(), formulaAnalysis);

            // 创建增强后的段落
            ProcessedSegment enhanced = new ProcessedSegment(enhancedContent, segment.getType(), segment.getMetadata());
            enhanced.setOrder(segment.getOrder());

            log.debug("Enhanced formula segment: original_length={}, enhanced_length={}", segment.getContent().length(),
                    enhancedContent.length());

            return enhanced;

        } catch (Exception e) {
            log.error("Failed to enhance formula segment", e);
            // 增强失败时返回原始段落
            return segment;
        }
    }

    @Override
    public int getPriority() {
        return 25; // 公式处理优先级较低
    }

    /** 检测内容是否包含数学公式 */
    private boolean containsFormula(String content) {
        if (content == null) {
            return false;
        }

        // 检测LaTeX公式标记
        return content.contains("$$") || // 独立公式
                content.contains("$") || // 行内公式
                content.contains("\\(") || // 替代行内公式语法
                content.contains("\\[") || // 替代独立公式语法
                content.contains("\\begin{") || // LaTeX环境
                content.matches(".*[a-zA-Z]\\s*=\\s*.*"); // 简单等式检测
    }

    /** 提取公式内容 */
    private String extractFormulaContent(String content) {
        if (content == null) {
            return "";
        }

        // 如果已经是纯公式，直接返回
        if (content.startsWith("$$") || content.startsWith("$")) {
            return content;
        }

        // 否则返回原内容
        return content;
    }

    /** 使用LLM分析公式 */
    private String analyzeFormulaWithLLM(String formulaContent, ProcessingContext context) {
        try {
            ChatModel chatModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, context.getLlmConfig());

            String prompt = buildFormulaAnalysisPrompt(formulaContent);

            UserMessage message = UserMessage.from(prompt);
            ChatResponse response = chatModel.chat(message);

            String analysis = response.aiMessage().text().trim();
            log.debug("Generated formula analysis: {}", analysis);

            return analysis;

        } catch (Exception e) {
            log.warn("Failed to analyze formula with LLM: {}", e.getMessage());
            return generateFallbackFormulaDescription(formulaContent);
        }
    }

    /** 构建公式分析提示词 */
    private String buildFormulaAnalysisPrompt(String formulaContent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下数学公式，用中文解释公式的含义、变量定义和应用场景，便于理解和搜索。\n\n");
        prompt.append("公式内容：\n");
        prompt.append(formulaContent);
        prompt.append("\n\n");
        prompt.append("请按以下格式分析：\n");
        prompt.append("公式含义：[公式表达的数学或物理概念]\n");
        prompt.append("变量说明：[各个变量或符号的含义]\n");
        prompt.append("应用场景：[这个公式常用于哪些领域或问题]\n");
        prompt.append("关键词：[便于搜索的数学或科学术语]");

        return prompt.toString();
    }

    /** 生成回退描述（LLM不可用时） */
    private String generateFallbackFormulaDescription(String formulaContent) {
        StringBuilder description = new StringBuilder();

        description.append("数学公式");

        // 简单分析公式特征
        if (formulaContent.contains("=")) {
            description.append("，包含等式");
        }
        if (formulaContent.contains("∫") || formulaContent.contains("\\int")) {
            description.append("，包含积分");
        }
        if (formulaContent.contains("∑") || formulaContent.contains("\\sum")) {
            description.append("，包含求和");
        }
        if (formulaContent.contains("√") || formulaContent.contains("\\sqrt")) {
            description.append("，包含平方根");
        }
        if (formulaContent.contains("^") || formulaContent.contains("_")) {
            description.append("，包含上下标");
        }
        if (formulaContent.contains("\\frac")) {
            description.append("，包含分数");
        }

        // 检测可能的数学领域
        String lowerContent = formulaContent.toLowerCase();
        if (lowerContent.contains("sin") || lowerContent.contains("cos") || lowerContent.contains("tan")) {
            description.append("，可能涉及三角函数");
        }
        if (lowerContent.contains("log") || lowerContent.contains("ln")) {
            description.append("，可能涉及对数函数");
        }
        if (lowerContent.contains("lim") || lowerContent.contains("\\lim")) {
            description.append("，可能涉及极限");
        }

        return description.toString();
    }
}