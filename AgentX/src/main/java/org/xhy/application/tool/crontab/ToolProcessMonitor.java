package org.xhy.application.tool.crontab;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.ToolDomainService;
import org.xhy.domain.tool.service.ToolStateService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ToolProcessMonitor {
    private final ScheduledExecutorService scheduler;
    private static final Map<String, ToolExecutionInfo> runningTools = new ConcurrentHashMap<>();
    private final ToolDomainService toolDomainService;
    private final ToolStateService toolStateService;

    public ToolProcessMonitor(ToolDomainService toolDomainService, ToolStateService toolStateService) {
        this.toolDomainService = toolDomainService;
        this.toolStateService = toolStateService;
        this.scheduler = Executors.newScheduledThreadPool(5);
    }

    @PostConstruct
    public void startMonitoring() {
        scheduler.scheduleAtFixedRate(this::resumeToolStateService, 0, 5, TimeUnit.MINUTES);
    }

    public static void recordToolState(String toolId, ToolStatus currentStatus) {
        runningTools.put(toolId, new ToolExecutionInfo(toolId, currentStatus, System.currentTimeMillis()));
        log.info("工具状态流转中: {} 状态: {}", toolId, currentStatus);
    }

    public static void recordToolStateTermination(ToolEntity toolEntity) {
        // 避免重复日志
        if (runningTools.containsKey(toolEntity.getId())) {
            ToolExecutionInfo remove = runningTools.remove(toolEntity.getId());
            log.info("工具流转结束: {} 状态: {}", remove.getToolId(), toolEntity.getStatus());
        }
    }

    private void resumeToolStateService() {
        // 查询数据库中所有正在处理的工具，然后去掉runningTools里的，就是需要恢复状态处理的工具
        List<ToolEntity> processingTools = toolDomainService.findProcessingTools();
        if (CollectionUtils.isEmpty(processingTools)) {
            return;
        }
        processingTools.removeIf(tool -> runningTools.containsKey(tool.getId()));
        processingTools.forEach(toolStateService::submitToolForProcessing);
    }

    private static class ToolExecutionInfo {
        private final String toolId;
        private final ToolStatus currentStatus;
        private final long startTime;

        public ToolExecutionInfo(String toolId, ToolStatus currentStatus, long startTime) {
            this.toolId = toolId;
            this.currentStatus = currentStatus;
            this.startTime = startTime;
        }

        public String getToolId() {
            return toolId;
        }

        public ToolStatus getCurrentStatus() {
            return currentStatus;
        }

        public long getStartTime() {
            return startTime;
        }
    }
}