package org.xhy.application.tool.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.application.tool.service.state.AppToolStateProcessor;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;

/** 人工审核状态处理器
 * 
 * 职责： 1. 处理进入人工审核状态的工具 2. 等待人工操作，不会自动流转到下一状态 */
public class AppManualReviewProcessor implements AppToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AppManualReviewProcessor.class);

    @Override
    public ToolStatus getStatus() {
        return ToolStatus.MANUAL_REVIEW;
    }

    @Override
    public void process(ToolEntity tool) {
        logger.info("工具ID: {} 进入MANUAL_REVIEW状态，等待人工审核。", tool.getId());
        // 人工审核状态只能等待人工审核，不自动处理
    }

    @Override
    public ToolStatus getNextStatus() {
        // 人工审核后的下一状态由人工操作决定，自动流程中不会进入下一步
        return null;
    }
}