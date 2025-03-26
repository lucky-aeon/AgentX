package org.xhy.domain.llm.model;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public enum ProviderType {
    OPENAI("openai", "OpenAI", OpenAiChatModel.class),
    ANTHROPIC("anthropic", "Anthropic", null), // TODO: 添加其他模型的支持
    GEMINI("gemini", "Google Gemini", null);
    
    private final String code;
    private final String name;
    private final Class<? extends ChatLanguageModel> modelClass;
    
    ProviderType(String code, String name, Class<? extends ChatLanguageModel> modelClass) {
        this.code = code;
        this.name = name;
        this.modelClass = modelClass;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Class<? extends ChatLanguageModel> getModelClass() {
        return modelClass;
    }
} 