package org.xhy.interfaces.dto.external.request;

import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/** 外部API聊天请求DTO */
public class ExternalChatRequest {

    /** 模型ID（可选，不传使用Agent绑定的模型） */
    private String model;

    /** 消息内容 */
    @NotBlank(message = "消息内容不可为空")
    private String message;

    /** 是否流式返回（可选，默认false） */
    private Boolean stream = false;

    /** 会话ID（可选，不传创建新会话） */
    private String sessionId;

    /** 文件列表（可选） */
    private List<String> files = new ArrayList<>();

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files != null ? files : new ArrayList<>();
    }
}