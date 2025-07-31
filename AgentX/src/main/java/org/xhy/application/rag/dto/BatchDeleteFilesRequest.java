package org.xhy.application.rag.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** 批量删除文件请求
 * @author zang
 * @date 2025-01-16 */
public class BatchDeleteFilesRequest {

    /** 文件URL列表 */
    @NotEmpty(message = "文件URL列表不能为空")
    private List<String> fileUrls;

    public List<String> getFileUrls() {
        return fileUrls;
    }

    public void setFileUrls(List<String> fileUrls) {
        this.fileUrls = fileUrls;
    }
}