package org.xhy.infrastructure.llm.factory;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import java.time.Duration;

public class LLMProviderFactory {

    /** 获取对应的服务商 不使用工厂模式，因为 OpenAiChatModel 没有无参构造器，并且其他类型的模型不能适配
     * @param protocol 协议
     * @param providerConfig 服务商信息 */
    public static ChatLanguageModel getLLMProvider(ProviderProtocol protocol, ProviderConfig providerConfig) {
        ChatLanguageModel model = null;
        if (protocol == ProviderProtocol.OpenAI) {
            OpenAiChatModel.OpenAiChatModelBuilder openAiChatModelBuilder = new OpenAiChatModel.OpenAiChatModelBuilder();
            openAiChatModelBuilder.apiKey(providerConfig.getApiKey());
            openAiChatModelBuilder.baseUrl(providerConfig.getBaseUrl());
            openAiChatModelBuilder.customHeaders(providerConfig.getCustomHeaders());
            openAiChatModelBuilder.modelName(providerConfig.getModel());
            openAiChatModelBuilder.timeout(Duration.ofHours(1));
            model = new OpenAiChatModel(openAiChatModelBuilder);
        } else if (protocol == ProviderProtocol.ANTHROPIC) {
            model = AnthropicChatModel.builder().apiKey(providerConfig.getApiKey()).baseUrl(providerConfig.getBaseUrl())
                    .modelName(providerConfig.getModel()).version("2023-06-01").timeout(Duration.ofHours(1)).build();
        }
        return model;
    }

    public static StreamingChatLanguageModel getLLMProviderByStream(ProviderProtocol protocol,
            ProviderConfig providerConfig) {
        StreamingChatLanguageModel model = null;
        if (protocol == ProviderProtocol.OpenAI) {
            model = new OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder().apiKey(providerConfig.getApiKey())
                    .baseUrl(providerConfig.getBaseUrl()).customHeaders(providerConfig.getCustomHeaders())
                    .modelName(providerConfig.getModel()).timeout(Duration.ofHours(1)).build();
        } else if (protocol == ProviderProtocol.ANTHROPIC) {
            model = AnthropicStreamingChatModel.builder().apiKey(providerConfig.getApiKey())
                    .baseUrl(providerConfig.getBaseUrl()).version("2023-06-01").modelName(providerConfig.getModel())
                    .timeout(Duration.ofHours(1)).build();
        }

        return model;
    }
}
