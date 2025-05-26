package org.xhy.domain.scheduledtask.service;

import org.junit.jupiter.api.Test;
import org.xhy.domain.scheduledtask.constant.RepeatType;
import org.xhy.domain.scheduledtask.constant.ScheduleTaskStatus;
import org.xhy.domain.scheduledtask.model.DelayedTaskItem;
import org.xhy.domain.scheduledtask.model.RepeatConfig;
import org.xhy.domain.scheduledtask.model.ScheduledTaskEntity;

import java.time.LocalDateTime;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/** 延迟队列测试 */
public class DelayedTaskQueueTest {

    @Test
    public void testDelayedTaskItem() throws InterruptedException {
        // 创建测试任务
        ScheduledTaskEntity task = new ScheduledTaskEntity();
        task.setId("test-task-1");
        task.setUserId("user-1");
        task.setAgentId("agent-1");
        task.setSessionId("session-1");
        task.setContent("测试任务内容");
        task.setRepeatType(RepeatType.NONE);
        task.setRepeatConfig(new RepeatConfig());
        task.setStatus(ScheduleTaskStatus.ACTIVE);

        // 设置3秒后执行
        LocalDateTime executeTime = LocalDateTime.now().plusSeconds(3);

        // 创建延迟队列项
        DelayedTaskItem item = new DelayedTaskItem(task, executeTime);

        // 创建延迟队列
        DelayQueue<DelayedTaskItem> queue = new DelayQueue<>();
        queue.offer(item);

        System.out.println("任务已添加到队列，等待执行...");
        System.out.println("当前时间: " + LocalDateTime.now());
        System.out.println("执行时间: " + executeTime);

        // 从队列中取出任务（会阻塞直到任务到期）
        DelayedTaskItem takenItem = queue.take();

        System.out.println("任务已从队列中取出");
        System.out.println("实际执行时间: " + LocalDateTime.now());
        System.out.println("任务ID: " + takenItem.getTaskId());
        System.out.println("任务内容: " + takenItem.getTask().getContent());

        // 验证延迟时间
        long delay = takenItem.getDelay(TimeUnit.MILLISECONDS);
        System.out.println("剩余延迟时间: " + delay + "ms");

        // 延迟应该接近0（因为任务已经到期）
        assert delay <= 100; // 允许100ms的误差
    }

    @Test
    public void testMultipleTasksOrdering() throws InterruptedException {
        DelayQueue<DelayedTaskItem> queue = new DelayQueue<>();
        LocalDateTime now = LocalDateTime.now();

        // 创建多个任务，执行时间不同
        for (int i = 1; i <= 3; i++) {
            ScheduledTaskEntity task = new ScheduledTaskEntity();
            task.setId("task-" + i);
            task.setContent("任务 " + i);
            task.setStatus(ScheduleTaskStatus.ACTIVE);

            // 任务1: 3秒后执行，任务2: 1秒后执行，任务3: 2秒后执行
            LocalDateTime executeTime = now.plusSeconds(4 - i);
            DelayedTaskItem item = new DelayedTaskItem(task, executeTime);
            queue.offer(item);

            System.out.println("添加任务: " + task.getId() + ", 执行时间: " + executeTime);
        }

        System.out.println("开始按顺序取出任务...");

        // 按执行时间顺序取出任务
        for (int i = 0; i < 3; i++) {
            DelayedTaskItem item = queue.take();
            System.out.println("取出任务: " + item.getTaskId() + ", 时间: " + LocalDateTime.now());
        }

        System.out.println("所有任务已执行完成");
    }
}