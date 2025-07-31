package org.xhy.application.conversation.service.message.agent.event;

import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowContext;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowState;

/** Agent工作流事件 用于在工作流状态转换时传递事件信息 */
public class AgentWorkflowEvent {
    private final AgentWorkflowContext<?> context;
    private final AgentWorkflowState fromState;
    private final AgentWorkflowState toState;

    public AgentWorkflowEvent(AgentWorkflowContext<?> context, AgentWorkflowState fromState,
            AgentWorkflowState toState) {
        this.context = context;
        this.fromState = fromState;
        this.toState = toState;
    }

    public AgentWorkflowContext<?> getContext() {
        return context;
    }

    public AgentWorkflowState getFromState() {
        return fromState;
    }

    public AgentWorkflowState getToState() {
        return toState;
    }
}