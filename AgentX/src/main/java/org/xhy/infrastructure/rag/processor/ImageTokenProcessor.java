package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.util.ast.Node;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 图片Token处理器 使用视觉模型进行图片OCR和内容理解
 * 
 * @author claude */
@Component
public class ImageTokenProcessor implements MarkdownTokenProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImageTokenProcessor.class);

    @Override
    public boolean canProcess(Node node) {
        return node instanceof Image;
    }

    @Override
    public ProcessedSegment process(Node node, ProcessingContext context) {
        try {
            Image image = (Image) node;
            String imageUrl = image.getUrl().toString();
            String altText = image.getText().toString();

            // 使用视觉模型处理图片
            String ocrAndDescription = processImageWithVisionModel(imageUrl, altText, context);

            String content = String.format("图片内容：%s", ocrAndDescription);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "image");
            metadata.put("url", imageUrl);
            metadata.put("alt", altText);

            return new ProcessedSegment(content, "image", metadata);

        } catch (Exception e) {
            log.error("Failed to process image node", e);
            // 回退方案：使用alt文本或默认描述
            Image image = (Image) node;
            String altText = image.getText().toString();
            String content = altText.isEmpty() ? "图片内容无法识别" : "图片：" + altText;
            return new ProcessedSegment(content, "image", null);
        }
    }

    @Override
    public int getPriority() {
        return 30; // 较低优先级，因为可能需要网络请求
    }

    /** 使用视觉模型处理图片
     *
     * @param imageUrl 图片URL
     * @param altText 替代文本
     * @param context 处理上下文
     * @return 图片的OCR文本和描述 */
    private String processImageWithVisionModel(String imageUrl, String altText, ProcessingContext context) {
        try {
            // 检查是否有可用的视觉模型配置
            if (context.getVisionModelConfig() == null) {
                log.warn("No vision model config available for image processing, using alt text");
                return altText.isEmpty() ? "图片内容无法识别" : altText;
            }

            ChatModel visionModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI,
                    context.getVisionModelConfig());

            // 构建提示文本
            String promptText = "请提取图片中的所有文字内容，并简要描述图片的主要内容。" + "请按以下格式输出：\n" + "文字内容：[提取的文字]\n" + "图片描述：[图片内容描述]";

            // 如果有替代文本，加入提示中
            if (!altText.isEmpty()) {
                promptText += "\n参考信息：" + altText;
            }

            UserMessage message = UserMessage.from(TextContent.from(promptText), ImageContent.from(imageUrl));

            ChatResponse response = visionModel.chat(message);
            String result = response.aiMessage().text().trim();

            log.debug("Processed image '{}' with result: {}", imageUrl, result);
            return result;

        } catch (Exception e) {
            log.warn("Failed to process image '{}' with vision model: {}", imageUrl, e.getMessage());

            // 回退方案：使用替代文本或默认描述
            if (!altText.isEmpty()) {
                return altText;
            } else {
                return "图片内容无法识别（URL: " + imageUrl + "）";
            }
        }
    }

    /** 验证图片URL是否有效
     *
     * @param imageUrl 图片URL
     * @return 是否有效 */
    private boolean isValidImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return false;
        }

        // 检查URL格式
        return imageUrl.startsWith("http://") || imageUrl.startsWith("https://") || imageUrl.startsWith("data:image/");
    }
}