package org.xhy.application.rag.dto;

/** RAG版本文件DTO
 * @author xhy
 * @date 2025-07-18 <br/>
 */
public class RagVersionFileDTO {

    /** 文件ID */
    private String id;

    /** 文件名 */
    private String fileName;

    /** 文件大小 */
    private Long fileSize;

    /** 文件类型 */
    private String fileType;

    /** 处理状态 */
    private Integer processStatus;

    /** 向量化状态 */
    private Integer embeddingStatus;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Integer getProcessStatus() {
        return processStatus;
    }

    public void setProcessStatus(Integer processStatus) {
        this.processStatus = processStatus;
    }

    public Integer getEmbeddingStatus() {
        return embeddingStatus;
    }

    public void setEmbeddingStatus(Integer embeddingStatus) {
        this.embeddingStatus = embeddingStatus;
    }
}