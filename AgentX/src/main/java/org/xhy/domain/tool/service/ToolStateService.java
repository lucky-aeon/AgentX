package org.xhy.domain.tool.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.tool.crontab.ToolProcessMonitor;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.repository.ToolRepository;
import org.xhy.domain.tool.service.state.ToolStateProcessor;
import org.xhy.domain.tool.service.state.impl.DeployingProcessor;
import org.xhy.domain.tool.service.state.impl.FetchingToolsProcessor;
import org.xhy.domain.tool.service.state.impl.GithubUrlValidateProcessor;
import org.xhy.domain.tool.service.state.impl.ManualReviewProcessor;
import org.xhy.domain.tool.service.state.impl.PublishingProcessor;
import org.xhy.domain.tool.service.state.impl.WaitingReviewProcessor;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.github.GitHubService;
import org.xhy.infrastructure.mcp_gateway.MCPGatewayService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class ToolStateService {

    private static final Logger logger = LoggerFactory.getLogger(ToolStateService.class);

    private final ToolRepository toolRepository;
    private final GitHubService gitHubService;
    private final MCPGatewayService mcpGatewayService;

    private final Map<ToolStatus, ToolStateProcessor> processorMap = new HashMap<>();
    private final ExecutorService executorService;

    public ToolStateService(ToolRepository toolRepository, GitHubService gitHubService,
            MCPGatewayService mcpGatewayService) {
        this.toolRepository = toolRepository;
        this.gitHubService = gitHubService;
        this.mcpGatewayService = mcpGatewayService;
        this.executorService = new ThreadPoolExecutor(5, 10, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), r -> {
            Thread t = new Thread(r, "tool-state-processor-thread");
            t.setDaemon(true);
            return t;
        }, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @PostConstruct
    public void init() {
        registerProcessor(new WaitingReviewProcessor());
        registerProcessor(new GithubUrlValidateProcessor());
        registerProcessor(new DeployingProcessor(mcpGatewayService));
        registerProcessor(new FetchingToolsProcessor(mcpGatewayService));
        registerProcessor(new PublishingProcessor(gitHubService));
        registerProcessor(new ManualReviewProcessor());
        logger.info("工具状态处理器初始化完成，已注册 {} 个处理器。", processorMap.size());
    }

    private void registerProcessor(ToolStateProcessor processor) {
        if (processorMap.containsKey(processor.getStatus())) {
            logger.warn("状态 {} 的处理器已被覆盖。原处理器: {}, 新处理器: {}", processor.getStatus(),
                    processorMap.get(processor.getStatus()).getClass().getName(), processor.getClass().getName());
        }
        processorMap.put(processor.getStatus(), processor);
    }

    public void submitToolForProcessing(ToolEntity toolEntity) {
        if (toolEntity == null) {
            throw new BusinessException("工具不存在");
        }
        logger.info("提交工具 ID: {} (当前状态: {}) 到状态处理队列。", toolEntity.getId(), toolEntity.getStatus());
        // 用线程池异步执行，避免上传接口被长耗时流程阻塞
        executorService.submit(() -> processToolState(toolEntity));
    }

    @Transactional
    public void manualReviewComplete(ToolEntity tool, boolean approved) {
        if (tool == null) {
            throw new BusinessException("工具不存在");
        }
        String toolId = tool.getId();
        if (tool.getStatus() != ToolStatus.MANUAL_REVIEW) {
            logger.warn("工具 ID: {} 当前状态不是 MANUAL_REVIEW ({})，忽略人工审核完成操作。", toolId, tool.getStatus());
            return;
        }

        if (approved) {
            tool.setStatus(ToolStatus.PUBLISHING);
            logger.info("工具 ID: {} 人工审核通过，状态更新为 PUBLISHING。", toolId);
            submitToolForProcessing(tool);
        } else {
            tool.setStatus(ToolStatus.FAILED);
            logger.info("工具 ID: {} 人工审核失败，状态更新为 FAILED。", toolId);
        }
    }

    public void processToolState(ToolEntity toolEntity) {
        final ToolStatus initialStatus = toolEntity.getStatus();
        ToolStateProcessor processor = processorMap.get(initialStatus);

        if (processor == null) {
            logger.warn("工具 ID: {} 当前状态 {} 没有对应的状态处理器，流程终止。", toolEntity.getId(), initialStatus);
            return;
        }

        logger.info("开始处理工具 ID: {} 的状态 {}", toolEntity.getId(), initialStatus);
        ToolProcessMonitor.recordToolState(toolEntity.getId(), initialStatus);

        try {
            // 1. 执行当前状态的处理逻辑
            processor.process(toolEntity);

            if (initialStatus == ToolStatus.PUBLISHING) {
                logger.info("工具 ID: {} 的 PUBLISHING 状态处理完成。", toolEntity.getId());
                return;
            }

            // 2. 状态处理器声明下一步，状态机自身只负责推进和落库
            ToolStatus nextStatusCandidate = processor.getNextStatus();

            if (nextStatusCandidate != null && nextStatusCandidate != initialStatus) {
                toolEntity.setStatus(nextStatusCandidate);
                toolRepository.updateById(toolEntity);
                logger.info("工具 ID: {} 状态从 {} 更新为 {}。", toolEntity.getId(), initialStatus, nextStatusCandidate);

                if (nextStatusCandidate == ToolStatus.MANUAL_REVIEW) {
                    // 到人工审核时暂停自动流转，等待管理员接口继续驱动
                    logger.info("工具 ID: {} 进入 MANUAL_REVIEW 状态，等待人工审核。", toolEntity.getId());
                    return;
                }

                // 非人工节点可以连续自动推进
                processToolState(toolEntity);
            } else {
                logger.info("工具 ID: {} 在状态 {} 处理完成，没有自动进入下一状态。", toolEntity.getId(), initialStatus);
            }
        } catch (Exception e) {
            logger.error("处理工具 ID: {} 的状态 {} 时发生错误: {}", toolEntity.getId(), initialStatus, e.getMessage(), e);

            ToolStatus failureStatus = (initialStatus == ToolStatus.PUBLISHING)
                    ? ToolStatus.PUBLISH_FAILED
                    : ToolStatus.FAILED;
            toolEntity.setStatus(failureStatus);
            toolEntity.setFailedStepStatus(initialStatus);
            toolEntity.setRejectReason("状态处理失败: " + e.getMessage());

            toolRepository.updateById(toolEntity);
            logger.info("工具 ID: {} 状态已更新为 {}，失败步骤: {}。", toolEntity.getId(), toolEntity.getStatus(), initialStatus);
        } finally {
            // 到终态或人工审核态时，从运行监控移除，避免恢复任务重复接管
            if (ToolStatus.isTerminalStatus(toolEntity.getStatus())
                    || toolEntity.getStatus() == ToolStatus.MANUAL_REVIEW) {
                ToolProcessMonitor.recordToolStateTermination(toolEntity);
            }
        }
    }
}
