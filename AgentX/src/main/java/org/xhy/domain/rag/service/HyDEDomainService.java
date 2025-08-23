package org.xhy.domain.rag.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xhy.domain.rag.model.ModelConfig;
import org.xhy.infrastructure.rag.service.UserModelConfigResolver;


/** HyDE（假设文档嵌入）领域服务 使用用户配置的LLM生成假设文档来改善RAG检索效果
 * 
 * @author claude */
@Service
public class HyDEDomainService {

    private static final Logger log = LoggerFactory.getLogger(HyDEDomainService.class);

    /** HyDE提示词模板 */
    private static final String HYDE_PROMPT_TEMPLATE = """
            根据以下问题，生成一个详细的专业回答，用于文档检索：

            问题：{{query}}

            请生成一个包含相关专业术语和概念的回答（150-300字），确保回答准确、专业且包含可能在相关文档中出现的关键词：
            """;


    private final UserModelConfigResolver userModelConfigResolver;

    public HyDEDomainService(UserModelConfigResolver userModelConfigResolver) {
        this.userModelConfigResolver = userModelConfigResolver;
    }

    /** 生成假设文档 使用用户配置的LLM根据查询问题生成假设文档，用于改善向量检索效果
     * 
     * @param query 用户查询问题
     * @param chatModelConfig 聊天模型配置
     * @return 生成的假设文档文本，生成失败时返回原始查询 */
    public String generateHypotheticalDocument(String query, ModelConfig chatModelConfig) {

        if (!shouldUseHyde(query)) {
            return query;
        }
        String trimmedQuery = query.trim();

        try {
            log.debug("开始HyDE生成，查询: '{}', 模型: {}", trimmedQuery, chatModelConfig.getModelId());

            // 通过基础设施层创建ChatModel实例
            ChatModel chatModel = userModelConfigResolver.createChatModel(chatModelConfig);

            // 构建提示词
            String promptText = HYDE_PROMPT_TEMPLATE.replace("{{query}}", trimmedQuery);
            UserMessage userMessage = new UserMessage(promptText);

            // 直接生成假设文档
            ChatResponse response = chatModel.chat(userMessage);
            String hypotheticalDocument = response.aiMessage().text().trim();

            log.info("HyDE生成成功，查询: '{}', 生成文档长度: {}", trimmedQuery, hypotheticalDocument.length());

            return hypotheticalDocument;

        } catch (Exception e) {
            log.warn("HyDE生成失败，查询: '{}', 错误: {}, 回退到原始查询", trimmedQuery, e.getMessage());
            return trimmedQuery;
        }
    }


    /** 检查是否适合使用HyDE 根据查询特征判断是否应该使用HyDE生成
     * 
     * @param query 用户查询
     * @return 是否适合使用HyDE */
    private boolean shouldUseHyde(String query) {
        if (!StringUtils.hasText(query)) {
            log.warn("HyDE生成失败：查询问题为空");
            return false;
        }

        String trimmedQuery = query.trim();

        // 查询过长时不使用HyDE（可能已经很详细）
        if (trimmedQuery.length() > 200) {
            log.debug("查询过长，跳过HyDE: '{}'", trimmedQuery.substring(0, 50) + "...");
            return false;
        }

        return true;
    }
}