package org.xhy.domain.rag.dto.resp;

import java.util.ArrayList;

/**
 * @author shilong.zang
 * @date 14:44 <br/>
 */
public class EmbeddingRespDTO {

    private String object;

    private ArrayList<EmbeddingDataBO> data;

    private String model;

    public static class EmbeddingDataBO {

        private String object;

        private double[] embedding;

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }

        public double[] getEmbedding() {
            return embedding;
        }

        public void setEmbedding(double[] embedding) {
            this.embedding = embedding;
        }
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public ArrayList<EmbeddingDataBO> getData() {
        return data;
    }

    public void setData(ArrayList<EmbeddingDataBO> data) {
        this.data = data;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
