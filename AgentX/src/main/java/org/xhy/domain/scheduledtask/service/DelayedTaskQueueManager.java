package org.xhy.domain.scheduledtask.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.scheduledtask.model.DelayedTaskItem;
import org.xhy.domain.scheduledtask.model.ScheduledTaskEntity;

import java.time.LocalDateTime;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 延迟队列管理器
 * 负责管理延迟队列和任务调度
 */
@Service
public class DelayedTaskQueueManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DelayedTaskQueueManager.class);
    
    /** 延迟队列 */
    private final DelayQueue<DelayedTaskItem> delayQueue = new DelayQueue<>();
    
    /** 任务执行器 */
    private final ScheduleTaskExecutor taskExecutor;
    
    /** 线程池 */
    private ExecutorService executorService;
    
    /** 队列消费线程 */
    private Thread consumerThread;
    
    /** 是否运行中 */
    private volatile boolean running = false;
    
    public DelayedTaskQueueManager(ScheduleTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }
    
    /**
     * 初始化队列管理器
     */
    @PostConstruct
    public void init() {
        // 创建线程池用于执行任务
        this.executorService = Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r, "scheduled-task-executor-");
            t.setDaemon(true);
            return t;
        });
        
        // 启动队列消费线程
        startConsumer();
        
        logger.info("延迟队列管理器已启动，线程池大小: 5");
    }
    
    /**
     * 销毁队列管理器
     */
    @PreDestroy
    public void destroy() {
        running = false;
        
        // 中断消费线程
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
        
        // 关闭线程池
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("延迟队列管理器已关闭");
    }
    
    /**
     * 添加任务到延迟队列
     * @param task 定时任务实体
     * @param executeTime 执行时间
     */
    public void addTask(ScheduledTaskEntity task, LocalDateTime executeTime) {
        DelayedTaskItem item = new DelayedTaskItem(task, executeTime);
        delayQueue.offer(item);
        logger.info("任务已添加到延迟队列: taskId={}, executeTime={}", task.getId(), executeTime);
    }
    
    /**
     * 移除任务从延迟队列
     * @param taskId 任务ID
     */
    public void removeTask(String taskId) {
        boolean removed = delayQueue.removeIf(item -> taskId.equals(item.getTaskId()));
        if (removed) {
            logger.info("任务已从延迟队列移除: taskId={}", taskId);
        } else {
            logger.debug("任务不在延迟队列中: taskId={}", taskId);
        }
    }
    
    /**
     * 获取队列大小
     * @return 队列大小
     */
    public int getQueueSize() {
        return delayQueue.size();
    }
    
    /**
     * 启动队列消费线程
     */
    private void startConsumer() {
        running = true;
        consumerThread = new Thread(this::consumeQueue, "delayed-task-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
        logger.info("延迟队列消费线程已启动");
    }
    
    /**
     * 队列消费逻辑
     */
    private void consumeQueue() {
        logger.info("延迟队列消费线程开始运行");
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // 从延迟队列中取出到期的任务
                DelayedTaskItem item = delayQueue.take();
                
                if (item != null) {
                    logger.debug("从延迟队列取出到期任务: taskId={}", item.getTaskId());
                    
                    // 提交任务到线程池执行
                    executorService.submit(() -> {
                        try {
                            ScheduledTaskEntity task = item.getTask();
                            
                            // 检查任务是否可以执行
                            if (taskExecutor.canExecute(task)) {
                                taskExecutor.executeTask(task);
                            } else {
                                logger.info("任务不满足执行条件，跳过执行: taskId={}", task.getId());
                            }
                        } catch (Exception e) {
                            logger.error("执行任务异常: taskId={}, error={}", item.getTaskId(), e.getMessage(), e);
                        }
                    });
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("队列消费异常: {}", e.getMessage(), e);
                
                // 短暂休眠后继续
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        logger.info("延迟队列消费线程已停止");
    }
} 