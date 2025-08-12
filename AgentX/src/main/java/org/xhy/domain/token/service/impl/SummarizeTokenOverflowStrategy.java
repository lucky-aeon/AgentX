package org.xhy.domain.token.service.impl;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.xhy.application.conversation.service.handler.context.AgentPromptTemplates;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.token.model.TokenMessage;
import org.xhy.domain.token.model.TokenProcessResult;
import org.xhy.domain.token.model.config.TokenOverflowConfig;
import org.xhy.domain.shared.enums.TokenOverflowStrategyEnum;
import org.xhy.domain.token.service.TokenOverflowStrategy;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 摘要策略Token超限处理实现 将超出阈值的早期消息生成摘要，保留摘要和最新消息 */
public class SummarizeTokenOverflowStrategy implements TokenOverflowStrategy {

    /** 策略配置 */
    private final TokenOverflowConfig config;

    /** 需要进行摘要的消息 */
    private List<TokenMessage> messagesToSummarize;

    /** 生成的摘要消息对象 */
    private TokenMessage summaryMessage;

    /** 构造函数
     * 
     * @param config 策略配置 */
    public SummarizeTokenOverflowStrategy(TokenOverflowConfig config) {
        this.config = config;
        this.messagesToSummarize = new ArrayList<>();
        this.summaryMessage = null;
    }

    /** 处理消息列表，应用摘要策略 将超过阈值的早期消息替换为一个摘要消息并添加到历史消息中，新摘要会追加到摘要记录中并更新创建时间
     * 
     * @param messages 待处理的消息列表
     * @return 处理后的消息列表（包含摘要消息+保留的消息） */
    @Override
    public TokenProcessResult process(List<TokenMessage> messages, TokenOverflowConfig tokenOverflowConfig) {
        if (!needsProcessing(messages)) {
            TokenProcessResult result = new TokenProcessResult();
            result.setRetainedMessages(messages);
            result.setStrategyName(getName());
            result.setProcessed(false);
            result.setTotalTokens(calculateTotalTokens(messages));
            return result;
        }

        // 按时间排序
        List<TokenMessage> sortedMessages = messages.stream().sorted(Comparator.comparing(TokenMessage::getCreatedAt))
                .collect(Collectors.toList());

        // 获取需要保留的消息数量
        int threshold = config.getSummaryThreshold();

        // 分割消息
        messagesToSummarize = sortedMessages.subList(0, sortedMessages.size() - threshold);
        List<TokenMessage> retainedMessages = new ArrayList<>(
                sortedMessages.subList(sortedMessages.size() - threshold, sortedMessages.size()));

        // 生成新的摘要消息
        TokenMessage newSummary = this.generateSummary(messagesToSummarize, tokenOverflowConfig, messages);
        // 添加摘要消息到活跃消息列表
        retainedMessages.add(0, newSummary);
        // 创建结果对象
        TokenProcessResult result = new TokenProcessResult();
        result.setRetainedMessages(retainedMessages);
        result.setSummary(newSummary.getContent());
        result.setStrategyName(getName());
        result.setProcessed(true);
        result.setTotalTokens(calculateTotalTokens(retainedMessages));

        return result;
    }

    /** 获取策略名称
     * 
     * @return 策略名称 */
    @Override
    public String getName() {
        return TokenOverflowStrategyEnum.SUMMARIZE.name();
    }

    /** 判断是否需要进行Token超限处理
     * 
     * @param messages 待处理的消息列表
     * @return 是否需要处理 */
    @Override
    public boolean needsProcessing(List<TokenMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }

        return messages.size() > config.getSummaryThreshold();
    }

    /** 获取需要摘要的消息列表（按时间排序） 这是应用层应该使用的方法，用于获取需要进行摘要处理的消息对象
     * 
     * @return 需要摘要的消息列表（按时间从旧到新排序） */
    public List<TokenMessage> getMessagesToSummarize() {
        return messagesToSummarize;
    }

    /** 生成摘要内容并更新摘要消息记录 */
    private TokenMessage generateSummary(List<TokenMessage> messages, TokenOverflowConfig tokenOverflowConfig,
            List<TokenMessage> historyMessages) {

        ProviderConfig providerConfig = tokenOverflowConfig.getProviderConfig();
        String summaryPrefixPrompt = "。最后请你以这段话作为生成摘要的开头返回，开头：" + AgentPromptTemplates.getSummaryPrefix();

        // 使用当前服务商调用大模型
        ChatModel chatLanguageModel = LLMProviderService.getStrand(providerConfig.getProtocol(), providerConfig);
        SystemMessage systemMessage = new SystemMessage("你是一个专业的对话摘要生成器，请严格按照以下要求工作：\n"
                + "1. 只基于提供的对话内容生成客观摘要，不得添加任何原对话中没有的信息\n" + "2. 特别关注：用户问题、回答中的关键信息、重要事实\n" + "3. 去除所有寒暄、表情符号和情感表达\n"
                + "4. 使用简洁的第三人称陈述句\n" + "5. 保持时间顺序和逻辑关系\n" + "6. 示例格式：[用户]问... [AI]回答...\n" + "禁止使用任何表情符号或拟人化表达"
                + "7. 提供的对话内容中格式与第六点的示例格式相符的，属于旧摘要，旧摘要部分必须全部保留要点" + summaryPrefixPrompt);
        List<Content> contents = messages.stream().map(message -> new TextContent(message.getContent()))
                .collect(Collectors.toList());
        UserMessage userMessage = new UserMessage(contents);
        ChatResponse chatResponse = chatLanguageModel.chat(Arrays.asList(systemMessage, userMessage));
        return this.createNewSummaryMessage(chatResponse.aiMessage().text(),
                chatResponse.tokenUsage().outputTokenCount(), historyMessages);
    }

    /** 创建新的摘要消息记录
     *
     * @param newSummary 摘要内容 */
    private TokenMessage createNewSummaryMessage(String newSummary, Integer newSummaryBodyTokenCount,
            List<TokenMessage> historyMessages) {

        TokenMessage newSummaryMessage = new TokenMessage();
        newSummaryMessage.setRole(Role.SUMMARY.name());
        newSummaryMessage.setContent(newSummary);
        newSummaryMessage.setBodyTokenCount(newSummaryBodyTokenCount);
        newSummaryMessage.setTokenCount(newSummaryBodyTokenCount);

        // 找到历史消息中的最早时间
        LocalDateTime earliestTime = historyMessages.stream()
                .filter(message -> !message.getRole().equals(Role.SUMMARY.name())).map(TokenMessage::getCreatedAt)
                .min(LocalDateTime::compareTo).orElse(LocalDateTime.now());

        // 设置创建时间和更新时间为最早时间的前一秒
        LocalDateTime summaryTime = earliestTime.minusSeconds(1);
        newSummaryMessage.setCreatedAt(summaryTime);
        return newSummaryMessage;
    }

    /** 计算消息列表的总token数 */
    private int calculateTotalTokens(List<TokenMessage> messages) {
        return messages.stream().mapToInt(m -> m.getBodyTokenCount() != null ? m.getBodyTokenCount() : 0).sum();
    }

    /** 获取生成的摘要消息对象
     * 
     * @return 摘要消息对象 */
    public TokenMessage getSummaryMessage() {
        return summaryMessage;
    }
}