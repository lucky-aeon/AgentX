package org.xhy.interfaces.dto.rag;

import java.io.Serial;
import java.io.Serializable;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

/** @author shilong.zang
 * @date 14:16 <br/>
 */
public class RagUploadRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 278593981747357964L;

    @NotNull(message = "文件不能为空")
    private MultipartFile file;

    /** 数据集id */
    @NotNull(message = "数据集id不能为空")
    private String dataSetId;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }
}
