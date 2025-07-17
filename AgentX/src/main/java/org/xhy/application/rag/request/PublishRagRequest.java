package org.xhy.application.rag.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/** 发布RAG请求
 * @author xhy
 * @date 2025-07-16 <br/>
 */
public class PublishRagRequest {

    /** 原始RAG数据集ID */
    @NotBlank(message = "RAG数据集ID不能为空")
    private String ragId;

    /** 版本号 */
    @NotBlank(message = "版本号不能为空")
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "版本号格式错误，应为x.x.x格式")
    private String version;

    /** 更新日志 */
    private String changeLog;

    /** 标签列表 */
    private List<String> labels;

    public String getRagId() {
        return ragId;
    }

    public void setRagId(String ragId) {
        this.ragId = ragId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }
}