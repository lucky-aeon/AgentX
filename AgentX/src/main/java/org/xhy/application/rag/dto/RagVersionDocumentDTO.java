package org.xhy.application.rag.dto;

import java.time.LocalDateTime;

/** RAG版本文档DTO
 * @author xhy
 * @date 2025-07-18 <br/>
 */
public class RagVersionDocumentDTO {

    /** 文档ID */
    private String id;

    /** 文档内容 */
    private String content;

    /** 页码 */
    private Integer page;

    /** 关联的文件名 */
    private String fileName;

    /** 创建时间 */
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}