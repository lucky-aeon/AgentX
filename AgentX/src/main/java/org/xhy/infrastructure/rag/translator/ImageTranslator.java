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
 * 使用视觉模型分析图片内容，转换为文本描述 */
@Component
public class ImageTranslator implements NodeTranslator {

    private final static String SYSTEM_PROMPT = "请分析以下图片的内容，描述图片中的内容。场景：RAG 中的图片向量化，因此你需要尽可能的描述，为了后续的向量化以及检索";

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

            // 检查是否有可用的视觉模型配置
            if (context.getVisionModelConfig() == null) {
                log.warn("No vision model config available for image OCR, using fallback translation");
                return imageUrl;
            }

            // 检查是否为可处理的图片URL
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                log.debug("No valid image URL found, using fallback translation");
                return imageUrl;
            }

            // 使用视觉模型分析图片
            return analyzeImageWithVisionModel(imageUrl, context);

        } catch (Exception e) {
            log.error("Failed to translate image content: {}", e.getMessage(), e);
            return node.getChars().toString(); // 出错时返回原内容
        }
    }

    @Override
    public int getPriority() {
        return 20; // 图片处理优先级较低，因为可能涉及网络请求
    }

    /** 使用视觉模型分析图片 */
    private String analyzeImageWithVisionModel(String imageUrl, ProcessingContext context) {
        try {
            ChatModel chatModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, context.getVisionModelConfig());

            UserMessage textMessage = UserMessage.from(SYSTEM_PROMPT);
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
}