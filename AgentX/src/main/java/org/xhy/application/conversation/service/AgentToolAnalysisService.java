package org.xhy.application.conversation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.domain.tool.model.UserToolEntity;
import org.xhy.domain.tool.service.UserToolDomainService;
import org.xhy.application.container.service.McpGatewayService;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Agent工具分析服务 - 分析Agent工具部署需求 */
@Service
public class AgentToolAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AgentToolAnalysisService.class);

    private final AgentDomainService agentDomainService;
    private final UserToolDomainService userToolDomainService;

    public AgentToolAnalysisService(AgentDomainService agentDomainService,
                                  UserToolDomainService userToolDomainService) {
        this.agentDomainService = agentDomainService;
        this.userToolDomainService = userToolDomainService;
    }

    /** 分析Agent工具部署需求
     * 
     * @param agentId Agent ID
     * @param userId 用户ID
     * @return 工具部署分析结果 */
    public ToolDeploymentAnalysis analyzeToolDeploymentRequirements(String agentId, String userId) {
        try {
            logger.info("开始分析Agent工具部署需求: agentId={}, userId={}", agentId, userId);

            // 1. 获取Agent信息
            AgentEntity agent = agentDomainService.getAgentById(agentId);
            if (agent == null) {
                throw new BusinessException("Agent不存在: " + agentId);
            }

            List<String> toolIds = agent.getToolIds();
            if (toolIds == null || toolIds.isEmpty()) {
                logger.info("Agent没有配置工具: {}", agentId);
                return new ToolDeploymentAnalysis(false, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            }

            // 2. 获取用户安装的工具
            List<UserToolEntity> userTools = userToolDomainService.getUserToolsByIds(userId, toolIds);
            
            // 3. 分析工具类型
            List<ToolDeploymentInfo> globalTools = new ArrayList<>();
            List<ToolDeploymentInfo> nonGlobalTools = new ArrayList<>();
            List<String> missingTools = new ArrayList<>();

            for (String toolId : toolIds) {
                UserToolEntity userTool = userTools.stream()
                        .filter(ut -> toolId.equals(ut.getToolId()))
                        .findFirst()
                        .orElse(null);

                if (userTool == null) {
                    missingTools.add(toolId);
                    continue;
                }

                ToolDeploymentInfo deploymentInfo = new ToolDeploymentInfo(
                        userTool.getToolId(),
                        userTool.getName(),
                        userTool.getMcpServerName(),
                        userTool.getIsGlobal() != null ? userTool.getIsGlobal() : false
                );

                if (deploymentInfo.isGlobal()) {
                    globalTools.add(deploymentInfo);
                } else {
                    nonGlobalTools.add(deploymentInfo);
                }
            }

            boolean needsContainer = !nonGlobalTools.isEmpty();

            logger.info("工具分析完成: agentId={}, userId={}, 全局工具={}, 非全局工具={}, 缺失工具={}, 需要容器={}",
                       agentId, userId, globalTools.size(), nonGlobalTools.size(), missingTools.size(), needsContainer);

            return new ToolDeploymentAnalysis(needsContainer, globalTools, nonGlobalTools, missingTools);

        } catch (Exception e) {
            logger.error("分析Agent工具部署需求失败: agentId={}, userId={}", agentId, userId, e);
            throw new BusinessException("工具部署分析失败: " + e.getMessage());
        }
    }

    /** 获取需要部署到容器的工具配置
     * 
     * @param deploymentAnalysis 部署分析结果
     * @return 工具配置列表 */
    public List<McpGatewayService.ToolConfig> getToolConfigsForDeployment(ToolDeploymentAnalysis deploymentAnalysis) {
        return deploymentAnalysis.getNonGlobalTools().stream()
                .map(tool -> new McpGatewayService.ToolConfig(
                        tool.getToolId(),
                        tool.getName(),
                        tool.getMcpServerName()
                ))
                .collect(Collectors.toList());
    }

    /** 工具部署分析结果 */
    public static class ToolDeploymentAnalysis {
        private final boolean needsContainer;
        private final List<ToolDeploymentInfo> globalTools;
        private final List<ToolDeploymentInfo> nonGlobalTools;
        private final List<String> missingTools;

        public ToolDeploymentAnalysis(boolean needsContainer, 
                                    List<ToolDeploymentInfo> globalTools,
                                    List<ToolDeploymentInfo> nonGlobalTools,
                                    List<String> missingTools) {
            this.needsContainer = needsContainer;
            this.globalTools = globalTools;
            this.nonGlobalTools = nonGlobalTools;
            this.missingTools = missingTools;
        }

        public boolean needsContainer() {
            return needsContainer;
        }

        public List<ToolDeploymentInfo> getGlobalTools() {
            return globalTools;
        }

        public List<ToolDeploymentInfo> getNonGlobalTools() {
            return nonGlobalTools;
        }

        public List<String> getMissingTools() {
            return missingTools;
        }

        public boolean hasAnyTools() {
            return !globalTools.isEmpty() || !nonGlobalTools.isEmpty();
        }

        public int getTotalToolCount() {
            return globalTools.size() + nonGlobalTools.size();
        }
    }

    /** 工具部署信息 */
    public static class ToolDeploymentInfo {
        private final String toolId;
        private final String name;
        private final String mcpServerName;
        private final boolean isGlobal;

        public ToolDeploymentInfo(String toolId, String name, String mcpServerName, boolean isGlobal) {
            this.toolId = toolId;
            this.name = name;
            this.mcpServerName = mcpServerName;
            this.isGlobal = isGlobal;
        }

        public String getToolId() {
            return toolId;
        }

        public String getName() {
            return name;
        }

        public String getMcpServerName() {
            return mcpServerName;
        }

        public boolean isGlobal() {
            return isGlobal;
        }
    }
}