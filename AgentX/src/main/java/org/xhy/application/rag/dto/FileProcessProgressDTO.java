package org.xhy.application.rag.dto;

/**
 * 文件处理进度响应
 * @author zang
 * @date 2025-01-10
 */
public class FileProcessProgressDTO {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 初始化状态
     */
    private Integer isInitialize;

    /**
     * 向量化状态
     */
    private Integer isEmbedding;

    /**
     * 当前处理页数
     */
    private Integer currentPageNumber;

    /**
     * 总页数
     */
    private Integer filePageSize;

    /**
     * 处理进度百分比
     */
    private Double processProgress;

    /**
     * 状态描述
     */
    private String statusDescription;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Integer getIsInitialize() {
        return isInitialize;
    }

    public void setIsInitialize(Integer isInitialize) {
        this.isInitialize = isInitialize;
    }

    public Integer getIsEmbedding() {
        return isEmbedding;
    }

    public void setIsEmbedding(Integer isEmbedding) {
        this.isEmbedding = isEmbedding;
    }

    public Integer getCurrentPageNumber() {
        return currentPageNumber;
    }

    public void setCurrentPageNumber(Integer currentPageNumber) {
        this.currentPageNumber = currentPageNumber;
    }

    public Integer getFilePageSize() {
        return filePageSize;
    }

    public void setFilePageSize(Integer filePageSize) {
        this.filePageSize = filePageSize;
    }

    public Double getProcessProgress() {
        return processProgress;
    }

    public void setProcessProgress(Double processProgress) {
        this.processProgress = processProgress;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }
}