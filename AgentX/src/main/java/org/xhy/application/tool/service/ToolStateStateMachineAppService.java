package org.xhy.application.tool.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.container.service.ReviewContainerService;
import org.xhy.application.tool.service.state.AppToolStateProcessor;
import org.xhy.application.tool.service.state.impl.AppDeployingProcessor;
import org.xhy.application.tool.service.state.impl.AppFetchingToolsProcessor;
import org.xhy.application.tool.service.state.impl.AppGithubUrlValidateProcessor;
import org.xhy.application.tool.service.state.impl.AppManualReviewProcessor;
import org.xhy.application.tool.service.state.impl.AppPublishingProcessor;
import org.xhy.application.tool.service.state.impl.AppWaitingReviewProcessor;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.ToolDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.github.GitHubService;
import org.xhy.infrastructure.mcp_gateway.MCPGatewayService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 工具状态机应用服务 - 统一管理工具状态转换
 * 
 * 职责： 1. 管理需要外部依赖的状态处理器（如调用基础设施层服务） 2. 协调领域层状态机和应用层状态处理 3. 提供统一的状态转换入口 */
@Service
public class ToolStateStateMachineAppService {

    private static final Logger logger = LoggerFactory.getLogger(ToolStateStateMachineAppService.class);

    private final ToolDomainService toolDomainService;
    private final MCPGatewayService mcpGatewayService;
    private final GitHubService gitHubService;
    private final ReviewContainerService reviewContainerService;

    private final Map<ToolStatus, AppToolStateProcessor> appProcessorMap = new HashMap<>();
    private final ExecutorService executorService;

    public ToolStateStateMachineAppService(ToolDomainService toolDomainService, MCPGatewayService mcpGatewayService,
            GitHubService gitHubService, ReviewContainerService reviewContainerService) {
        this.toolDomainService = toolDomainService;
        this.mcpGatewayService = mcpGatewayService;
        this.gitHubService = gitHubService;
        this.reviewContainerService = reviewContainerService;

        // 创建线程池用于异步状态处理
        this.executorService = new ThreadPoolExecutor(5, // 核心线程数
                10, // 最大线程数
                60L, // 空闲线程存活时间
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(), r -> {
                    Thread t = new Thread(r, "app-tool-state-processor-thread");
                    t.setDaemon(true);
                    return t;
                }, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /** 初始化应用层状态处理器 */
    @PostConstruct
    public void init() {
        // 注册状态处理器（按状态流转顺序）
        registerAppProcessor(new AppWaitingReviewProcessor());
        registerAppProcessor(new AppGithubUrlValidateProcessor(gitHubService));
        registerAppProcessor(new AppDeployingProcessor(mcpGatewayService, reviewContainerService));
        registerAppProcessor(new AppFetchingToolsProcessor(mcpGatewayService, reviewContainerService));
        registerAppProcessor(new AppManualReviewProcessor());
        registerAppProcessor(new AppPublishingProcessor(gitHubService));

        logger.info("工具状态处理器初始化完成，已注册 {} 个处理器。", appProcessorMap.size());
    }

    /** 注册应用层状态处理器
     *
     * @param processor 状态处理器 */
    private void registerAppProcessor(AppToolStateProcessor processor) {
        if (appProcessorMap.containsKey(processor.getStatus())) {
            logger.warn("状态 {} 的处理器已被覆盖。原处理器: {}, 新处理器: {}", processor.getStatus(),
                    appProcessorMap.get(processor.getStatus()).getClass().getName(), processor.getClass().getName());
        }
        appProcessorMap.put(processor.getStatus(), processor);
    }

    /** 提交工具进行状态处理（统一入口）
     *
     * @param toolEntity 工具实体 */
    public void submitToolForProcessing(ToolEntity toolEntity) {
        if (toolEntity == null) {
            throw new BusinessException("工具不存在");
        }

        logger.info("提交工具ID: {} (当前状态: {}) 到状态处理队列。", toolEntity.getId(), toolEntity.getStatus());
        executorService.submit(() -> processToolState(toolEntity));
    }

    /** 处理工具状态转换（核心逻辑）
     *
     * @param toolEntity 工具实体 */
    public void processToolState(ToolEntity toolEntity) {
        final ToolStatus currentStatus = toolEntity.getStatus();

        AppToolStateProcessor appProcessor = appProcessorMap.get(currentStatus);

        if (appProcessor != null) {
            processProcessor(toolEntity, appProcessor);
        }
    }

    /** 使用应用层处理器处理状态
     *
     * @param toolEntity 工具实体
     * @param processor 应用层状态处理器 */
    private void processProcessor(ToolEntity toolEntity, AppToolStateProcessor processor) {
        final ToolStatus initialStatus = toolEntity.getStatus();

        logger.info("开始处理工具ID: {} 的状态: {}", toolEntity.getId(), initialStatus);

        try {
            // 执行状态处理
            processor.process(toolEntity);

            // 获取下一个状态
            ToolStatus nextStatus = processor.getNextStatus();

            if (nextStatus != null && nextStatus != initialStatus) {
                // 更新状态并持久化
                toolEntity.setStatus(nextStatus);
                toolDomainService.updateToolEntity(toolEntity);

                logger.info("工具ID: {} 状态从 {} 更新为 {}。", toolEntity.getId(), initialStatus, nextStatus);

                // 如果进入手动审核状态，暂停自动流转
                if (nextStatus == ToolStatus.MANUAL_REVIEW) {
                    logger.info("工具ID: {} 进入MANUAL_REVIEW状态，等待人工审核。", toolEntity.getId());
                    return;
                }

                // 递归处理下一个状态
                processToolState(toolEntity);
            } else {
                logger.info("工具ID: {} 在状态 {} 处理完成，没有自动的下一状态或状态未改变。", toolEntity.getId(), initialStatus);
            }
        } catch (Exception e) {
            logger.error("处理工具ID: {} 的状态 {} 时发生错误: {}", toolEntity.getId(), initialStatus, e.getMessage(), e);

            // 更新为失败状态
            toolEntity.setStatus(ToolStatus.FAILED);
            toolEntity.setFailedStepStatus(initialStatus);
            toolEntity.setRejectReason("状态处理失败: " + e.getMessage());

            toolDomainService.updateToolEntity(toolEntity);

            logger.info("工具ID: {} 状态已更新为 {}，失败步骤: {}，原因: {}", toolEntity.getId(), toolEntity.getStatus(), initialStatus,
                    e.getMessage());
        }
    }

    /** 处理人工审核完成
     *
     * @param tool 工具实体
     * @param approved 是否通过审核
     * @return 工具ID */
    public String manualReviewComplete(ToolEntity tool, boolean approved) {
        String toolId = toolDomainService.manualReviewComplete(tool, approved);

        if (approved) {
            logger.info("工具ID: {} 人工审核通过，状态更新为 APPROVED。", toolId);
            // 继续状态处理流程
            submitToolForProcessing(tool);
        } else {
            logger.info("工具ID: {} 人工审核失败，状态更新为 FAILED。", toolId);
        }

        return toolId;
    }

}