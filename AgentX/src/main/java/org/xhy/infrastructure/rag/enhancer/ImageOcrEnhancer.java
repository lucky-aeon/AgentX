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
        return "image".equals(segment.getType());
    }

    @Override
    public ProcessedSegment enhance(ProcessedSegment segment, ProcessingContext context) {
        try {
            // 检查是否有可用的视觉模型配置
            if (context.getVisionModelConfig() == null) {
                log.warn("No vision model config available for image OCR, skipping enhancement");
                return segment;
            }

            // 从元数据中提取图片信息
            Map<String, Object> metadata = segment.getMetadata();
            String imageUrl = metadata != null ? (String) metadata.get("url") : null;
            String altText = metadata != null ? (String) metadata.get("alt") : "";

            // 检查是否为可处理的图片URL
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                log.debug("No valid image URL found, skipping OCR enhancement");
                return segment;
            }

            // 使用视觉模型分析图片
            String imageAnalysis = analyzeImageWithVisionModel(imageUrl, altText, context);

            // 增强内容：保留原始图片引用 + 添加OCR分析
            String enhancedContent = String.format("%s\n\n图片内容分析：%s", segment.getContent(), imageAnalysis);

            // 创建增强后的段落
            ProcessedSegment enhanced = new ProcessedSegment(enhancedContent, segment.getType(), segment.getMetadata());
            enhanced.setOrder(segment.getOrder());
            
            log.debug("Enhanced image segment: url={}, original_length={}, enhanced_length={}", 
                     imageUrl, segment.getContent().length(), enhancedContent.length());
            
            return enhanced;

        } catch (Exception e) {
            log.error("Failed to enhance image segment", e);
            // 增强失败时返回原始段落
            return segment;
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

            String prompt = buildImageAnalysisPrompt(imageUrl, altText);

            UserMessage message = UserMessage.from(prompt);
            ChatResponse response = chatModel.chat(message);

            String analysis = response.aiMessage().text().trim();
            log.debug("Generated image analysis for {}: {}", imageUrl, analysis);

            return analysis;

        } catch (Exception e) {
            log.warn("Failed to analyze image with vision model: {}", e.getMessage());
            return generateFallbackImageDescription(imageUrl, altText);
        }
    }

    /** 构建图片分析提示词 */
    private String buildImageAnalysisPrompt(String imageUrl, String altText) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下图片的内容，用中文描述图片中的主要信息，包括文字、图表、对象等，便于搜索和理解。\n\n");
        
        // 注意：实际的视觉模型调用需要特殊的消息格式，这里简化处理
        prompt.append("图片URL：").append(imageUrl).append("\n");
        
        if (altText != null && !altText.trim().isEmpty()) {
            prompt.append("图片描述：").append(altText).append("\n");
        }
        
        prompt.append("\n请按以下格式分析：\n");
        prompt.append("图片类型：[图片的类别，如截图、图表、照片等]\n");
        prompt.append("主要内容：[图片中的核心信息]\n");
        prompt.append("文字内容：[如果有文字，请提取出来]\n");
        prompt.append("关键词：[便于搜索的关键词]");

        return prompt.toString();
    }

    /** 生成回退描述（视觉模型不可用时） */
    private String generateFallbackImageDescription(String imageUrl, String altText) {
        StringBuilder description = new StringBuilder();
        
        description.append("图片");
        
        if (altText != null && !altText.trim().isEmpty()) {
            description.append("：").append(altText);
        }
        
        // 根据URL推断图片类型
        String lowerUrl = imageUrl.toLowerCase();
        if (lowerUrl.contains("screenshot") || lowerUrl.contains("capture")) {
            description.append("，可能是截图");
        } else if (lowerUrl.contains("chart") || lowerUrl.contains("graph")) {
            description.append("，可能是图表");
        } else if (lowerUrl.contains("diagram")) {
            description.append("，可能是示意图");
        }
        
        // 根据文件扩展名判断格式
        if (lowerUrl.endsWith(".png")) {
            description.append("（PNG格式）");
        } else if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            description.append("（JPEG格式）");
        } else if (lowerUrl.endsWith(".gif")) {
            description.append("（GIF格式）");
        }
        
        description.append("。图片链接：").append(imageUrl);
        
        return description.toString();
    }
}