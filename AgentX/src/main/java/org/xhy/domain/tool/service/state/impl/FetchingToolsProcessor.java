package org.xhy.domain.tool.service.state.impl;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.config.ToolDefinition;
import org.xhy.domain.tool.model.config.ToolSpecificationConverter;
import org.xhy.domain.tool.service.state.ToolStateProcessor;
import org.xhy.infrastructure.exception.BusinessException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 获取工具列表处理器
 */
public class FetchingToolsProcessor implements ToolStateProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(FetchingToolsProcessor.class);
    
    @Override
    public ToolStatus getStatus() {
        return ToolStatus.FETCHING_TOOLS;
    }
    
    @Override
    public void process(ToolEntity tool) {
        try {
            // 从installCommand中获取工具名称
            Map<String, Object> installCommand = tool.getInstallCommand();
            if (installCommand == null || installCommand.isEmpty()) {
                throw new BusinessException("安装命令为空");
            }
            
            // 解析mcpServers中的第一个key作为工具名称
            @SuppressWarnings("unchecked")
            Map<String, Object> mcpServers = (Map<String, Object>) installCommand.get("mcpServers");
            if (mcpServers == null || mcpServers.isEmpty()) {
                throw new BusinessException("mcpServers为空");
            }
            
            // 获取第一个key作为工具名称
            String toolName = mcpServers.keySet().iterator().next();
            if (toolName == null || toolName.isEmpty()) {
                throw new BusinessException("无法获取工具名称");
            }

            String url = "http://127.0.0.1:8005/"+toolName+"/sse/sse?api_key=123456";

            HttpMcpTransport transport = new HttpMcpTransport.Builder().sseUrl(url).timeout(Duration.ofHours(1))
                    .logRequests(false).logResponses(true).build();

            McpClient client = new DefaultMcpClient.Builder().transport(transport).build();
            List<ToolSpecification> toolSpecifications = client.listTools();
            List<ToolDefinition> toolDefinitions = ToolSpecificationConverter.convert(toolSpecifications);
            tool.setToolList(toolDefinitions);
        } catch (Exception e) {
            throw new BusinessException("获取工具列表失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.MANUAL_REVIEW;
    }
} 