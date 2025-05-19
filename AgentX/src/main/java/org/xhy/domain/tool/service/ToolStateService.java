package org.xhy.domain.tool.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
import org.xhy.infrastructure.external_services.GitHubService;
import org.xhy.infrastructure.external_services.MCPGatewayService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 工具状态流转服务。
 * 管理工具在不同状态间的转换，并执行各状态对应的处理逻辑。
 */
@Service
public class ToolStateService {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolStateService.class);
    
    private final ToolRepository toolRepository;
    private final GitHubService gitHubService;
    private final MCPGatewayService mcpGatewayService;

    private final Map<ToolStatus, ToolStateProcessor> processorMap = new HashMap<>();
    private final ExecutorService executorService;
    
    /**
     * 构造函数。
     * @param toolRepository 工具仓库，用于数据持久化。
     * @param gitHubService GitHub服务，用于与GitHub API交互。
     */
    public ToolStateService(ToolRepository toolRepository,
                            GitHubService gitHubService,
                            MCPGatewayService mcpGatewayService) {
        this.toolRepository = toolRepository;
        this.gitHubService = gitHubService;
        this.mcpGatewayService = mcpGatewayService;

        // 创建具有无限队列的固定大小线程池用于异步处理状态转换
        this.executorService = new ThreadPoolExecutor(
                5, // 核心线程数
                10, // 最大线程数
                60L, // 空闲线程存活时间
                TimeUnit.SECONDS, // 时间单位
                new LinkedBlockingQueue<>(), // 无界队列
                r -> {
                    Thread t = new Thread(r, "tool-state-processor-thread");
                    t.setDaemon(true); // 设置为守护线程，以便JVM退出时它们不会阻止退出
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由提交任务的线程直接执行
        );
    }
    
    /**
     * 初始化方法，在Bean属性设置完成后调用。
     * 负责注册所有状态处理器。
     */
    @PostConstruct
    public void init() {
        // 注册基础状态处理器
        registerProcessor(new WaitingReviewProcessor());
        registerProcessor(new GithubUrlValidateProcessor());
        // 移除或保留 DeployingProcessor 和 FetchingToolsProcessor 取决于它们是否还在流程中
         registerProcessor(new DeployingProcessor(mcpGatewayService));
         registerProcessor(new FetchingToolsProcessor(mcpGatewayService));
        registerProcessor(new ManualReviewProcessor());

        // 注册"发布中"状态处理器
        registerProcessor(new PublishingProcessor(gitHubService));
        
        logger.info("工具状态处理器初始化完成，已注册 {} 个处理器。", processorMap.size());
    }

    /**
     * 注册一个状态处理器到映射中。
     * @param processor 要注册的状态处理器。
     */
    private void registerProcessor(ToolStateProcessor processor) {
        if (processorMap.containsKey(processor.getStatus())) {
            logger.warn("状态 {} 的处理器已被覆盖。原处理器: {}, 新处理器: {}", 
                        processor.getStatus(), 
                        processorMap.get(processor.getStatus()).getClass().getName(),
                        processor.getClass().getName());
        }
        processorMap.put(processor.getStatus(), processor);
    }
    
    /**
     * 提交一个工具，根据其当前状态进行异步处理。
     * @param toolId 要处理的工具的ID。
     */
    public void submitToolForProcessing(String toolId) {
        ToolEntity tool = toolRepository.selectById(toolId);
        if (tool == null) {
            logger.error("尝试处理不存在的工具，ID: {}", toolId);
            throw new BusinessException("工具不存在: " + toolId);
        }
        
        logger.debug("提交工具ID: {} (当前状态: {}) 到状态处理队列。", tool.getId(), tool.getStatus());
        executorService.submit(() -> processToolState(tool));
    }
    
    /**
     * 核心状态处理逻辑。
     * 此方法在executorService的线程中异步执行。
     * @param toolEntity 要处理的工具实体。
     */
    public void processToolState(ToolEntity toolEntity) {
        final ToolStatus initialStatus = toolEntity.getStatus(); // Capture status before any changes
        ToolStateProcessor processor = processorMap.get(initialStatus);

        if (processor == null) {
            logger.warn("工具ID: {} 当前状态 {} 没有找到对应的状态处理器。流程终止。", toolEntity.getId(), initialStatus);
            return;
        }

        logger.info("开始处理工具ID: {} 的状态: {}", toolEntity.getId(), initialStatus);

        try {
            // 执行当前状态的处理逻辑
            // processor.process(toolEntity) 可能会修改 toolEntity 的非状态字段，
            // ToolStateService 负责状态的变更和持久化。
            processor.process(toolEntity);

            // 如果 processor.process() 未抛出异常，则继续判断下一个状态

            // 特殊处理 PUBLISHING 状态成功完成的情况
            if (initialStatus == ToolStatus.APPROVED) {
                // PublishingProcessor.process() 成功执行完（未抛异常）意味着发布成功
                toolEntity.setStatus(ToolStatus.APPROVED);
                toolRepository.updateById(toolEntity);
                logger.info("工具ID: {} 成功完成发布，状态更新为 APPROVED。", toolEntity.getId());
                return; // PUBLISHED 是此流程的最终成功状态
            }
            
            // 对于其他状态，获取理论上的下一个状态
            ToolStatus nextStatusCandidate = processor.getNextStatus();
            
            if (nextStatusCandidate != null && nextStatusCandidate != initialStatus) {
                toolEntity.setStatus(nextStatusCandidate);
                toolRepository.updateById(toolEntity); // 持久化状态变更和 processor 可能对 toolEntity 所做的其他修改
                logger.info("工具ID: {} 状态从 {} 更新为 {}。", toolEntity.getId(), initialStatus, nextStatusCandidate);

                processToolState(toolEntity); // 这将获取最新的实体进行下一次处理

            } else {
                logger.info("工具ID: {} 在状态 {} 处理完成，没有自动的下一状态或状态未改变。", toolEntity.getId(), initialStatus);
            }
        } catch (Exception e) {
            logger.error("处理工具ID: {} 的状态 {} 时发生错误: {}", toolEntity.getId(), initialStatus, e.getMessage(), e);
            
            // 此处 toolEntity 可能已被 processor 修改，或者保持 process 方法开始时的状态
            // 无论如何，我们需要将其标记为失败状态
            
            ToolStatus failureStatus = (initialStatus == ToolStatus.PUBLISHING) ? ToolStatus.PUBLISH_FAILED : ToolStatus.FAILED;
            toolEntity.setStatus(failureStatus);
            toolEntity.setFailedStepStatus(initialStatus); // 记录失败在哪一步
            toolEntity.setRejectReason("状态处理失败: " + e.getMessage()); // 考虑消息长度和敏感信息
            
            toolRepository.updateById(toolEntity); // 持久化失败状态和原因
            logger.info("工具ID: {} 状态已更新为 {}，失败步骤: {}，原因: {}", 
                        toolEntity.getId(), toolEntity.getStatus(), initialStatus, e.getMessage());
        }
    }

}