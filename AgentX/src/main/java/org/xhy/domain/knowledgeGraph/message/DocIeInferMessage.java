package org.xhy.domain.knowledgeGraph.message;

import java.io.Serializable;
import java.util.Objects;

/**
 * 文档信息抽取推理消息
 * 支持分页处理以优化大文档的知识图谱提取
 * 
 * @author shilong.zang
 * @date 13:59 <br/>
 */
public class DocIeInferMessage implements Serializable {

    /**
     * 文件id
     */
    private String fileId;
    private String fileName;
    private String documentText;
    
    /**
     * 当前页码（从1开始）
     */
    private Integer pageNumber;
    
    /**
     * 总页数
     */
    private Integer totalPages;

    public DocIeInferMessage() {
    }

    public DocIeInferMessage(String fileId, String fileName, String documentText) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.documentText = documentText;
    }
    
    public DocIeInferMessage(String fileId, String fileName, String documentText, Integer pageNumber, Integer totalPages) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.documentText = documentText;
        this.pageNumber = pageNumber;
        this.totalPages = totalPages;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DocIeInferMessage that = (DocIeInferMessage) o;
        return Objects.equals(fileId, that.fileId) && Objects.equals(fileName, that.fileName)
                && Objects.equals(documentText, that.documentText)
                && Objects.equals(pageNumber, that.pageNumber) && Objects.equals(totalPages, that.totalPages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, fileName, documentText, pageNumber, totalPages);
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDocumentText() {
        return documentText;
    }

    public void setDocumentText(String documentText) {
        this.documentText = documentText;
    }
    
    public Integer getPageNumber() {
        return pageNumber;
    }
    
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }
    
    public Integer getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
    
    /**
     * 检查是否为分页消息
     */
    public boolean isPaged() {
        return pageNumber != null && totalPages != null;
    }
    
    /**
     * 检查是否为最后一页
     */
    public boolean isLastPage() {
        return isPaged() && pageNumber.equals(totalPages);
    }
}
