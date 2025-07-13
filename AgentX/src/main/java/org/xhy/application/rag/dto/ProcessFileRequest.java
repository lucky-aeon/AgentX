package org.xhy.application.rag.dto;

import jakarta.validation.constraints.NotBlank;

/** 文件预处理请求
 * @author zang
 * @date 2025-01-10 */
public class ProcessFileRequest {

    /** 文件ID */
    @NotBlank(message = "文件ID不能为空")
    private String fileId;

    /** 数据集ID */
    @NotBlank(message = "数据集ID不能为空")
    private String datasetId;

    /** 处理类型：1-初始化，2-向量化 */
    private Integer processType;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public Integer getProcessType() {
        return processType;
    }

    public void setProcessType(Integer processType) {
        this.processType = processType;
    }
}