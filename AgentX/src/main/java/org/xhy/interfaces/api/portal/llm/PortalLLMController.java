package org.xhy.interfaces.api.portal.llm;


import java.util.Arrays;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.llm.dto.ModelDTO;
import org.xhy.application.llm.dto.ProviderDTO;
import org.xhy.application.llm.service.LLMAppService;
import org.xhy.domain.llm.model.enums.ModelType;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;
import org.xhy.domain.llm.model.enums.ProviderType;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.llm.request.ModelCreateRequest;
import org.xhy.interfaces.dto.llm.request.ModelUpdateRequest;
import org.xhy.interfaces.dto.llm.request.ProviderCreateRequest;
import org.xhy.interfaces.dto.llm.request.ProviderUpdateRequest;


/**
 * 大模型服务商
 */
@RestController
@RequestMapping("/llm")
public class PortalLLMController {


    public final LLMAppService llmAppService;

    public PortalLLMController(LLMAppService llmAppService) {
        this.llmAppService = llmAppService;
    }

    /**
     * 获取服务商详细信息
     * @param providerId 服务商id
     */
    @GetMapping("/providers/{providerId}")
    public Result<ProviderDTO> getProviderDetail(@PathVariable String providerId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(llmAppService.getProviderDetail(providerId, userId));
    }

    /**
     * 获取服务商列表，支持按类型过滤
     * @param type 服务商类型：all-所有，official-官方，user-用户的（默认）
     * @return 服务商列表
     */
    @GetMapping("/providers")
    public Result<List<ProviderDTO>> getProviders(
            @RequestParam(required = false, defaultValue = "all") String type) {

        ProviderType providerType = ProviderType.fromCode(type);
        String userId = UserContext.getCurrentUserId();
        return Result.success(llmAppService.getProvidersByType(providerType, userId));
    }

    /**
     * 创建服务提供商
     * @param providerCreateRequest 服务提供商创建请求
     */
    @PostMapping("/providers")
    public Result<ProviderDTO> createProvider(@RequestBody ProviderCreateRequest providerCreateRequest) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(llmAppService.createProvider(providerCreateRequest, userId));
    }

    /**
     * 更新服务提供商
     * @param providerUpdateRequest 服务提供商更新请求
     */
    @PutMapping("/providers")
    public Result<ProviderDTO> updateProvider(@RequestBody ProviderUpdateRequest providerUpdateRequest) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(llmAppService.updateProvider(providerUpdateRequest, userId));
    }

    /**
     * 修改服务商状态
     * @param providerId 服务商id
     */
    @PostMapping("/providers/{providerId}/status")
    public Result<Void> updateProviderStatus(@PathVariable String providerId){
        String userId = UserContext.getCurrentUserId();
        llmAppService.updateProviderStatus(providerId,userId);
        return Result.success();
    }
    

    /**
     * 删除服务提供商
     * @param providerId 服务提供商ID
     */
    @DeleteMapping("/providers/{providerId}")
    public Result<Void> deleteProvider(@PathVariable String providerId) {
        String userId = UserContext.getCurrentUserId();
        llmAppService.deleteProvider(providerId, userId);
        return Result.success();
    }
    
    /**
     * 获取服务提供商列表
     */
    @GetMapping("/providers/protocols")
    public Result<List<ProviderProtocol>> getProviders() {
        return Result.success(llmAppService.getUserProviderProtocols());
    }

    /**
     * 添加模型
     * @param modelCreateRequest ModelCreateRequest
     */
    @PostMapping("/models")
    public Result<ModelDTO> createModel(@RequestBody ModelCreateRequest modelCreateRequest) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(llmAppService.createModel(modelCreateRequest, userId));
    }

    /**
     * 修改模型
     * @param modelUpdateRequest ModelUpdateRequest
     */
    @PutMapping("/models")
    public Result<ModelDTO> updateModel(@RequestBody @Validated ModelUpdateRequest modelUpdateRequest) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(llmAppService.updateModel(modelUpdateRequest, userId));
    }

    /**
     * 删除模型
     * @param modelId 模型主键
     */
    @DeleteMapping("/models/{modelId}")
    public Result<Void> deleteModel(@PathVariable String modelId) {
        String userId = UserContext.getCurrentUserId();
        llmAppService.deleteModel(modelId, userId);
        return Result.success();
    }

    /**
     * 修改模型状态
     * @param modelId 模型主键
     */
    @PutMapping("/models/{modelId}/status")
    public Result<Void> updateModelStatus(@PathVariable String modelId) {
        String userId = UserContext.getCurrentUserId();
        llmAppService.updateModelStatus(modelId, userId);
        return Result.success();
    }

    /**
     * 获取模型类型
     * @return
     */
    @GetMapping("/models/types")
    public Result<List<ModelType>> getModelTypes() {
        return Result.success(Arrays.asList(ModelType.values()));
    }


    /**
     * 获取所有激活模型
     * @param modelType 模型类型（可选），不传则查询所有类型
     * @return 模型列表
     */
    @GetMapping("/models")
    public Result<List<ModelDTO>> getModels(@RequestParam(required = false) String modelType) {
        String userId = UserContext.getCurrentUserId();
        ModelType type = modelType != null ? ModelType.fromCode(modelType) : null;
        return Result.success(llmAppService.getActiveModelsByType(ProviderType.ALL, userId, type));
    }
}
