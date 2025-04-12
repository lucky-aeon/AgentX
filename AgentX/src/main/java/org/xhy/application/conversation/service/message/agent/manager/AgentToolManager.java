package org.xhy.application.conversation.service.message.agent.manager;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent工具管理器
 * 负责创建和管理工具提供者
 */
@Component
public class AgentToolManager {
    /**
     * 创建工具提供者
     * 
     * @param toolUrls 工具URL列表
     * @return 工具提供者实例，如果工具列表为空则返回null
     */
    public ToolProvider createToolProvider(List<String> toolUrls) {
        if (toolUrls == null || toolUrls.isEmpty()) {
            return null;
        }
        
        List<McpClient> mcpClients = new ArrayList<>();
        
        for (String toolUrl : toolUrls) {
            McpTransport transport = new HttpMcpTransport.Builder()
                    .sseUrl(toolUrl)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
            
            McpClient mcpClient = new DefaultMcpClient.Builder()
                    .transport(transport)
                    .build();
            
            mcpClients.add(mcpClient);
        }
        
        return McpToolProvider.builder()
                .mcpClients(mcpClients)
                .build();
    }
    
    /**
     * 获取可用的工具列表
     * 
     * @return 工具URL列表
     */
    public List<String> getAvailableTools() {
        // 这里可以从配置、数据库等获取可用工具
        return new ArrayList<>();
    }
} 