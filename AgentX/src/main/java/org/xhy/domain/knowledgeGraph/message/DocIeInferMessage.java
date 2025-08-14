package org.xhy.domain.knowledgeGraph.message;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author shilong.zang
 * @date 13:59 <br/>
 */
public class DocIeInferMessage implements Serializable {

    /**
     * 文件id
     */
    private String fileId;
    private String fileName;
    private String documentText;

    public DocIeInferMessage() {
    }

    public DocIeInferMessage(String fileId, String fileName, String documentText) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.documentText = documentText;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DocIeInferMessage that = (DocIeInferMessage) o;
        return Objects.equals(fileId, that.fileId) && Objects.equals(fileName, that.fileName)
                && Objects.equals(documentText, that.documentText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, fileName, documentText);
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDocumentText() {
        return documentText;
    }

    public void setDocumentText(String documentText) {
        this.documentText = documentText;
    }
}
