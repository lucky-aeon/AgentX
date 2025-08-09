package org.xhy.infrastructure.rag.enhancer;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** 图片OCR增强器
 * 
 * 职责：
 * - 对图片类型的段落进行视觉模型分析增强
 * - 识别图片内容并生成文本描述
 * - 提取图片中的文字和关键信息
 * 
 * @author claude */
@Component
public class ImageOcrEnhancer implements SegmentEnhancer {

    private static final Logger log = LoggerFactory.getLogger(ImageOcrEnhancer.class);

    @Override
    public boolean canEnhance(ProcessedSegment segment) {
        // 检查是否包含图片类型的特殊节点
        return segment.hasSpecialNodes() && 
               segment.getSpecialNodeCount(SegmentType.IMAGE) > 0;
    }

    @Override
    public ProcessedSegment enhance(ProcessedSegment segment, ProcessingContext context) {
        try {
            // 检查是否有可用的视觉模型配置
            if (context.getVisionModelConfig() == null) {
                log.warn("No vision model config available for image OCR, skipping enhancement");
                return segment;
            }

            // 处理所有图片类型的特殊节点
            for (SpecialNode node : segment.getSpecialNodes().values()) {
                if (node.getNodeType() == SegmentType.IMAGE && !node.isProcessed()) {
                    enhanceImageNode(node, context);
                }
            }

            log.debug("Enhanced segment with {} image nodes", segment.getSpecialNodeCount(SegmentType.IMAGE));
            
            return segment;

        } catch (Exception e) {
            log.error("Failed to enhance image segment", e);
            // 增强失败时返回原始段落
            return segment;
        }
    }
    
    /** 增强单个图片节点 */
    private void enhanceImageNode(SpecialNode node, ProcessingContext context) {
        try {
            // 从节点元数据中提取图片信息
            Map<String, Object> metadata = node.getNodeMetadata();
            String imageUrl = metadata != null ? (String) metadata.get("url") : null;
            String altText = metadata != null ? (String) metadata.get("alt") : "";

            // 检查是否为可处理的图片URL
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                log.debug("No valid image URL found in node, skipping OCR enhancement");
                return;
            }

            // 使用视觉模型分析图片
            String imageAnalysis = analyzeImageWithVisionModel(imageUrl, altText, context);

            // 增强内容：保留原始图片引用 + 添加OCR分析
            String enhancedContent = String.format("%s\n\n图片内容分析：%s", 
                                                  node.getOriginalContent(), imageAnalysis);
            
            // 更新特殊节点的增强内容
            node.setEnhancedContent(enhancedContent);
            node.markAsProcessed();

            log.debug("Enhanced image node: url={}, original_length={}, enhanced_length={}", 
                     imageUrl, node.getOriginalContent().length(), enhancedContent.length());

        } catch (Exception e) {
            log.warn("Failed to enhance individual image node: {}", e.getMessage());
        }
    }

    @Override
    public int getPriority() {
        return 20; // 图片处理优先级较低，因为可能涉及网络请求
    }

    /** 使用视觉模型分析图片 */
    private String analyzeImageWithVisionModel(String imageUrl, String altText, ProcessingContext context) {
        try {
            ChatModel chatModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, context.getVisionModelConfig());

            String prompt = buildImageAnalysisPrompt(altText);

            UserMessage message = UserMessage.from(prompt);
            ImageContent imageContent = new ImageContent(imageUrl);
            ChatResponse response = chatModel.chat(Arrays.asList(UserMessage.from(imageContent), message));

            String analysis = response.aiMessage().text().trim();
            log.debug("Generated image analysis for {}: {}", imageUrl, analysis);

            return analysis;

        } catch (Exception e) {
            log.warn("Failed to analyze image with vision model: {}", e.getMessage());
            return "";
        }
    }

    /** 构建图片分析提示词 */
    private String buildImageAnalysisPrompt(String altText) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下图片的内容，用中文描述图片中的主要信息,主要用于 RAG 中便于搜索和理解。\n\n");
        
        if (altText != null && !altText.trim().isEmpty()) {
            prompt.append("图片描述：").append(altText).append("\n");
        }
        
        prompt.append("关键词：[便于搜索的关键词]");

        return prompt.toString();
    }
}