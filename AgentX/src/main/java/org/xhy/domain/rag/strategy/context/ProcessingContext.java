package org.xhy.domain.rag.strategy.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.rag.message.RagDocMessage;
import org.xhy.domain.rag.model.ModelConfig;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;
import org.xhy.infrastructure.rag.factory.EmbeddingModelFactory;
import org.xhy.infrastructure.rag.service.UserModelConfigResolver;

/** Markdown处理上下文 */
public class ProcessingContext {

    private static final Logger log = LoggerFactory.getLogger(ProcessingContext.class);

    /** 嵌入模型配置 */
    private final EmbeddingModelFactory.EmbeddingConfig embeddingConfig;

    /** LLM配置（用于公式和表格翻译） */
    private final ProviderConfig llmConfig;

    /** 视觉模型配置（用于图片处理） */
    private final ProviderConfig visionModelConfig;

    /** 用户ID */
    private final String userId;

    /** 文件ID */
    private final String fileId;

    public ProcessingContext(EmbeddingModelFactory.EmbeddingConfig embeddingConfig, ProviderConfig llmConfig,
            ProviderConfig visionModelConfig, String userId, String fileId) {
        this.embeddingConfig = embeddingConfig;
        this.llmConfig = llmConfig;
        this.visionModelConfig = visionModelConfig;
        this.userId = userId;
        this.fileId = fileId;
    }

    /** 从RagDocSyncOcrMessage构建处理上下文
     *
     * @param message 消息对象
     * @param userModelConfigResolver 用户模型配置解析器
     * @return 处理上下文 */
    public static ProcessingContext from(RagDocMessage message, UserModelConfigResolver userModelConfigResolver) {
        try {
            String userId = message.getUserId();

            // 获取嵌入模型配置
            EmbeddingModelFactory.EmbeddingConfig embeddingConfig = null;
            try {
                ModelConfig embeddingModelConfig = userModelConfigResolver.getUserEmbeddingModelConfig(userId);
                embeddingConfig = new EmbeddingModelFactory.EmbeddingConfig(embeddingModelConfig.getApiKey(),
                        embeddingModelConfig.getBaseUrl(), embeddingModelConfig.getModelEndpoint());
            } catch (Exception e) {
                log.warn("获取用户 {} 嵌入模型配置失败: {}", userId, e.getMessage());
            }

            // 获取聊天模型配置（用于LLM处理）
            ProviderConfig llmConfig = null;
            try {
                ModelConfig chatModelConfig = userModelConfigResolver.getUserChatModelConfig(userId);
                llmConfig = new ProviderConfig(chatModelConfig.getApiKey(), chatModelConfig.getBaseUrl(),
                        chatModelConfig.getModelEndpoint(), ProviderProtocol.OPENAI);
            } catch (Exception e) {
                log.warn("获取用户 {} 聊天模型配置失败: {}", userId, e.getMessage());
            }

            // 获取OCR/视觉模型配置
            ProviderConfig visionModelConfig = null;
            try {
                ModelConfig ocrModelConfig = userModelConfigResolver.getUserOcrModelConfig(userId);
                visionModelConfig = new ProviderConfig(ocrModelConfig.getApiKey(), ocrModelConfig.getBaseUrl(),
                        ocrModelConfig.getModelEndpoint(), ProviderProtocol.OPENAI);
            } catch (Exception e) {
                log.warn("获取用户 {} OCR模型配置失败: {}", userId, e.getMessage());
            }

            return new ProcessingContext(embeddingConfig, llmConfig, visionModelConfig, userId, message.getFileId());

        } catch (Exception e) {
            log.error("从消息创建ProcessingContext失败: {}", e.getMessage(), e);
            // 创建一个空配置的上下文作为回退
            return new ProcessingContext(null, null, null, message.getUserId(), message.getFileId());
        }
    }

    public EmbeddingModelFactory.EmbeddingConfig getEmbeddingConfig() {
        return embeddingConfig;
    }

    public ProviderConfig getLlmConfig() {
        return llmConfig;
    }

    public ProviderConfig getVisionModelConfig() {
        return visionModelConfig;
    }

    public String getUserId() {
        return userId;
    }

    public String getFileId() {
        return fileId;
    }
}