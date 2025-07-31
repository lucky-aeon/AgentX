package org.xhy.domain.rag.dto.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** @author shilong.zang
 * @date 15:16 <br/>
 */
public class RerankResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = -4763176490538778562L;

    /** 唯一标识符 */
    private String id;

    /** 搜索结果列表 */
    private List<SearchResult> results;

    /** 令牌统计信息 */
    private Meta meta;

    /** 搜索结果项 */
    public static class SearchResult implements Serializable {

        @Serial
        private static final long serialVersionUID = -2428070945016880585L;
        /** 文档内容 */
        private Document document;

        /** 索引位置 */
        private Integer index;

        /** 相关性分数 */
        @JsonProperty("relevance_score")
        private Double relevanceScore;

        public Document getDocument() {
            return document;
        }

        public void setDocument(Document document) {
            this.document = document;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public Double getRelevanceScore() {
            return relevanceScore;
        }

        public void setRelevanceScore(Double relevanceScore) {
            this.relevanceScore = relevanceScore;
        }
    }

    /** 文档内容 */
    public static class Document implements Serializable {

        @Serial
        private static final long serialVersionUID = -6132815214174496256L;
        /** 文本内容 */
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    /** 令牌统计信息 */
    public static class Meta implements Serializable {

        @Serial
        private static final long serialVersionUID = 5986231625205198272L;

        private TokenInfo tokens;

        @JsonProperty("billed_units")
        private BilledUnits billedUnits;

        public TokenInfo getTokens() {
            return tokens;
        }

        public void setTokens(TokenInfo tokens) {
            this.tokens = tokens;
        }

        public BilledUnits getBilledUnits() {
            return billedUnits;
        }

        public void setBilledUnits(BilledUnits billedUnits) {
            this.billedUnits = billedUnits;
        }
    }

    public static class BilledUnits implements Serializable {

        @Serial
        private static final long serialVersionUID = 5723230611565604949L;
        @JsonProperty("input_tokens")
        private Integer inputTokens;
        @JsonProperty("output_tokens")
        private Integer outputTokens;
        @JsonProperty("output_units")
        private Integer outputUnits;

        private Integer classifications;

        public Integer getInputTokens() {
            return inputTokens;
        }

        public void setInputTokens(Integer inputTokens) {
            this.inputTokens = inputTokens;
        }

        public Integer getOutputTokens() {
            return outputTokens;
        }

        public void setOutputTokens(Integer outputTokens) {
            this.outputTokens = outputTokens;
        }

        public Integer getOutputUnits() {
            return outputUnits;
        }

        public void setOutputUnits(Integer outputUnits) {
            this.outputUnits = outputUnits;
        }

        public Integer getClassifications() {
            return classifications;
        }

        public void setClassifications(Integer classifications) {
            this.classifications = classifications;
        }
    }

    public static class TokenInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = -1766061142376465518L;
        /** 输入令牌数 */
        @JsonProperty("input_tokens")
        private Integer inputTokens;

        /** 输出令牌数 */
        @JsonProperty("output_tokens")
        private Integer outputTokens;

        public Integer getInputTokens() {
            return inputTokens;
        }

        public void setInputTokens(Integer inputTokens) {
            this.inputTokens = inputTokens;
        }

        public Integer getOutputTokens() {
            return outputTokens;
        }

        public void setOutputTokens(Integer outputTokens) {
            this.outputTokens = outputTokens;
        }

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<SearchResult> getResults() {
        return results;
    }

    public void setResults(List<SearchResult> results) {
        this.results = results;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }
}
