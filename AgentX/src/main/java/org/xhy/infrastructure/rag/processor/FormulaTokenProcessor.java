package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.util.ast.Node;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.straegy.MarkdownTokenProcessor;
import org.xhy.domain.rag.straegy.context.ProcessingContext;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 公式Token处理器 使用大模型将LaTeX公式翻译为自然语言
 * 
 * @author claude */
@Component
public class FormulaTokenProcessor implements MarkdownTokenProcessor {

    private static final Logger log = LoggerFactory.getLogger(FormulaTokenProcessor.class);

    // 匹配LaTeX数学公式的正则表达式
    private static final Pattern INLINE_MATH_PATTERN = Pattern.compile("\\$([^$]+)\\$");
    private static final Pattern BLOCK_MATH_PATTERN = Pattern.compile("\\$\\$([^$]+)\\$\\$");

    public FormulaTokenProcessor() {
    }

    @Override
    public boolean canProcess(Node node) {
        // 检查节点文本是否包含数学公式
        if (node != null) {
            String text = node.getChars().toString();
            return containsMathFormula(text);
        }
        return false;
    }

    @Override
    public ProcessedSegment process(Node node, ProcessingContext context) {
        try {
            String text = node.getChars().toString();
            String processedContent = processMathFormulas(text, context);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "formula");
            metadata.put("original_text", text);

            return new ProcessedSegment(processedContent, "formula", metadata);

        } catch (Exception e) {
            log.error("Failed to process formula node", e);
            // 回退方案：返回原始文本
            String text = node.getChars().toString();
            return new ProcessedSegment("公式内容：" + text, "formula", null);
        }
    }

    @Override
    public int getPriority() {
        return 10; // 高优先级，优先处理公式
    }


    /** 检查文本是否包含数学公式 */
    private boolean containsMathFormula(String text) {
        return INLINE_MATH_PATTERN.matcher(text).find() || BLOCK_MATH_PATTERN.matcher(text).find();
    }

    /** 处理文本中的数学公式 */
    private String processMathFormulas(String text, ProcessingContext context) {
        StringBuilder result = new StringBuilder();
        String currentText = text;

        // 处理块级公式 $$...$$
        Matcher blockMatcher = BLOCK_MATH_PATTERN.matcher(currentText);
        while (blockMatcher.find()) {
            String formula = blockMatcher.group(1).trim();
            String translation = translateFormulaWithLLM(formula, context);

            String replacement = String.format("数学公式：%s。公式含义：%s", formula, translation);
            currentText = currentText.replace(blockMatcher.group(0), replacement);
        }

        // 处理行内公式 $...$
        Matcher inlineMatcher = INLINE_MATH_PATTERN.matcher(currentText);
        while (inlineMatcher.find()) {
            String formula = inlineMatcher.group(1).trim();
            String translation = translateFormulaWithLLM(formula, context);

            String replacement = String.format("公式 %s (%s)", formula, translation);
            currentText = currentText.replace(inlineMatcher.group(0), replacement);
        }

        return currentText;
    }

    /** 使用大模型翻译LaTeX公式为自然语言 */
    private String translateFormulaWithLLM(String latex, ProcessingContext context) {
        try {
            // 检查是否有可用的LLM配置
            if (context.getLlmConfig() == null) {
                log.warn("No LLM config available for formula translation, using fallback");
                return "数学表达式：" + latex;
            }

            ChatModel chatModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, context.getLlmConfig());

            String prompt = String.format("请将以下LaTeX数学公式翻译成简洁的中文自然语言描述，要求准确易懂：\n%s\n\n" + "请只返回翻译结果，不要包含其他说明。", latex);

            UserMessage message = UserMessage.from(prompt);
            ChatResponse response = chatModel.chat(message);

            String translation = response.aiMessage().text().trim();
            log.debug("Translated formula '{}' to '{}'", latex, translation);

            return translation;

        } catch (Exception e) {
            log.warn("Failed to translate formula '{}' with LLM: {}", latex, e.getMessage());
            return "数学表达式：" + latex; // 回退方案
        }
    }
}