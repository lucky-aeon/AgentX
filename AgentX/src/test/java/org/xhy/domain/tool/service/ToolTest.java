package org.xhy.domain.tool.service;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.state.impl.PublishingProcessor;

@SpringBootTest
public class ToolTest {

    @Autowired
    private ToolStateService toolStateService;

    @Autowired
    private ToolDomainService toolDomainService;
    @Autowired
    private PublishingProcessor publishApprovedTool;

    @Test
    public void testToolState(){

        ToolEntity tool = toolDomainService.getTool("fcf8589b869aada08e4fe7c29121ddb8");
        toolStateService.processToolState(tool);
        while (true){}
    }
}
