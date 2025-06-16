package org.xhy.domain.agent.service;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.stereotype.Service;
import org.xhy.domain.agent.constant.SystemPromptTemplates;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.config.ToolDefinition;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统提示词生成领域服务
 */
@Service
public class SystemPromptGeneratorDomainService {
    
    /**
     * 生成系统提示词
     * 该方法只负责核心的提示词生成逻辑，不涉及其他领域的调用
     */
    public String generateSystemPrompt(String agentName, String agentDescription, 
                                     List<ToolEntity> tools, ChatModel chatModel) {
        
        // 1. 构建生成prompt
        String generationPrompt = buildGenerationPrompt(agentName, agentDescription, tools);
        
        // 2. 调用LLM生成（LLM客户端由应用层传入）
        SystemMessage systemMessage = new SystemMessage(SystemPromptTemplates.SYSTEM_PROMPT_GENERATION_TEMPLATE);
        UserMessage userMessage = new UserMessage(generationPrompt);
        
        ChatResponse response = chatModel.chat(Arrays.asList(systemMessage, userMessage));
        
        // 3. 后处理和验证
        String generatedPrompt = response.aiMessage().text();
        return generatedPrompt;
    }
    
    /**
     * 构建用于生成的提示词
     */
    private String buildGenerationPrompt(String agentName, String agentDescription, List<ToolEntity> tools) {
        // 1. 获取我们的"元提示词"模板
        String metaPromptTemplate = SystemPromptTemplates.SYSTEM_PROMPT_GENERATION_TEMPLATE;

        // 2. 高效地构建助手和工具的概览信息
        StringBuilder overview = new StringBuilder();
        overview.append("名称: ").append(agentName).append("\n");
        overview.append("描述: ").append(agentDescription).append("\n\n");
        overview.append("可用工具概览:\n");

        if (tools != null && !tools.isEmpty()) {
            for (ToolEntity tool : tools) {
                overview.append("- 工具名称: ").append(tool.getName()).append("\n");
                overview.append("  工具描述: ").append(tool.getDescription()).append("\n");
            }
        } else {
            overview.append("该助手当前没有配置任何特定工具。\n");
        }

        // 3. 将模板和信息组合成最终的、发往LLM的完整指令
        return metaPromptTemplate + overview.toString();
    }
    
    /**
     * 验证和清理生成的提示词
     */
    private String validateAndCleanPrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new BusinessException("生成的系统提示词为空");
        }
        
        // 移除可能的格式标记
        String cleaned = prompt.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "");
        }
        
        // 基本长度验证
        if (cleaned.length() < 10) {
            throw new BusinessException("生成的系统提示词过短");
        }
        
        if (cleaned.length() > 5000) {
            throw new BusinessException("生成的系统提示词过长");
        }
        
        return cleaned.trim();
    }
} 