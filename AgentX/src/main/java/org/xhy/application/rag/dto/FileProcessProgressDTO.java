package org.xhy.application.rag.dto;

import org.xhy.domain.rag.constant.FileProcessingStatusEnum;

/** 文件处理进度响应
 * @author zang
 * @date 2025-01-10 */
public class FileProcessProgressDTO {

    /** 文件ID */
    private String fileId;

    /** 文件名 */
    private String filename;

    /** 统一处理状态枚举 */
    private FileProcessingStatusEnum processingStatusEnum;

    /** 处理状态编码 */
    private Integer processingStatus;

    /** 处理状态描述（中文） */
    private String processingStatusDescription;

    /** 当前OCR处理页数 */
    private Integer currentOcrPageNumber;

    /** 当前向量化处理页数 */
    private Integer currentEmbeddingPageNumber;

    /** 总页数 */
    private Integer filePageSize;

    /** OCR处理进度百分比 */
    private Double ocrProcessProgress;

    /** 向量化处理进度百分比 */
    private Double embeddingProcessProgress;

    /** 状态描述 */
    private String statusDescription;

    // 为了兼容旧版本，保留原有字段
    /** 初始化状态（数字） - 已弃用，使用processingStatus */
    @Deprecated
    private Integer isInitialize;

    /** 向量化状态（数字） - 已弃用，使用processingStatus */
    @Deprecated
    private Integer isEmbedding;

    /** 初始化状态（中文） - 已弃用，使用processingStatusDescription */
    @Deprecated
    private String initializeStatus;

    /** 向量化状态（中文） - 已弃用，使用processingStatusDescription */
    @Deprecated
    private String embeddingStatus;

    /** 当前处理页数（兼容字段，指向OCR页数） */
    private Integer currentPageNumber;

    /** 处理进度百分比（兼容字段，指向OCR进度） */
    private Double processProgress;

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

    public FileProcessingStatusEnum getProcessingStatusEnum() {
        return processingStatusEnum;
    }

    public void setProcessingStatusEnum(FileProcessingStatusEnum processingStatusEnum) {
        this.processingStatusEnum = processingStatusEnum;
    }

    public Integer getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(Integer processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getProcessingStatusDescription() {
        return processingStatusDescription;
    }

    public void setProcessingStatusDescription(String processingStatusDescription) {
        this.processingStatusDescription = processingStatusDescription;
    }

    public Integer getCurrentOcrPageNumber() {
        return currentOcrPageNumber;
    }

    public void setCurrentOcrPageNumber(Integer currentOcrPageNumber) {
        this.currentOcrPageNumber = currentOcrPageNumber;
    }

    public Integer getCurrentEmbeddingPageNumber() {
        return currentEmbeddingPageNumber;
    }

    public void setCurrentEmbeddingPageNumber(Integer currentEmbeddingPageNumber) {
        this.currentEmbeddingPageNumber = currentEmbeddingPageNumber;
    }

    public Integer getFilePageSize() {
        return filePageSize;
    }

    public void setFilePageSize(Integer filePageSize) {
        this.filePageSize = filePageSize;
    }

    public Double getOcrProcessProgress() {
        return ocrProcessProgress;
    }

    public void setOcrProcessProgress(Double ocrProcessProgress) {
        this.ocrProcessProgress = ocrProcessProgress;
    }

    public Double getEmbeddingProcessProgress() {
        return embeddingProcessProgress;
    }

    public void setEmbeddingProcessProgress(Double embeddingProcessProgress) {
        this.embeddingProcessProgress = embeddingProcessProgress;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    // 兼容性字段的getter和setter
    @Deprecated
    public Integer getIsInitialize() {
        return isInitialize;
    }

    @Deprecated
    public void setIsInitialize(Integer isInitialize) {
        this.isInitialize = isInitialize;
    }

    @Deprecated
    public Integer getIsEmbedding() {
        return isEmbedding;
    }

    @Deprecated
    public void setIsEmbedding(Integer isEmbedding) {
        this.isEmbedding = isEmbedding;
    }

    @Deprecated
    public String getInitializeStatus() {
        return initializeStatus;
    }

    @Deprecated
    public void setInitializeStatus(String initializeStatus) {
        this.initializeStatus = initializeStatus;
    }

    @Deprecated
    public String getEmbeddingStatus() {
        return embeddingStatus;
    }

    @Deprecated
    public void setEmbeddingStatus(String embeddingStatus) {
        this.embeddingStatus = embeddingStatus;
    }

    public Integer getCurrentPageNumber() {
        return currentPageNumber;
    }

    public void setCurrentPageNumber(Integer currentPageNumber) {
        this.currentPageNumber = currentPageNumber;
    }

    public Double getProcessProgress() {
        return processProgress;
    }

    public void setProcessProgress(Double processProgress) {
        this.processProgress = processProgress;
    }
}