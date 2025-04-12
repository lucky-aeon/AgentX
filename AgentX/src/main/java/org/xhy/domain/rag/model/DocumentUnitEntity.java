package org.xhy.domain.rag.model;

import java.io.Serial;
import java.io.Serializable;

import org.xhy.infrastructure.entity.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @author shilong.zang
 * @date 20:24 <br/>
 */
@TableName("document_unit")
public class DocumentUnitEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 7001509997040094844L;

    /**
     * 主键
     */
    private String id;

    /**
     * 文档ID
     */
    private String fileId;

    /**
     * 页码
     */
    private Integer page;

    /**
     * 当前页内容
     */
    private String content;

    /**
     * 是否进行向量化
     */
    private Boolean isVector;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getVector() {
        return isVector;
    }

    public void setVector(Boolean vector) {
        isVector = vector;
    }
}
