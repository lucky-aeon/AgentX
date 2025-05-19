package org.xhy.domain.tool.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.repository.ToolRepository;
import org.xhy.domain.tool.service.state.ToolStateProcessor;
import org.xhy.domain.tool.service.state.impl.DeployingProcessor;
import org.xhy.domain.tool.service.state.impl.FetchingToolsProcessor;
import org.xhy.domain.tool.service.state.impl.GithubUrlValidateProcessor;
import org.xhy.domain.tool.service.state.impl.ManualReviewProcessor;
import org.xhy.domain.tool.service.state.impl.WaitingReviewProcessor;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 工具状态流转服务
 */
@Service
public class ToolStateService {
    
    private static final Logger log = Logger.getLogger(ToolStateService.class.getName());
    
    private final ToolRepository toolRepository;
    private final Map<ToolStatus, ToolStateProcessor> processorMap = new HashMap<>();
    private final ExecutorService executorService;
    
    public ToolStateService(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
        
        // 创建无限队列的线程池
        this.executorService = new ThreadPoolExecutor(
                5, // 核心线程数
                10, // 最大线程数
                60L, // 空闲线程存活时间
                TimeUnit.SECONDS, // 时间单位
                new LinkedBlockingQueue<>(), // 无限队列
                r -> {
                    Thread t = new Thread(r, "tool-state-thread");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
    }
    
    /**
     * 初始化状态处理器
     */
    @PostConstruct
    public void init() {
        // 注册状态处理器
        registerProcessor(new WaitingReviewProcessor());
        registerProcessor(new GithubUrlValidateProcessor());
        registerProcessor(new DeployingProcessor());
        registerProcessor(new FetchingToolsProcessor());
        registerProcessor(new ManualReviewProcessor());
        
        log.info("工具状态处理器初始化完成");
    }
    
    /**
     * 注册状态处理器
     */
    private void registerProcessor(ToolStateProcessor processor) {
        processorMap.put(processor.getStatus(), processor);
    }
    
    /**
     * 提交工具进行状态处理
     */
    public void submitTool(String toolId) {
        ToolEntity tool = toolRepository.selectById(toolId);
        if (tool == null) {
            throw new BusinessException("工具不存在: " + toolId);
        }
        
        executorService.submit(() -> processToolState(tool));
    }
    
    /**
     * 处理工具状态
     */
    private void processToolState(ToolEntity tool) {
        ToolStatus currentStatus = tool.getStatus();
        if (currentStatus == ToolStatus.APPROVED){
            return;
        }
        ToolStateProcessor processor = processorMap.get(currentStatus);
        
        if (processor == null) {
            log.warning("找不到状态处理器: " + currentStatus);
            return;
        }
        
        log.info("开始处理工具状态: " + tool.getId() + ", 状态: " + currentStatus);
        
        try {
            // 处理当前状态
            processor.process(tool);
            
            // 获取下一个状态
            ToolStatus nextStatus = processor.getNextStatus();
            if (nextStatus != null && !nextStatus.equals(currentStatus)) {
                tool.setStatus(nextStatus);
                toolRepository.updateById(tool);
                
                log.info("工具状态已更新: " + tool.getId() + ", 新状态: " + nextStatus);
                
                // 如果有下一个状态且不是人工审核，继续处理
                if (nextStatus != ToolStatus.MANUAL_REVIEW && nextStatus != ToolStatus.APPROVED) {
                    // 递归处理下一个状态
                    processToolState(toolRepository.selectById(tool.getId()));
                }
            }
        } catch (Exception e) {
            log.severe("处理工具状态失败: " + tool.getId() + ", 状态: " + currentStatus + ", 错误: " + e.getMessage());
            
            // 设置失败状态
            tool.setStatus(ToolStatus.FAILED);
            tool.setFailedStepStatus(currentStatus);
            tool.setRejectReason(e.getMessage());
            toolRepository.updateById(tool);
        }
    }
    
    /**
     * 手动审核通过
     */
    public void approveManualReview(String toolId) {
        ToolEntity tool = toolRepository.selectById(toolId);
        if (tool == null) {
            throw new BusinessException("工具不存在: " + toolId);
        }
        
        if (tool.getStatus() != ToolStatus.MANUAL_REVIEW) {
            throw new BusinessException("工具当前状态不是人工审核: " + tool.getStatus());
        }
        
        tool.setStatus(ToolStatus.APPROVED);
        toolRepository.updateById(tool);
        
        log.info("工具已审核通过: " + toolId);
    }
    
    /**
     * 重新开始状态流转
     */
    public void restartProcess(String toolId) {
        ToolEntity tool = toolRepository.selectById(toolId);
        if (tool == null) {
            throw new BusinessException("工具不存在: " + toolId);
        }
        
        if (tool.getStatus() != ToolStatus.FAILED) {
            throw new BusinessException("只有失败状态的工具才能重新开始: " + tool.getStatus());
        }
        
        // 重置为等待审核状态
        tool.setStatus(ToolStatus.WAITING_REVIEW);
        tool.setFailedStepStatus(null);
        tool.setRejectReason(null);
        toolRepository.updateById(tool);
        
        // 提交到处理队列
        submitTool(toolId);
        
        log.info("工具已重新开始状态流转: " + toolId);
    }
} 