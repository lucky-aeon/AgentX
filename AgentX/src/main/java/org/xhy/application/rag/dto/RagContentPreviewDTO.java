package org.xhy.application.rag.dto;

import java.util.List;

/** RAG内容预览DTO
 * @author xhy
 * @date 2025-07-18 <br/>
 */
public class RagContentPreviewDTO {

    /** 版本ID */
    private String id;

    /** 名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 版本号 */
    private String version;

    /** 文件列表 */
    private List<RagVersionFileDTO> files;

    /** 示例文档内容（前几条） */
    private List<RagVersionDocumentDTO> sampleDocuments;

    /** 总文档数量 */
    private Integer totalDocuments;

    /** 总文件大小 */
    private Long totalSize;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<RagVersionFileDTO> getFiles() {
        return files;
    }

    public void setFiles(List<RagVersionFileDTO> files) {
        this.files = files;
    }

    public List<RagVersionDocumentDTO> getSampleDocuments() {
        return sampleDocuments;
    }

    public void setSampleDocuments(List<RagVersionDocumentDTO> sampleDocuments) {
        this.sampleDocuments = sampleDocuments;
    }

    public Integer getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(Integer totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }
}