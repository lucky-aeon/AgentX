package org.xhy.application.rag.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

/** 上传文件到数据集请求
 * @author shilong.zang
 * @date 2024-12-09 */
public class UploadFileRequest {

    /** 数据集ID */
    @NotBlank(message = "数据集ID不能为空")
    private String datasetId;

    /** 上传的文件 */
    private MultipartFile file;

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}