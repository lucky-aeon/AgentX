package org.xhy.application.rag.dto;

import jakarta.validation.constraints.NotBlank;

/** 更新语料内容请求
 * 
 * @author shilong.zang */
public class UpdateDocumentUnitRequest {

    /** 语料ID */
    @NotBlank(message = "语料ID不能为空")
    private String documentUnitId;

    /** 更新后的内容 */
    @NotBlank(message = "内容不能为空")
    private String content;

    /** 是否重新向量化 */
    private Boolean reEmbedding = true;

    public String getDocumentUnitId() {
        return documentUnitId;
    }

    public void setDocumentUnitId(String documentUnitId) {
        this.documentUnitId = documentUnitId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getReEmbedding() {
        return reEmbedding;
    }

    public void setReEmbedding(Boolean reEmbedding) {
        this.reEmbedding = reEmbedding;
    }
}