package org.xhy.infrastructure.llm.factory;


import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;


public class LLMProviderFactory {


    /**
     * 获取对应的服务商
     * 不使用工厂模式，因为 OpenAiChatModel 没有无参构造器，并且其他类型的模型不能适配
     * @param protocol 协议
     * @param providerConfig 服务商信息
     */
    public static ChatLanguageModel getLLMProvider(ProviderProtocol protocol, ProviderConfig providerConfig){
        ChatLanguageModel model = null;
        if (protocol == ProviderProtocol.OpenAI){
            OpenAiChatModel.OpenAiChatModelBuilder openAiChatModelBuilder = new OpenAiChatModel.OpenAiChatModelBuilder();
            openAiChatModelBuilder.apiKey(providerConfig.getApiKey());
            openAiChatModelBuilder.baseUrl(providerConfig.getBaseUrl());
            openAiChatModelBuilder.customHeaders(providerConfig.getCustomHeaders());
            model = new OpenAiChatModel(openAiChatModelBuilder);
        }
        return model;
    }

    public static StreamingChatLanguageModel getLLMProviderByStream(ProviderProtocol protocol, ProviderConfig providerConfig){
        StreamingChatLanguageModel model = null;
        if (protocol == ProviderProtocol.OpenAI){
            OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder openAiStreamingChatModelBuilder = new OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder();
            openAiStreamingChatModelBuilder.apiKey(providerConfig.getApiKey());
            openAiStreamingChatModelBuilder.baseUrl(providerConfig.getBaseUrl());
            openAiStreamingChatModelBuilder.customHeaders(providerConfig.getCustomHeaders());
            openAiStreamingChatModelBuilder.modelName(providerConfig.getModel());
            model = new OpenAiStreamingChatModel(openAiStreamingChatModelBuilder);
        }
        return model;
    }
}
