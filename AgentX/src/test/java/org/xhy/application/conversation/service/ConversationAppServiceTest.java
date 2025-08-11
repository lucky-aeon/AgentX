package org.xhy.application.conversation.service;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

//@SpringBootTest
public class ConversationAppServiceTest {

    // @Autowired
    // private ConversationAppService conversationAppService;

    public static void main(String[] args) {
        List<String> messageTextList = List.of("HashMap在JVM中的内存布局如何"); // 11
        String systemMessageText = "你是一个有用的AI助手。"; // token: 8
        String summaryPrefix = "以下是用户历史消息的摘要，请仅作为参考，用户没有提起则不要回答摘要中的内容：\\n"; // token: 26
        ProviderConfig providerConfig = new ProviderConfig("",
                "https://api.siliconflow.cn/v1", "deepseek-ai/DeepSeek-V3", ProviderProtocol.OPENAI);
        // 使用当前服务商调用大模型
        ChatModel chatLanguageModel = LLMProviderService.getStrand(providerConfig.getProtocol(), providerConfig);
        SystemMessage systemMessage = new SystemMessage(systemMessageText);
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (String messageText : messageTextList) {
            chatMessages.add(new UserMessage(messageText));
        }
        chatMessages.add(systemMessage);
        ChatResponse chatResponse = chatLanguageModel.chat(chatMessages);
        System.out.println("input_token_count: " + chatResponse.tokenUsage().inputTokenCount());
    }
}
