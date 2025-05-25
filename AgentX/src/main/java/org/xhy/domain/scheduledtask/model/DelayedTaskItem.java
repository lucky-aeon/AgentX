package org.xhy.domain.scheduledtask.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 延迟队列任务项
 * 用于延迟队列中的定时任务执行
 */
public class DelayedTaskItem implements Delayed {
    
    /** 任务ID */
    private final String taskId;
    
    /** 执行时间（毫秒时间戳） */
    private final long executeTime;
    
    /** 任务实体 */
    private final ScheduledTaskEntity task;
    
    public DelayedTaskItem(ScheduledTaskEntity task, LocalDateTime executeTime) {
        this.taskId = task.getId();
        this.task = task;
        this.executeTime = executeTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
        long delay = executeTime - System.currentTimeMillis();
        return unit.convert(delay, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public int compareTo(Delayed other) {
        if (this == other) {
            return 0;
        }
        
        if (other instanceof DelayedTaskItem) {
            DelayedTaskItem otherItem = (DelayedTaskItem) other;
            return Long.compare(this.executeTime, otherItem.executeTime);
        }
        
        return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public ScheduledTaskEntity getTask() {
        return task;
    }
    
    public long getExecuteTime() {
        return executeTime;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DelayedTaskItem that = (DelayedTaskItem) obj;
        return taskId.equals(that.taskId);
    }
    
    @Override
    public int hashCode() {
        return taskId.hashCode();
    }
    
    @Override
    public String toString() {
        return "DelayedTaskItem{" +
                "taskId='" + taskId + '\'' +
                ", executeTime=" + executeTime +
                '}';
    }
} 