package org.xhy.domain.rag.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import org.xhy.infrastructure.converter.PgVectorTypeHandler;
import org.xhy.infrastructure.entity.BaseEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @author shilong.zang
 * @date 15:40 <br/>
 */
@TableName("document_chunks")
public class DocumentChunkEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 180377127282950531L;

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 文档ID
     */
    private String documentId;

    /**
     * 块内容
     */
    private String content;

    /**
     * 块序号
     */
    private Integer chunkIndex;

    /**
     * 向量嵌入
     * 使用自定义TypeHandler处理PostgreSQL vector类型
     */
    @TableField(typeHandler = PgVectorTypeHandler.class)
    private float[] embedding;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DocumentChunkEntity that = (DocumentChunkEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(documentId, that.documentId) && Objects.equals(content, that.content) && Objects.equals(chunkIndex, that.chunkIndex) && Objects.deepEquals(embedding, that.embedding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, documentId, content, chunkIndex, Arrays.hashCode(embedding));
    }
}
