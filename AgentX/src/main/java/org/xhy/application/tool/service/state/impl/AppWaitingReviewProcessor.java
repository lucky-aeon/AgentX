package org.xhy.application.tool.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.application.tool.service.state.AppToolStateProcessor;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;

/** 等待审核状态处理器
 * 
 * 职责： 1. 处理刚提交的工具，从等待审核状态开始流转 2. 自动转换到GitHub URL验证状态 */
public class AppWaitingReviewProcessor implements AppToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AppWaitingReviewProcessor.class);

    @Override
    public ToolStatus getStatus() {
        return ToolStatus.WAITING_REVIEW;
    }

    @Override
    public void process(ToolEntity tool) {
        logger.info("工具ID: {} 进入WAITING_REVIEW状态，开始审核流程。", tool.getId());
        // 等待审核状态不需要特殊处理，直接进入下一步
    }

    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.GITHUB_URL_VALIDATE;
    }
}