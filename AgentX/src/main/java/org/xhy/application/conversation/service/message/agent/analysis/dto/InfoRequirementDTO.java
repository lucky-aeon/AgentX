package org.xhy.application.conversation.service.message.agent.analysis.dto;

public class InfoRequirementDTO {

    /** 是否信息完整 true 表示信息完整，不需要额外输入； false 表示缺少信息，需要用户补充 */
    private boolean infoComplete;

    /** 如果 infoComplete 为 false， 则该字段是大模型给出的提示，告知用户需要补充哪些信息 */
    private String missingInfoPrompt;

    public boolean isInfoComplete() {
        return infoComplete;
    }

    public void setInfoComplete(boolean infoComplete) {
        this.infoComplete = infoComplete;
    }

    public String getMissingInfoPrompt() {
        return missingInfoPrompt;
    }

    public void setMissingInfoPrompt(String missingInfoPrompt) {
        this.missingInfoPrompt = missingInfoPrompt;
    }
}
