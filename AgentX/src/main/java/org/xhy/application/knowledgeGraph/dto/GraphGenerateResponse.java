package org.xhy.application.knowledgeGraph.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 图谱生成响应DTO
 * 
 * @author shilong.zang */
@Schema(description = "图谱生成响应")
public class GraphGenerateResponse {

    @Schema(description = "任务ID", example = "task123")
    private String taskId;

    @Schema(description = "处理状态", example = "PROCESSING", allowableValues = {"PROCESSING", "COMPLETED", "FAILED"})
    private String status;

    @Schema(description = "处理消息", example = "图谱生成任务已提交")
    private String message;

    @Schema(description = "文档内容预览", example = "文档内容...")
    private String documentPreview;

    @Schema(description = "处理时间戳", example = "1640995200000")
    private Long timestamp;

    // Constructors
    public GraphGenerateResponse() {
    }

    public GraphGenerateResponse(String taskId, String status, String message) {
        this.taskId = taskId;
        this.status = status;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public GraphGenerateResponse(String taskId, String status, String message, String documentPreview) {
        this.taskId = taskId;
        this.status = status;
        this.message = message;
        this.documentPreview = documentPreview;
        this.timestamp = System.currentTimeMillis();
    }

    // Static factory methods
    public static GraphGenerateResponse processing(String taskId, String message, String documentPreview) {
        return new GraphGenerateResponse(taskId, "PROCESSING", message, documentPreview);
    }

    public static GraphGenerateResponse completed(String taskId, String message) {
        return new GraphGenerateResponse(taskId, "COMPLETED", message);
    }

    public static GraphGenerateResponse failed(String taskId, String message) {
        return new GraphGenerateResponse(taskId, "FAILED", message);
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDocumentPreview() {
        return documentPreview;
    }

    public void setDocumentPreview(String documentPreview) {
        this.documentPreview = documentPreview;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "GraphGenerateResponse{" + "taskId='" + taskId + '\'' + ", status='" + status + '\'' + ", message='"
                + message + '\'' + ", documentPreview='" + documentPreview + '\'' + ", timestamp=" + timestamp + '}';
    }
}
