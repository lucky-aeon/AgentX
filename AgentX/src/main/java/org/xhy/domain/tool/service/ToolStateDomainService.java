package org.xhy.domain.tool.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

/** 工具状态流转服务。 管理工具在不同状态间的转换，并执行各状态对应的处理逻辑。 */
@Service
public class ToolStateDomainService {

    private static final Logger logger = LoggerFactory.getLogger(ToolStateDomainService.class);

    private final ToolRepository toolRepository;
    private final GitHubService gitHubService;
    private final MCPGatewayService mcpGatewayService;

    private final Map<ToolStatus, ToolStateProcessor> processorMap = new HashMap<>();
    private final ExecutorService executorService;

    /** 构造函数。
     * 
     * @param toolRepository 工具仓库，用于数据持久化。
     * @param gitHubService GitHub服务，用于与GitHub API交互。 */
    public ToolStateDomainService(ToolRepository toolRepository, GitHubService gitHubService,
            MCPGatewayService mcpGatewayService) {
        this.toolRepository = toolRepository;
        this.gitHubService = gitHubService;
        this.mcpGatewayService = mcpGatewayService;

        // 创建具有无限队列的固定大小线程池用于异步处理状态转换
        this.executorService = new ThreadPoolExecutor(5, // 核心线程数
                10, // 最大线程数
                60L, // 空闲线程存活时间
                TimeUnit.SECONDS, // 时间单位
                new LinkedBlockingQueue<>(), // 无界队列
                r -> {
                    Thread t = new Thread(r, "tool-state-processor-thread");
                    t.setDaemon(true); // 设置为守护线程，以便JVM退出时它们不会阻止退出
                    return t;
                }, new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由提交任务的线程直接执行
        );
    }

    /** 初始化方法，在Bean属性设置完成后调用。 负责注册所有状态处理器。 */
    @PostConstruct
    public void init() {
        // 注册基础状态处理器
        registerProcessor(new WaitingReviewProcessor());
        registerProcessor(new GithubUrlValidateProcessor(gitHubService));
        // 移除或保留 DeployingProcessor 和 FetchingToolsProcessor 取决于它们是否还在流程中
        registerProcessor(new DeployingProcessor(mcpGatewayService));
        registerProcessor(new FetchingToolsProcessor(mcpGatewayService));

        // 移除手动审核状态直接关联PublishingProcessor的注册
        // registerProcessor(ToolStatus.MANUAL_REVIEW,new
        // PublishingProcessor(gitHubService));

        // 注册"发布中"状态处理器
        registerProcessor(new PublishingProcessor(gitHubService));
        registerProcessor(new ManualReviewProcessor());

        logger.info("工具状态处理器初始化完成，已注册 {} 个处理器。", processorMap.size());
    }

    /** 注册一个状态处理器到映射中。
     * 
     * @param processor 要注册的状态处理器。 */
    private void registerProcessor(ToolStateProcessor processor) {
        if (processorMap.containsKey(processor.getStatus())) {
            logger.warn("状态 {} 的处理器已被覆盖。原处理器: {}, 新处理器: {}", processor.getStatus(),
                    processorMap.get(processor.getStatus()).getClass().getName(), processor.getClass().getName());
        }
        processorMap.put(processor.getStatus(), processor);
    }

    /** 提交一个工具，根据其当前状态进行异步处理。
     * 
     * @param toolEntity 要处理的工具的ID。 */
    public void submitToolForProcessing(ToolEntity toolEntity) {
        if (toolEntity == null) {
            throw new BusinessException("工具不存在");
        }

        logger.info("提交工具ID: {} (当前状态: {}) 到状态处理队列。", toolEntity.getId(), toolEntity.getStatus());
        executorService.submit(() -> processToolState(toolEntity));
    }

    /** 处理人工审核完成的工具。 由外部调用（如后台管理系统）来驱动人工审核后的状态流转。
     * 
     * @param tool 要处理的工具。
     * @param approved 审核结果，true表示批准，false表示拒绝。
     * @return 返回审核后的工具ID，方便调用方进行后续处理 */
    @Transactional // 确保状态更新和可能的后续操作在事务中
    public String manualReviewComplete(ToolEntity tool, boolean approved) {
        if (tool == null) {
            throw new BusinessException("工具不存在");
        }
        String toolId = tool.getId();
        if (tool.getStatus() != ToolStatus.MANUAL_REVIEW) {
            logger.warn("工具ID: {} 当前状态不是MANUAL_REVIEW ({})，忽略人工审核完成操作。", toolId, tool.getStatus());
            return toolId;
        }

        if (approved) {
            tool.setStatus(ToolStatus.APPROVED); // 审核通过，进入发布状态
            toolRepository.updateById(tool); // 保存APPROVED状态
            logger.info("工具ID: {} 人工审核通过，状态更新为 APPROVED。", toolId);
            submitToolForProcessing(tool);
        } else {
            tool.setStatus(ToolStatus.FAILED);
            toolRepository.updateById(tool);
            logger.info("工具ID: {} 人工审核失败，状态更新为 FAILED。", toolId);
        }

        return toolId;
    }

    /** 核心状态处理逻辑。 此方法在executorService的线程中异步执行。
     * 
     * @param toolEntity 要处理的工具实体。 */
    public void processToolState(ToolEntity toolEntity) {
        final ToolStatus initialStatus = toolEntity.getStatus(); // Capture status before any changes
        ToolStateProcessor processor = processorMap.get(initialStatus);

        if (processor == null) {
            logger.warn("工具ID: {} 当前状态 {} 没有找到对应的状态处理器。流程终止。", toolEntity.getId(), initialStatus);
            return;
        }

        logger.info("开始处理工具ID: {} 的状态: {}", toolEntity.getId(), initialStatus);

        try {
            processor.process(toolEntity);

            ToolStatus nextStatusCandidate = processor.getNextStatus();

            if (nextStatusCandidate != null && nextStatusCandidate != initialStatus) {
                toolEntity.setStatus(nextStatusCandidate);
                toolRepository.updateById(toolEntity); // 持久化状态变更和 processor 可能对 toolEntity 所做的其他修改
                logger.info("工具ID: {} 状态从 {} 更新为 {}。", toolEntity.getId(), initialStatus, nextStatusCandidate);

                if (nextStatusCandidate == ToolStatus.MANUAL_REVIEW) {
                    logger.info("工具ID: {} 进入MANUAL_REVIEW状态，等待人工审核。", toolEntity.getId());
                    return;
                }
                processToolState(toolEntity);

            } else {
                logger.info("工具ID: {} 在状态 {} 处理完成，没有自动的下一状态或状态未改变。", toolEntity.getId(), initialStatus);
            }
        } catch (Exception e) {
            logger.error("处理工具ID: {} 的状态 {} 时发生错误: {}", toolEntity.getId(), initialStatus, e.getMessage(), e);

            toolEntity.setStatus(ToolStatus.FAILED);
            toolEntity.setFailedStepStatus(initialStatus);
            toolEntity.setRejectReason("状态处理失败: " + e.getMessage());

            toolRepository.updateById(toolEntity);
            logger.info("工具ID: {} 状态已更新为 {}，失败步骤: {}，原因: {}", toolEntity.getId(), toolEntity.getStatus(), initialStatus,
                    e.getMessage());
        }
    }

}