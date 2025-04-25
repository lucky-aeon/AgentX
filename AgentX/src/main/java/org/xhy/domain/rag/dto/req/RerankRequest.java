package org.xhy.domain.rag.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author shilong.zang
 * @date 15:15 <br/>
 */
public class RerankRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -1272322443949808505L;


    private String model;

    private String query;

    private List<String> documents;

    @JsonProperty("return_documents")
    private boolean returnDocuments = false;

    @JsonProperty("max_chunks_per_doc")
    private Integer maxChucksPerDoc = 10;

    @JsonProperty("overlap_tokens")
    private Integer overlapTokens = 80;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getDocuments() {
        return documents;
    }

    public void setDocuments(List<String> documents) {
        this.documents = documents;
    }

    public boolean isReturnDocuments() {
        return returnDocuments;
    }

    public void setReturnDocuments(boolean returnDocuments) {
        this.returnDocuments = returnDocuments;
    }

    public Integer getMaxChucksPerDoc() {
        return maxChucksPerDoc;
    }

    public void setMaxChucksPerDoc(Integer maxChucksPerDoc) {
        this.maxChucksPerDoc = maxChucksPerDoc;
    }

    public Integer getOverlapTokens() {
        return overlapTokens;
    }

    public void setOverlapTokens(Integer overlapTokens) {
        this.overlapTokens = overlapTokens;
    }


}
