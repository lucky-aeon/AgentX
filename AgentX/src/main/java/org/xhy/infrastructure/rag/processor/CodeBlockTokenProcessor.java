package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
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
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import java.util.HashMap;
import java.util.Map;

/** 代码块Token处理器 将代码块转换为自然语言描述，便于RAG检索
 * 
 * @author claude */
@Component
public class CodeBlockTokenProcessor implements MarkdownTokenProcessor {

    private static final Logger log = LoggerFactory.getLogger(CodeBlockTokenProcessor.class);

    @Override
    public boolean canProcess(Node node) {
        return node instanceof FencedCodeBlock || node instanceof IndentedCodeBlock;
    }

    @Override
    public ProcessedSegment process(Node node, ProcessingContext context) {
        try {
            String codeContent;
            String language = null;

            if (node instanceof FencedCodeBlock) {
                FencedCodeBlock codeBlock = (FencedCodeBlock) node;
                codeContent = codeBlock.getContentChars().toString().trim();
                // 获取语言标识
                if (codeBlock.getInfo() != null && !codeBlock.getInfo().isBlank()) {
                    language = codeBlock.getInfo().toString().trim();
                }
            } else if (node instanceof IndentedCodeBlock) {
                IndentedCodeBlock codeBlock = (IndentedCodeBlock) node;
                codeContent = codeBlock.getContentChars().toString().trim();
            } else {
                codeContent = node.getChars().toString().trim();
            }

            // 使用LLM生成代码描述
            String codeDescription = describeCodeWithLLM(codeContent, language, context);

            String content = String.format("代码块内容：%s", codeDescription);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "code");
            metadata.put("language", language != null ? language : "unknown");
            metadata.put("code", codeContent);
            metadata.put("lines", codeContent.split("\n").length);

            return new ProcessedSegment(content, "code", metadata);

        } catch (Exception e) {
            log.error("Failed to process code block node", e);
            // 回退方案：返回原始代码文本
            String codeText = node.getChars().toString();
            return new ProcessedSegment("代码内容：" + codeText, "code", null);
        }
    }

    @Override
    public int getPriority() {
        return 5; // 最高优先级，优先处理代码块
    }

    /** 使用大模型生成代码描述 */
    private String describeCodeWithLLM(String code, String language, ProcessingContext context) {
        try {
            // 检查是否有可用的LLM配置
            if (context.getLlmConfig() == null) {
                log.warn("No LLM config available for code description, using fallback");
                return generateFallbackDescription(code, language);
            }

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

        if (language != null && !language.isEmpty()) {
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

    /** 生成回退描述 */
    private String generateFallbackDescription(String code, String language) {
        StringBuilder description = new StringBuilder();

        if (language != null && !language.isEmpty()) {
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

        description.append("。代码内容：").append(code);

        return description.toString();
    }

    /** 检测编程语言（如果未指定） */
    private String detectLanguage(String code) {
        String lowerCode = code.toLowerCase().trim();

        // Java
        if (lowerCode.contains("public class") || lowerCode.contains("public static void main")) {
            return "java";
        }
        // Python
        if (lowerCode.contains("def ") || lowerCode.contains("import ") || lowerCode.contains("from ")) {
            return "python";
        }
        // JavaScript
        if (lowerCode.contains("function") || lowerCode.contains("const ") || lowerCode.contains("let ")) {
            return "javascript";
        }
        // SQL
        if (lowerCode.contains("select ") || lowerCode.contains("insert ") || lowerCode.contains("update ")) {
            return "sql";
        }
        // Shell
        if (lowerCode.startsWith("#!/bin/bash") || lowerCode.contains("echo ")) {
            return "shell";
        }

        return "unknown";
    }
}