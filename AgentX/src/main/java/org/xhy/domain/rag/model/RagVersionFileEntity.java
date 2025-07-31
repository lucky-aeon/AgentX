package org.xhy.domain.rag.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import org.xhy.infrastructure.entity.BaseEntity;

/** RAG版本文件实体（文件快照）
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@TableName("rag_version_files")
public class RagVersionFileEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -1L;

    /** 文件ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 关联的RAG版本ID */
    private String ragVersionId;

    /** 原始文件ID（仅标识） */
    private String originalFileId;

    /** 文件名 */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件页数 */
    private Integer filePageSize;

    /** 文件类型 */
    private String fileType;

    /** 文件存储路径 */
    private String filePath;

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

    public String getRagVersionId() {
        return ragVersionId;
    }

    public void setRagVersionId(String ragVersionId) {
        this.ragVersionId = ragVersionId;
    }

    public String getOriginalFileId() {
        return originalFileId;
    }

    public void setOriginalFileId(String originalFileId) {
        this.originalFileId = originalFileId;
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

    public Integer getFilePageSize() {
        return filePageSize;
    }

    public void setFilePageSize(Integer filePageSize) {
        this.filePageSize = filePageSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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