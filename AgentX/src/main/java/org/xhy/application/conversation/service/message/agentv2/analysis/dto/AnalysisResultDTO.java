package org.xhy.application.conversation.service.message.agentv2.analysis.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 消息分析结果
 * 包含是否是问答消息的标志和可能的直接回复内容
 */
public class AnalysisResultDTO {

    @JsonProperty("isQuestion")
    private boolean isQuestion;

    @JsonProperty("reply")
    private String reply;

    public AnalysisResultDTO() {
        // 默认构造函数，用于Jackson反序列化
    }

    public AnalysisResultDTO(boolean isQuestion, String reply) {
        this.isQuestion = isQuestion;
        this.reply = reply;
    }

    public boolean isQuestion() {
        return isQuestion;
    }

    public void setQuestion(boolean question) {
        isQuestion = question;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    @Override
    public String toString() {
        return "AnalysisResultDTO{" +
                "isQuestion=" + isQuestion +
                ", reply='" + reply + '\'' +
                '}';
    }
}
