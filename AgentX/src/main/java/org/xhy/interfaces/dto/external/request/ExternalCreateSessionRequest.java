package org.xhy.interfaces.dto.external.request;

/** 外部API创建会话请求DTO */
public class ExternalCreateSessionRequest {

    /** 会话标题（可选，默认"新会话"） */
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}