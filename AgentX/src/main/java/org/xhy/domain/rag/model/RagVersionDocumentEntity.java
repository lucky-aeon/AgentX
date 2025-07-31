package org.xhy.domain.rag.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import org.xhy.infrastructure.entity.BaseEntity;

/** RAG版本文档单元实体（文档内容快照）
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@TableName("rag_version_documents")
public class RagVersionDocumentEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -1L;

    /** 文档单元ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 关联的RAG版本ID */
    private String ragVersionId;

    /** 关联的版本文件ID */
    private String ragVersionFileId;

    /** 原始文档单元ID（仅标识） */
    private String originalDocumentId;

    /** 文档内容 */
    private String content;

    /** 页码 */
    private Integer page;

    /** 向量ID（在向量数据库中的ID） */
    private String vectorId;

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

    public String getRagVersionFileId() {
        return ragVersionFileId;
    }

    public void setRagVersionFileId(String ragVersionFileId) {
        this.ragVersionFileId = ragVersionFileId;
    }

    public String getOriginalDocumentId() {
        return originalDocumentId;
    }

    public void setOriginalDocumentId(String originalDocumentId) {
        this.originalDocumentId = originalDocumentId;
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

    public String getVectorId() {
        return vectorId;
    }

    public void setVectorId(String vectorId) {
        this.vectorId = vectorId;
    }
}