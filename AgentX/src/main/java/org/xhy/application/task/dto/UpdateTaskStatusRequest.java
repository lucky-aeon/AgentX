package org.xhy.application.task.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 更新任务状态请求
 */
public class UpdateTaskStatusRequest {
    
    /**
     * 任务状态
     */
    @NotBlank(message = "任务状态不能为空")
    private String status;
    
    /**
     * 进度
     */
    @Min(value = 0, message = "进度不能小于0")
    @Max(value = 100, message = "进度不能大于100")
    private Integer progress;
    
    public UpdateTaskStatusRequest() {
    }
    
    // Getters and Setters
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getProgress() {
        return progress;
    }
    
    public void setProgress(Integer progress) {
        this.progress = progress;
    }
} 