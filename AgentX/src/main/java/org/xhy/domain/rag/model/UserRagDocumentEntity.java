package org.xhy.domain.rag.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import org.xhy.infrastructure.entity.BaseEntity;

/** 用户RAG文档快照实体
 * @author xhy
 * @date 2025-07-22 <br/>
 */
@TableName("user_rag_documents")
public class UserRagDocumentEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -1L;

    /** 文档快照ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 关联的用户RAG ID */
    private String userRagId;

    /** 关联的用户RAG文件ID */
    private String userRagFileId;

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

    public String getUserRagId() {
        return userRagId;
    }

    public void setUserRagId(String userRagId) {
        this.userRagId = userRagId;
    }

    public String getUserRagFileId() {
        return userRagFileId;
    }

    public void setUserRagFileId(String userRagFileId) {
        this.userRagFileId = userRagFileId;
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