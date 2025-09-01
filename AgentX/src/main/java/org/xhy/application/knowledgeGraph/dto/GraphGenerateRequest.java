package org.xhy.application.knowledgeGraph.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/** 图谱生成请求DTO
 * 
 * @author shilong.zang */
@Schema(description = "图谱生成请求")
public class GraphGenerateRequest {

    @NotBlank(message = "文件ID不能为空")
    @Schema(description = "文件ID", example = "file123", required = true)
    private String fileId;

    @Schema(description = "是否异步处理", example = "true")
    private Boolean async = true;

    // Constructors
    public GraphGenerateRequest() {
    }

    public GraphGenerateRequest(String fileId) {
        this.fileId = fileId;
    }

    public GraphGenerateRequest(String fileId, Boolean async) {
        this.fileId = fileId;
        this.async = async;
    }

    // Getters and Setters
    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    @Override
    public String toString() {
        return "GraphGenerateRequest{" + "fileId='" + fileId + '\'' + ", async=" + async + '}';
    }
}
