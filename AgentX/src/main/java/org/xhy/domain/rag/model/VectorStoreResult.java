package org.xhy.domain.rag.model;

import org.xhy.domain.rag.constant.SearchType;

import java.io.Serializable;
import java.util.Map;

/** vector_store表查询结果统一数据结构 同时支持向量检索和关键词检索的结果表示
 * 
 * @author claude */
public class VectorStoreResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 向量存储ID */
    private String embeddingId;

    /** 文本内容 */
    private String text;

    /** 元数据（JSON格式） */
    private Map<String, Object> metadata;

    /** 检索分数（向量相似度分数或关键词rank分数） */
    private Double score;

    /** 检索类型标识 */
    private SearchType searchType;

    public VectorStoreResult() {
    }

    public VectorStoreResult(String embeddingId, String text, Map<String, Object> metadata, Double score,
            SearchType searchType) {
        this.embeddingId = embeddingId;
        this.text = text;
        this.metadata = metadata;
        this.score = score;
        this.searchType = searchType;
    }

    /** 从metadata中获取DOCUMENT_ID
     * @return 文档ID */
    public String getDocumentId() {
        if (metadata == null) {
            return null;
        }
        return (String) metadata.get("DOCUMENT_ID");
    }

    /** 从metadata中获取FILE_ID
     * @return 文件ID */
    public String getFileId() {
        if (metadata == null) {
            return null;
        }
        return (String) metadata.get("FILE_ID");
    }

    /** 从metadata中获取DATA_SET_ID
     * @return 数据集ID */
    public String getDataSetId() {
        if (metadata == null) {
            return null;
        }
        return (String) metadata.get("DATA_SET_ID");
    }

    /** 从metadata中获取FILE_NAME
     * @return 文件名 */
    public String getFileName() {
        if (metadata == null) {
            return null;
        }
        return (String) metadata.get("FILE_NAME");
    }

    public String getEmbeddingId() {
        return embeddingId;
    }

    public void setEmbeddingId(String embeddingId) {
        this.embeddingId = embeddingId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }
}