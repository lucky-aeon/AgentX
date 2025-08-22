package org.xhy.infrastructure.rag.translator;

import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.util.ast.Node;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.strategy.context.ProcessingContext;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import java.util.Arrays;

/** 图片翻译器
 * 
 * 使用视觉模型分析图片内容，转换为文本描述
 * 
 * @author claude */
@Component
public class ImageTranslator implements NodeTranslator {

    private static final Logger log = LoggerFactory.getLogger(ImageTranslator.class);

    @Override
    public boolean canTranslate(Node node) {
        return node instanceof Image;
    }

    @Override
    public String translate(Node node, ProcessingContext context) {
        try {
            // 基于AST节点准确提取图片信息
            Image imageNode = (Image) node;
            String originalMarkdown = node.getChars().toString();
            String imageUrl = imageNode.getUrl().toString();
            String altText = extractAltText(imageNode);

            // 检查是否有可用的视觉模型配置
            if (context.getVisionModelConfig() == null) {
                log.warn("No vision model config available for image OCR, using fallback translation");
                return generateFallbackDescription(originalMarkdown, imageUrl, altText);
            }

            // 检查是否为可处理的图片URL
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                log.debug("No valid image URL found, using fallback translation");
                return generateFallbackDescription(originalMarkdown, imageUrl, altText);
            }

            // 使用视觉模型分析图片
            String imageAnalysis = analyzeImageWithVisionModel(imageUrl, altText, context);

            // 增强内容：保留原始图片引用 + 添加OCR分析
            String enhancedContent;
            if (imageAnalysis != null && !imageAnalysis.trim().isEmpty()) {
                enhancedContent = String.format("%s\n\n图片内容分析：%s", originalMarkdown, imageAnalysis);
            } else {
                enhancedContent = generateFallbackDescription(originalMarkdown, imageUrl, altText);
            }

            log.debug("Enhanced image: url={}, original_length={}, enhanced_length={}", imageUrl,
                    originalMarkdown.length(), enhancedContent.length());

            return enhancedContent;

        } catch (Exception e) {
            log.error("Failed to translate image content: {}", e.getMessage(), e);
            return node.getChars().toString(); // 出错时返回原内容
        }
    }

    @Override
    public int getPriority() {
        return 20; // 图片处理优先级较低，因为可能涉及网络请求
    }

    /** 提取图片的Alt文本 */
    private String extractAltText(Image imageNode) {
        StringBuilder altText = new StringBuilder();
        // 遍历Image节点的子节点，提取alt文本
        for (Node child : imageNode.getChildren()) {
            extractTextRecursively(child, altText);
        }
        return altText.toString().trim();
    }

    /** 递归提取文本内容 */
    private void extractTextRecursively(Node node, StringBuilder text) {
        if (node instanceof com.vladsch.flexmark.ast.Text) {
            text.append(node.getChars().toString());
        } else {
            for (Node child : node.getChildren()) {
                extractTextRecursively(child, text);
            }
        }
    }

    /** 使用视觉模型分析图片 */
    private String analyzeImageWithVisionModel(String imageUrl, String altText, ProcessingContext context) {
        try {
            ChatModel chatModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, context.getVisionModelConfig());

            String prompt = buildImageAnalysisPrompt(altText);

            UserMessage textMessage = UserMessage.from(prompt);
            ImageContent imageContent = new ImageContent(imageUrl);
            UserMessage imageMessage = UserMessage.from(imageContent);

            ChatResponse response = chatModel.chat(Arrays.asList(imageMessage, textMessage));

            String analysis = response.aiMessage().text().trim();
            log.debug("Generated image analysis for {}: {}", imageUrl, analysis);

            return analysis;

        } catch (Exception e) {
            log.warn("Failed to analyze image with vision model: {}", e.getMessage());
            return null;
        }
    }

    /** 构建图片分析提示词 */
    private String buildImageAnalysisPrompt(String altText) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下图片的内容，用中文描述图片中的主要信息,主要用于 RAG 中便于搜索和理解。\n\n");

        if (altText != null && !altText.trim().isEmpty()) {
            prompt.append("图片描述：").append(altText).append("\n");
        }

        prompt.append("请描述图片的主要内容，包括：\n");
        prompt.append("- 主要物体或场景\n");
        prompt.append("- 图片中的文字内容（如有）\n");
        prompt.append("- 重要的视觉元素\n");
        prompt.append("关键词：[便于搜索的关键词]");

        return prompt.toString();
    }

    /** 生成回退描述（视觉模型不可用时） */
    private String generateFallbackDescription(String originalMarkdown, String url, String alt) {
        StringBuilder description = new StringBuilder();

        description.append("这是一张图片");

        if (alt != null && !alt.trim().isEmpty()) {
            description.append("：").append(alt.trim());
        }

        if (url != null && !url.trim().isEmpty()) {
            description.append("（图片地址：").append(url).append("）");
        }

        description.append("。此内容包含图像信息，适合查询视觉相关问题。");

        return String.format("%s\n\n图片描述：%s", originalMarkdown, description.toString());
    }
}