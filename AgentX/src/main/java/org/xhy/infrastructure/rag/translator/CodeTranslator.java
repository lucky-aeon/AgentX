package org.xhy.infrastructure.rag.translator;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
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

/** 代码翻译器
 * 
 * 将代码块翻译为自然语言描述，便于RAG检索
 * 
 * @author claude */
@Component
public class CodeTranslator implements NodeTranslator {

    private static final Logger log = LoggerFactory.getLogger(CodeTranslator.class);

    @Override
    public boolean canTranslate(Node node) {
        return node instanceof FencedCodeBlock || node instanceof IndentedCodeBlock;
    }

    @Override
    public String translate(Node node, ProcessingContext context) {
        try {
            // 基于AST节点准确提取代码信息
            String language = null;
            String codeContent = null;
            String originalMarkdown = node.getChars().toString();

            if (node instanceof FencedCodeBlock) {
                FencedCodeBlock codeBlock = (FencedCodeBlock) node;
                language = codeBlock.getInfo() != null && !codeBlock.getInfo().isBlank()
                        ? codeBlock.getInfo().toString().trim()
                        : "text";
                codeContent = codeBlock.getContentChars().toString(); // 准确提取，无需字符串解析
            } else if (node instanceof IndentedCodeBlock) {
                IndentedCodeBlock codeBlock = (IndentedCodeBlock) node;
                language = "text";
                codeContent = codeBlock.getContentChars().toString(); // 准确提取，无需字符串解析
            }

            // 检查是否有可用的LLM配置
            if (context.getLlmConfig() == null) {
                log.warn("No LLM config available for code analysis, using fallback translation");
                return generateFallbackDescription(codeContent, language);
            }

            // 使用LLM生成代码描述
            String codeDescription = describeCodeWithLLM(codeContent, language, context);

            // 增强内容：保留原始代码 + 添加LLM描述
            String enhancedContent = String.format("%s\n\n代码功能描述：%s", originalMarkdown, codeDescription);

            log.debug("Enhanced code: language={}, original_length={}, enhanced_length={}", language,
                    originalMarkdown.length(), enhancedContent.length());

            return enhancedContent;

        } catch (Exception e) {
            log.error("Failed to translate code content: {}", e.getMessage(), e);
            return node.getChars().toString(); // 出错时返回原内容
        }
    }

    @Override
    public int getPriority() {
        return 10; // 高优先级处理代码块
    }

    /** 使用LLM生成代码描述 */
    private String describeCodeWithLLM(String code, String language, ProcessingContext context) {
        try {
            ChatModel chatModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, context.getLlmConfig());

            String prompt = buildCodeAnalysisPrompt(code, language);

            UserMessage message = UserMessage.from(prompt);
            ChatResponse response = chatModel.chat(message);

            String description = response.aiMessage().text().trim();
            log.debug("Generated code description for {} code: {}", language, description);

            return description;

        } catch (Exception e) {
            log.warn("Failed to describe code with LLM: {}", e.getMessage());
            return generateFallbackDescription(code, language);
        }
    }

    /** 构建代码分析提示词 */
    private String buildCodeAnalysisPrompt(String code, String language) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下代码并用简洁的中文自然语言描述其功能和作用，便于搜索和理解。");

        if (language != null && !language.isEmpty() && !"unknown".equals(language)) {
            prompt.append("这是").append(language).append("代码：\n\n");
        } else {
            prompt.append("代码内容：\n\n");
        }

        prompt.append("```").append(language != null ? language : "").append("\n");
        prompt.append(code);
        prompt.append("\n```\n\n");

        prompt.append("请按以下格式输出：\n");
        prompt.append("功能：[代码的主要功能]\n");
        prompt.append("详细说明：[具体实现逻辑或关键步骤]\n");
        prompt.append("关键词：[便于搜索的关键技术词汇]");

        return prompt.toString();
    }

    /** 生成回退描述（LLM不可用时） */
    private String generateFallbackDescription(String code, String language) {
        StringBuilder description = new StringBuilder();

        if (language != null && !language.isEmpty() && !"unknown".equals(language)) {
            description.append(language).append("代码片段");
        } else {
            description.append("代码片段");
        }

        // 简单分析代码特征
        String[] lines = code.split("\n");
        description.append("，共").append(lines.length).append("行");

        // 检测常见关键字
        String lowerCode = code.toLowerCase();
        if (lowerCode.contains("function") || lowerCode.contains("def ") || lowerCode.contains("func ")) {
            description.append("，包含函数定义");
        }
        if (lowerCode.contains("class ")) {
            description.append("，包含类定义");
        }
        if (lowerCode.contains("import ") || lowerCode.contains("#include") || lowerCode.contains("require")) {
            description.append("，包含导入语句");
        }

        return description.toString();
    }
}