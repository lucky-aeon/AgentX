package org.xhy.interfaces.api.admin;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.llm.dto.ProviderDTO;
import org.xhy.application.llm.service.LLMAppService;
import org.xhy.interfaces.api.common.Result;

/**
 * 管理员LLM管理
 */
@RestController
@RequestMapping("/admin/llm")
public class AdminLLMController {

    private final LLMAppService llmAppService;

    public AdminLLMController(LLMAppService llmAppService) {
        this.llmAppService = llmAppService;
    }

    @PostMapping("")
    public Result<ProviderDTO> createProvider(){
//        llmAppService.createProvider()
        return Result.success();
    }
}
