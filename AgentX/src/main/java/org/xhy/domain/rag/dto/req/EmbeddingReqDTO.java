package org.xhy.domain.rag.dto.req;

/** @author shilong.zang
 * @date 14:41 <br/>
 */
public class EmbeddingReqDTO {

    /** 模型 */
    private String model;

    /** 语料 */
    private String input;

    private String encoding_format = "float";

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getEncoding_format() {
        return encoding_format;
    }

    public void setEncoding_format(String encoding_format) {
        this.encoding_format = encoding_format;
    }
}
