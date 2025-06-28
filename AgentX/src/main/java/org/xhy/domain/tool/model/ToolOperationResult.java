package org.xhy.domain.tool.model;

/** 工具操作结果 用于Domain层向Application层传递操作结果和状态转换需求 */
public class ToolOperationResult {

    private final ToolEntity tool;
    private final boolean needStateTransition;

    private ToolOperationResult(ToolEntity tool, boolean needStateTransition) {
        this.tool = tool;
        this.needStateTransition = needStateTransition;
    }

    public static ToolOperationResult of(ToolEntity tool, boolean needStateTransition) {
        return new ToolOperationResult(tool, needStateTransition);
    }

    public static ToolOperationResult withoutTransition(ToolEntity tool) {
        return new ToolOperationResult(tool, false);
    }

    public static ToolOperationResult withTransition(ToolEntity tool) {
        return new ToolOperationResult(tool, true);
    }

    public ToolEntity getTool() {
        return tool;
    }

    public boolean needStateTransition() {
        return needStateTransition;
    }
}