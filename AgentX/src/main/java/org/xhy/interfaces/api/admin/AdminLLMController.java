package org.xhy.interfaces.api.admin;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.admin.llm.service.AdminLLMAppService;
import org.xhy.application.llm.dto.ModelDTO;
import org.xhy.application.llm.dto.ProviderDTO;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.llm.request.ModelCreateRequest;
import org.xhy.interfaces.dto.llm.request.ModelUpdateRequest;
import org.xhy.interfaces.dto.llm.request.ProviderCreateRequest;
import org.xhy.interfaces.dto.llm.request.ProviderUpdateRequest;

/**
 * 管理员LLM管理
 */
@RestController
@RequestMapping("/admin/llm")
public class AdminLLMController {

    private final AdminLLMAppService adminLLMAppService;

    public AdminLLMController(AdminLLMAppService adminLLMAppService) {
        this.adminLLMAppService = adminLLMAppService;
    }

    /**
     * 创建服务商
     * @param request 请求对象
     */
    @PostMapping("")
    public Result<ProviderDTO> createProvider(@RequestBody @Validated ProviderCreateRequest  request){
        String userId = UserContext.getCurrentUserId();
        return Result.success(adminLLMAppService.createProvider(request, userId));
    }

    /**
     * 更新服务商
     * @param id 服务商id
     * @param request 请求对象
     */
    @PutMapping("/{id}")
    public Result<ProviderDTO> updateProvider(@PathVariable String id, @RequestBody @Validated ProviderUpdateRequest request){
        String userId = UserContext.getCurrentUserId();
        request.setId(id);
        return Result.success(adminLLMAppService.updateProvider(request, userId));
    }

    /**
     * 删除服务商
     * @param id 服务商id
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProvider(@PathVariable String id){
        String userId = UserContext.getCurrentUserId();
        adminLLMAppService.deleteProvider(id, userId);
        return Result.success();
    }

    /**
     * 创建模型
     * @param request 请求对象
     */
    @PostMapping("/model")
    public Result<ModelDTO> createModel(@RequestBody @Validated ModelCreateRequest request){
        String userId = UserContext.getCurrentUserId();
        return Result.success(adminLLMAppService.createModel(request, userId));
    }

    /**
     * 更新模型
     * @param id 更新的id
     * @param request 请求对象
     */
    @PutMapping("/model/{id}")
    public Result<ModelDTO> updateModel(@PathVariable String id, @RequestBody @Validated ModelUpdateRequest request){
        String userId = UserContext.getCurrentUserId();
        request.setId(id);
        return Result.success(adminLLMAppService.updateModel(request, userId));
    }


    /**
     * 删除模型
     * @param id 模型id
     */
    @DeleteMapping("/model/{id}")
    public Result<Void> deleteModel(@PathVariable String id){
        String userId = UserContext.getCurrentUserId();
        adminLLMAppService.deleteModel(id, userId);
        return Result.success();
    }

}
