package org.xhy.application.conversation.service.message.agentv2.analysis.dto;

public class AnalyzerMessageDTO {

    private boolean isQuestion;

    private String reply;

    public boolean getIsQuestion() {
        return isQuestion;
    }

    public void setIsQuestion(boolean question) {
        isQuestion = question;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}