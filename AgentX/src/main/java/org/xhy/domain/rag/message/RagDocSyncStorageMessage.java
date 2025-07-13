package org.xhy.domain.rag.message;

import java.io.Serial;
import java.io.Serializable;

/** @author shilong.zang
 * @date 20:54 <br/>
 */
public class RagDocSyncStorageMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = -5764144581856293209L;
    /** 主键 */
    private String id;

    /** 文件ID */
    private String fileId;

    /** 页码 */
    private Integer page;

    /** 当前页内容 */
    private String content;

    /** 是否进行向量化 */
    private Boolean isVector;

    private String fileName;

    /** 数据集ID */
    private String datasetId;

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }
}
