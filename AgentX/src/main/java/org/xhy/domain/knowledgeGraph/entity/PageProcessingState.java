package org.xhy.domain.knowledgeGraph.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 分页处理状态实体
 * 用于追踪文档分页知识图谱提取的处理状态
 * 
 * @author shilong.zang
 */
public class PageProcessingState {

    /**
     * 文档ID
     */
    private String documentId;
    
    /**
     * 总页数
     */
    private Integer totalPages;
    
    /**
     * 已处理完成的页码列表
     */
    private List<Integer> completedPages;
    
    /**
     * 处理失败的页码列表
     */
    private List<Integer> failedPages;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 处理状态
     */
    private ProcessingStatus status;

    public PageProcessingState() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = ProcessingStatus.PROCESSING;
    }

    public PageProcessingState(String documentId, Integer totalPages) {
        this();
        this.documentId = documentId;
        this.totalPages = totalPages;
    }

    /**
     * 检查是否所有页面都已处理完成
     */
    public boolean isAllPagesCompleted() {
        if (completedPages == null || totalPages == null) {
            return false;
        }
        return completedPages.size() == totalPages;
    }

    /**
     * 检查指定页面是否已处理完成
     */
    public boolean isPageCompleted(Integer pageNumber) {
        return completedPages != null && completedPages.contains(pageNumber);
    }

    /**
     * 检查指定页面是否处理失败
     */
    public boolean isPageFailed(Integer pageNumber) {
        return failedPages != null && failedPages.contains(pageNumber);
    }

    /**
     * 获取剩余未处理页面数
     */
    public int getRemainingPages() {
        if (totalPages == null) {
            return 0;
        }
        int completed = completedPages != null ? completedPages.size() : 0;
        return totalPages - completed;
    }

    /**
     * 标记页面处理完成
     */
    public void markPageCompleted(Integer pageNumber) {
        if (completedPages != null && !completedPages.contains(pageNumber)) {
            completedPages.add(pageNumber);
        }
        // 从失败列表中移除（如果存在）
        if (failedPages != null) {
            failedPages.remove(pageNumber);
        }
        this.updatedAt = LocalDateTime.now();
        
        // 检查是否所有页面都已完成
        if (isAllPagesCompleted()) {
            this.status = ProcessingStatus.COMPLETED;
        }
    }

    /**
     * 标记页面处理失败
     */
    public void markPageFailed(Integer pageNumber) {
        if (failedPages != null && !failedPages.contains(pageNumber)) {
            failedPages.add(pageNumber);
        }
        // 从完成列表中移除（如果存在）
        if (completedPages != null) {
            completedPages.remove(pageNumber);
        }
        this.updatedAt = LocalDateTime.now();
        this.status = ProcessingStatus.PARTIAL_FAILED;
    }

    /**
     * 更新处理状态
     */
    public void updateStatus() {
        this.updatedAt = LocalDateTime.now();
        
        if (isAllPagesCompleted()) {
            this.status = ProcessingStatus.COMPLETED;
        } else if (failedPages != null && !failedPages.isEmpty()) {
            this.status = ProcessingStatus.PARTIAL_FAILED;
        } else {
            this.status = ProcessingStatus.PROCESSING;
        }
    }

    // Getters and Setters
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public List<Integer> getCompletedPages() {
        return completedPages;
    }

    public void setCompletedPages(List<Integer> completedPages) {
        this.completedPages = completedPages;
    }

    public List<Integer> getFailedPages() {
        return failedPages;
    }

    public void setFailedPages(List<Integer> failedPages) {
        this.failedPages = failedPages;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessingStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageProcessingState that = (PageProcessingState) o;
        return Objects.equals(documentId, that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId);
    }

    @Override
    public String toString() {
        return "PageProcessingState{" +
                "documentId='" + documentId + '\'' +
                ", totalPages=" + totalPages +
                ", completedPages=" + (completedPages != null ? completedPages.size() : 0) +
                ", failedPages=" + (failedPages != null ? failedPages.size() : 0) +
                ", status=" + status +
                ", updatedAt=" + updatedAt +
                '}';
    }

    /**
     * 处理状态枚举
     */
    public enum ProcessingStatus {
        PROCESSING("处理中"),
        COMPLETED("已完成"),
        PARTIAL_FAILED("部分失败"),
        FAILED("全部失败");

        private final String description;

        ProcessingStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
