package org.xhy.domain.rag.message;

import java.io.Serial;
import java.io.Serializable;

/** @author shilong.zang
 * @date 09:55 <br/>
 */
public class RagDocSyncOcrMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 5517731583403276913L;

    /** 文件id */
    private String fileId;

    /** 文件总页数 */
    private Integer pageSize;

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}
