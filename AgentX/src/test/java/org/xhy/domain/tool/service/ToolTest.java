package org.xhy.domain.tool.service;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ToolTest {

    @Autowired
    private ToolStateService toolStateService;

    @Test
    public void testToolState(){
        toolStateService.submitTool("fcf8589b869aada08e4fe7c29121ddb8");
        while (true){

        }
    }
}
