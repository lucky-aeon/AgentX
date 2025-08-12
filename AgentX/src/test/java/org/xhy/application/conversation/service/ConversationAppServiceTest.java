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
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//@SpringBootTest
public class ConversationAppServiceTest {

    // @Autowired
    // private ConversationAppService conversationAppService;

    public static void main(String[] args) {
        // testSameMessageBodyTokenWithDiffHistory();
        testSingleMessageBodyTokenWithAgentPrompt();
    }

    private static void testSingleMessageBodyTokenWithAgentPrompt() {
        List<String> messageTextList = List.of("跟他差不多水平的歌手有谁");
        String systemMessageText = "你是一个有用的AI助手。"; // token: 8
        String summaryPrefix = "以下是用户历史消息的摘要，请仅作为参考，用户没有提起则不要回答摘要中的内容：\\n"; // token: 26
        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入API密钥: ");
        String apiKey = scanner.nextLine();
        scanner.close();
        ProviderConfig providerConfig = new ProviderConfig(apiKey, "https://api.siliconflow.cn/v1",
                "deepseek-ai/DeepSeek-V3", ProviderProtocol.OPENAI);
        // 使用当前服务商调用大模型
        ChatModel chatLanguageModel = LLMProviderService.getStrand(providerConfig.getProtocol(), providerConfig);
        SystemMessage systemMessage = new SystemMessage(systemMessageText);
        sendMessage("singleMessage", messageTextList, systemMessage, chatLanguageModel);
    }

    /** 可以看到消息本体token在历史消息中算得的本体token略小于没有历史消息单独计算的本体token，但是误差不大，作为token策略来说，可以忽略
     * @return 空 */
    private static void testSameMessageBodyTokenWithDiffHistory() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入API密钥: ");
        String apiKey = scanner.nextLine();
        scanner.close();
        String sameMessageText = "跟他差不多水平的歌手有谁"; // singleWithAgentPromptToken: 15
        List<String> messageTextList1 = List.of("我是真的真的真的真的喜欢腻啊，坤坤", sameMessageText); // diff: 20 total: 28
        List<String> messageTextList2 = List.of("时代少年团，我们喜欢腻，我们稀饭倪啊，我们喜欢泥", sameMessageText); // diff: 26 total: 34
        List<List<String>> messages4Testing = new ArrayList<>();
        messages4Testing.add(messageTextList1);
        messages4Testing.add(messageTextList2);
        String systemMessageText = "你是一个有用的AI助手。"; // token: 8
        ProviderConfig providerConfig = new ProviderConfig(apiKey, "https://api.siliconflow.cn/v1",
                "deepseek-ai/DeepSeek-V3", ProviderProtocol.OPENAI);
        // 使用当前服务商调用大模型
        ChatModel chatLanguageModel = LLMProviderService.getStrand(providerConfig.getProtocol(), providerConfig);
        SystemMessage systemMessage = new SystemMessage(systemMessageText);
        ExecutorService executorService = Executors.newFixedThreadPool(messages4Testing.size());
        for (int i = 0; i < messages4Testing.size(); i++) {
            List<String> messageTextList = messages4Testing.get(i);
            int finalI = i;
            executorService.submit(() -> {
                sendMessage(finalI + "", messageTextList, systemMessage, chatLanguageModel);
            });
        }
        executorService.shutdown();
        try {
            // 等待所有任务执行完成
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                // 如果超时则强制关闭
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            // 如果等待过程中被中断，则强制关闭
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static void sendMessage(String logName, List<String> messageTextList1, SystemMessage systemMessage,
            ChatModel chatLanguageModel) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (String messageText : messageTextList1) {
            chatMessages.add(new UserMessage(messageText));
        }
        chatMessages.add(systemMessage);
        ChatResponse chatResponse = chatLanguageModel.chat(chatMessages);
        System.out.println(logName + " input_token_count: " + chatResponse.tokenUsage().inputTokenCount());
    }
}
