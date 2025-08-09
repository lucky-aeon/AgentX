package org.xhy.infrastructure.rag.enhancer;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.enhancer.SegmentEnhancer;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.model.SpecialNode;
import org.xhy.domain.rag.model.enums.SegmentType;
import org.xhy.domain.rag.straegy.context.ProcessingContext;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import java.util.Map;

/** 代码分析增强器
 * 
 * 职责：
 * - 对代码块类型的段落进行LLM分析增强
 * - 将代码转换为自然语言描述，便于RAG检索
 * - 生成代码功能说明和关键词
 * 
 * @author claude */
@Component
public class CodeAnalysisEnhancer implements SegmentEnhancer {

    private static final Logger log = LoggerFactory.getLogger(CodeAnalysisEnhancer.class);

    @Override
    public boolean canEnhance(ProcessedSegment segment) {
        // 检查是否包含代码类型的特殊节点
        return segment.hasSpecialNodes() && 
               segment.getSpecialNodeCount(SegmentType.CODE) > 0;
    }

    @Override
    public ProcessedSegment enhance(ProcessedSegment segment, ProcessingContext context) {
        try {
            // 检查是否有可用的LLM配置
            if (context.getLlmConfig() == null) {
                log.warn("No LLM config available for code analysis, skipping enhancement");
                return segment;
            }

            // 处理所有代码类型的特殊节点
            for (SpecialNode node : segment.getSpecialNodes().values()) {
                if (node.getNodeType() == SegmentType.CODE && !node.isProcessed()) {
                    enhanceCodeNode(node, context);
                }
            }

            log.debug("Enhanced segment with {} code nodes", segment.getSpecialNodeCount(SegmentType.CODE));
            
            return segment;

        } catch (Exception e) {
            log.error("Failed to enhance code segment", e);
            // 增强失败时返回原始段落
            return segment;
        }
    }
    
    /** 增强单个代码节点 */
    private void enhanceCodeNode(SpecialNode node, ProcessingContext context) {
        try {
            // 从节点元数据中提取代码信息
            Map<String, Object> metadata = node.getNodeMetadata();
            String language = metadata != null ? (String) metadata.get("language") : "unknown";
            
            // 提取代码内容（去掉markdown格式）
            String codeContent = extractCodeContent(node.getOriginalContent(), language);
            
            // 使用LLM生成代码描述
            String codeDescription = describeCodeWithLLM(codeContent, language, context);

            // 增强内容：保留原始代码 + 添加LLM描述
            String enhancedContent = String.format("%s\n\n代码功能描述：%s", 
                                                  node.getOriginalContent(), codeDescription);
            
            // 更新特殊节点的增强内容
            node.setEnhancedContent(enhancedContent);
            node.markAsProcessed();

            log.debug("Enhanced code node: language={}, original_length={}, enhanced_length={}", 
                     language, node.getOriginalContent().length(), enhancedContent.length());

        } catch (Exception e) {
            log.warn("Failed to enhance individual code node: {}", e.getMessage());
        }
    }

    @Override
    public int getPriority() {
        return 10; // 高优先级处理代码块
    }

    /** 从markdown格式中提取纯代码内容 */
    private String extractCodeContent(String markdownContent, String language) {
        // 移除代码块的markdown标记
        String content = markdownContent;
        
        // 移除开头的```language
        if (content.startsWith("```")) {
            int firstNewline = content.indexOf('\n');
            if (firstNewline > 0) {
                content = content.substring(firstNewline + 1);
            }
        }
        
        // 移除结尾的```
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        
        return content.trim();
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