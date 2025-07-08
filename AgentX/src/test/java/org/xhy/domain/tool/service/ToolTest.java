package org.xhy.domain.tool.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.xhy.application.tool.service.ToolStateStateMachineAppService;
import org.xhy.domain.tool.model.ToolEntity;

@SpringBootTest
public class ToolTest {

    @Autowired
    private ToolStateStateMachineAppService toolStateStateMachine;

    @Autowired
    private ToolDomainService toolDomainService;

    @Test
    public void testToolState() {
        ToolEntity tool = toolDomainService.getTool("fcf8589b869aada08e4fe7c29121ddb8");
        toolStateStateMachine.submitToolForProcessing(tool);

        // 简化测试逻辑，避免无限循环
        // while (true) {
        // }
    }
}
