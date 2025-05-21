package org.xhy.domain.tool.service.state.impl;

import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.state.ToolStateProcessor;

/** 人工审核状态处理器 */
public class ManualReviewProcessor implements ToolStateProcessor {

    @Override
    public ToolStatus getStatus() {
        return ToolStatus.MANUAL_REVIEW;
    }

    @Override
    public void process(ToolEntity tool) {
        // 人工审核状态只能等待人工审核，不自动处理
    }

    @Override
    public ToolStatus getNextStatus() {
        // 人工审核后的下一状态由人工操作决定，自动流程中不会进入下一步
        return null;
    }
}