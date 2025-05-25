package org.xhy.application.scheduledtask.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.scheduledtask.dto.ScheduledTaskDTO;
import org.xhy.domain.scheduledtask.constant.RepeatType;
import org.xhy.domain.scheduledtask.model.RepeatConfig;
import org.xhy.domain.scheduledtask.service.DelayedTaskQueueManager;
import org.xhy.interfaces.dto.scheduledtask.request.CreateScheduledTaskRequest;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 延迟队列执行测试
 * 测试10秒后任务是否能执行成功
 */
@SpringBootTest
@Transactional
@Rollback(value = false)
public class DelayedTaskExecutionTest {

    @Resource
    private ScheduledTaskAppService scheduledTaskAppService;
    
    @Resource
    private DelayedTaskQueueManager queueManager;

    // 测试数据
    private static final String TEST_USER_ID = "1fec531705a7bda022cb2cf3650d0d17";
    private static final String TEST_AGENT_ID = "0ee94c8945453f1f66bbe9d38e52d69f";
    private static final String TEST_SESSION_ID = "3cdefe61af23fb0bb071c5f9655c8b68";

    @Test
    void testTaskExecutionAfter10Seconds() throws InterruptedException {
        System.out.println("=== 测试10秒后任务执行 ===");
        
        // 创建一个10秒后执行的一次性任务
        CreateScheduledTaskRequest request = new CreateScheduledTaskRequest();
        request.setContent("你好,你是谁");
        request.setAgentId(TEST_AGENT_ID);
        request.setSessionId(TEST_SESSION_ID);
        request.setRepeatType(RepeatType.NONE);
        
        RepeatConfig config = new RepeatConfig();
        LocalDateTime executeTime = LocalDateTime.now().plusSeconds(10);
        config.setExecuteDateTime(executeTime);
        request.setRepeatConfig(config);
        
        // 创建任务
        ScheduledTaskDTO task = scheduledTaskAppService.createScheduledTask(request, TEST_USER_ID);
        assertNotNull(task);
        
        System.out.println("✅ 任务已创建: " + task.getId());
        System.out.println("📅 计划执行时间: " + executeTime);
        System.out.println("🕐 当前时间: " + LocalDateTime.now());
        System.out.println("📊 队列大小: " + queueManager.getQueueSize());
        
        // 等待任务执行（等待12秒确保任务执行）
        System.out.println("⏳ 等待10秒后任务执行...");
        Thread.sleep(12000);
        
        System.out.println("⏰ 等待完成时间: " + LocalDateTime.now());
        System.out.println("📊 执行后队列大小: " + queueManager.getQueueSize());
        
        // 验证任务状态
        ScheduledTaskDTO updatedTask = scheduledTaskAppService.getTask(task.getId(), TEST_USER_ID);
        System.out.println("📋 任务最终状态: " + updatedTask.getStatus());
        System.out.println("⏱️ 最后执行时间: " + updatedTask.getLastExecuteTime());
        
        // 验证任务确实被执行了
        assertNotNull(updatedTask.getLastExecuteTime(), "任务应该已经执行，应该有最后执行时间");
        
        System.out.println("✅ 10秒延迟任务执行测试完成 - 任务执行成功！");
    }
} 