package org.xhy.application.conversation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.container.service.McpGatewayService;
import org.xhy.application.container.dto.ContainerDTO;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;

/** Agent对话流程服务 - 实现流程图中的完整业务逻辑 */
@Service
public class AgentConversationFlowService {

    private static final Logger logger = LoggerFactory.getLogger(AgentConversationFlowService.class);

    private final AgentToolAnalysisService agentToolAnalysisService;
    private final ContainerIntegrationAppService containerIntegrationAppService;

    public AgentConversationFlowService(AgentToolAnalysisService agentToolAnalysisService,
                                      ContainerIntegrationAppService containerIntegrationAppService) {
        this.agentToolAnalysisService = agentToolAnalysisService;
        this.containerIntegrationAppService = containerIntegrationAppService;
    }

    /** 开始Agent对话流程（实现流程图逻辑）
     * 
     * @param agentId Agent ID
     * @param userId 用户ID
     * @return 对话流程准备结果 */
    public ConversationFlowResult startAgentConversationFlow(String agentId, String userId) {
        try {
            logger.info("开始Agent对话流程: agentId={}, userId={}", agentId, userId);

            // 1. 分析Agent工具需求
            AgentToolAnalysisService.ToolDeploymentAnalysis toolAnalysis = 
                    agentToolAnalysisService.analyzeToolDeploymentRequirements(agentId, userId);

            // 2. 检查是否有工具
            if (!toolAnalysis.hasAnyTools()) {
                logger.info("Agent没有工具，直接进入对话: agentId={}", agentId);
                return new ConversationFlowResult(true, "Agent无工具，直接开始对话", null, toolAnalysis);
            }

            // 3. 检查是否需要容器（有非全局工具）
            if (!toolAnalysis.needsContainer()) {
                logger.info("Agent只有全局工具，无需容器: agentId={}, 全局工具数量={}", 
                           agentId, toolAnalysis.getGlobalTools().size());
                return new ConversationFlowResult(true, "仅使用全局工具，无需容器", null, toolAnalysis);
            }

            // 4. 检查用户容器状态
            ContainerIntegrationAppService.ContainerCheckResult containerCheck = 
                    containerIntegrationAppService.checkUserContainerStatus(userId);

            ContainerDTO container = null;

            // 5. 如果容器不存在，创建容器
            if (!containerCheck.isContainerExists()) {
                logger.info("用户容器不存在，创建新容器: userId={}", userId);
                ContainerIntegrationAppService.ContainerCreationResult creationResult = 
                        containerIntegrationAppService.createUserContainerIfNeeded(userId);
                
                if (!creationResult.isSuccess()) {
                    return new ConversationFlowResult(false, "容器创建失败: " + creationResult.getMessage(), 
                                                    null, toolAnalysis);
                }
                container = creationResult.getContainer();
            } else if (!containerCheck.isHealthy()) {
                return new ConversationFlowResult(false, "容器状态异常: " + containerCheck.getMessage(), 
                                                containerCheck.getContainer(), toolAnalysis);
            } else {
                container = containerCheck.getContainer();
            }

            // 6. 部署工具到用户容器
            List<McpGatewayService.ToolConfig> toolConfigs = 
                    agentToolAnalysisService.getToolConfigsForDeployment(toolAnalysis);

            if (!toolConfigs.isEmpty()) {
                // 检查哪些工具需要部署
                List<McpGatewayService.ToolConfig> toolsToDeployList = 
                        containerIntegrationAppService.checkToolsNeedDeployment(userId, toolConfigs);

                if (!toolsToDeployList.isEmpty()) {
                    logger.info("开始部署工具到用户容器: userId={}, 工具数量={}", userId, toolsToDeployList.size());
                    ContainerIntegrationAppService.ToolDeploymentResult deploymentResult = 
                            containerIntegrationAppService.deployToolsToUserWorkspace(userId, toolsToDeployList);

                    if (!deploymentResult.isSuccess()) {
                        logger.warn("工具部署部分失败: userId={}, 成功={}, 失败={}", 
                                   userId, deploymentResult.getSuccessCount(), deploymentResult.getFailedCount());
                        // 注意：即使部分失败，也可以继续对话，只是某些工具不可用
                    }
                } else {
                    logger.info("所有工具已部署，无需重复部署: userId={}", userId);
                }
            }

            // 7. 准备完成，可以开始对话
            logger.info("Agent对话流程准备完成: agentId={}, userId={}, 全局工具={}, 非全局工具={}", 
                       agentId, userId, toolAnalysis.getGlobalTools().size(), toolAnalysis.getNonGlobalTools().size());

            return new ConversationFlowResult(true, "对话流程准备完成", container, toolAnalysis);

        } catch (Exception e) {
            logger.error("Agent对话流程失败: agentId={}, userId={}", agentId, userId, e);
            return new ConversationFlowResult(false, "对话流程失败: " + e.getMessage(), null, null);
        }
    }

    /** 获取Agent可用的MCP服务器名称列表（用于对话中的工具调用）
     * 
     * @param agentId Agent ID
     * @param userId 用户ID
     * @return MCP服务器名称列表 */
    public List<String> getAgentMcpServerNames(String agentId, String userId) {
        try {
            AgentToolAnalysisService.ToolDeploymentAnalysis toolAnalysis = 
                    agentToolAnalysisService.analyzeToolDeploymentRequirements(agentId, userId);

            // 返回所有可用工具的MCP服务器名称
            List<String> mcpServerNames = new java.util.ArrayList<>();
            
            // 添加全局工具
            toolAnalysis.getGlobalTools().forEach(tool -> 
                    mcpServerNames.add(tool.getMcpServerName()));
            
            // 添加非全局工具（已部署到用户容器的）
            toolAnalysis.getNonGlobalTools().forEach(tool -> 
                    mcpServerNames.add(tool.getMcpServerName()));

            logger.info("获取Agent MCP服务器列表: agentId={}, userId={}, 数量={}", 
                       agentId, userId, mcpServerNames.size());

            return mcpServerNames;

        } catch (Exception e) {
            logger.error("获取Agent MCP服务器列表失败: agentId={}, userId={}", agentId, userId, e);
            throw new BusinessException("获取工具列表失败: " + e.getMessage());
        }
    }

    /** 对话流程结果 */
    public static class ConversationFlowResult {
        private final boolean success;
        private final String message;
        private final ContainerDTO container;
        private final AgentToolAnalysisService.ToolDeploymentAnalysis toolAnalysis;

        public ConversationFlowResult(boolean success, String message, ContainerDTO container,
                                    AgentToolAnalysisService.ToolDeploymentAnalysis toolAnalysis) {
            this.success = success;
            this.message = message;
            this.container = container;
            this.toolAnalysis = toolAnalysis;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public ContainerDTO getContainer() {
            return container;
        }

        public AgentToolAnalysisService.ToolDeploymentAnalysis getToolAnalysis() {
            return toolAnalysis;
        }

        public boolean needsContainer() {
            return toolAnalysis != null && toolAnalysis.needsContainer();
        }

        public boolean hasAnyTools() {
            return toolAnalysis != null && toolAnalysis.hasAnyTools();
        }
    }
}