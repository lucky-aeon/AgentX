package org.xhy.interfaces.api.admin;

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
import org.xhy.application.admin.llm.service.AdminLLMAppService;
import org.xhy.application.llm.dto.ModelDTO;
import org.xhy.application.llm.dto.ProviderDTO;
import org.xhy.domain.llm.model.enums.ModelType;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.llm.request.ModelCreateRequest;
import org.xhy.interfaces.dto.llm.request.ModelUpdateRequest;
import org.xhy.interfaces.dto.llm.request.ProviderCreateRequest;
import org.xhy.interfaces.dto.llm.request.ProviderUpdateRequest;

import java.util.List;

/** 管理员LLM管理 */
@RestController
@RequestMapping("/admin/llms")
public class AdminLLMController {

    private final AdminLLMAppService adminLLMAppService;

    public AdminLLMController(AdminLLMAppService adminLLMAppService) {
        this.adminLLMAppService = adminLLMAppService;
    }

    /** 获取服务商列表
     * @param page 页码（可选，默认1）
     * @param pageSize 每页大小（可选，默认20）
     * @return 服务商列表 */
    @GetMapping("/providers")
    public Result<List<ProviderDTO>> getProviders(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(adminLLMAppService.getOfficialProviders(userId, page, pageSize));
    }

    /** 获取服务商详情
     * @param providerId 服务商ID
     * @return 服务商详情 */
    @GetMapping("/providers/{providerId}")
    public Result<ProviderDTO> getProviderDetail(@PathVariable String providerId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(adminLLMAppService.getProviderDetail(providerId, userId));
    }

    /** 创建服务商
     * @param request 请求对象 */
    @PostMapping("/providers")
    public Result<ProviderDTO> createProvider(@RequestBody @Validated ProviderCreateRequest request) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(adminLLMAppService.createProvider(request, userId));
    }

    /** 更新服务商
     * @param id 服务商id
     * @param request 请求对象 */
    @PutMapping("/providers/{id}")
    public Result<ProviderDTO> updateProvider(@PathVariable String id,
            @RequestBody @Validated ProviderUpdateRequest request) {
        String userId = UserContext.getCurrentUserId();
        request.setId(id);
        return Result.success(adminLLMAppService.updateProvider(request, userId));
    }

    /** 切换服务商状态
     * @param id 服务商ID
     * @return 操作结果 */
    @PostMapping("/providers/{id}/status")
    public Result<Void> toggleProviderStatus(@PathVariable String id) {
        String userId = UserContext.getCurrentUserId();
        adminLLMAppService.toggleProviderStatus(id, userId);
        return Result.success();
    }

    /** 删除服务商
     * @param id 服务商id */
    @DeleteMapping("/providers/{id}")
    public Result<Void> deleteProvider(@PathVariable String id) {
        String userId = UserContext.getCurrentUserId();
        adminLLMAppService.deleteProvider(id, userId);
        return Result.success();
    }

    /** 获取支持的协议列表
     * @return 协议列表 */
    @GetMapping("/providers/protocols")
    public Result<List<ProviderProtocol>> getProviderProtocols() {
        return Result.success(adminLLMAppService.getProviderProtocols());
    }

    /** 获取模型列表
     * @param providerId 服务商ID（可选，不传则查询所有）
     * @param modelType 模型类型（可选）
     * @param page 页码（可选，默认1）
     * @param pageSize 每页大小（可选，默认20）
     * @return 模型列表 */
    @GetMapping("/models")
    public Result<List<ModelDTO>> getModels(
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        String userId = UserContext.getCurrentUserId();
        ModelType type = modelType != null ? ModelType.fromCode(modelType) : null;
        return Result.success(adminLLMAppService.getOfficialModels(userId, providerId, type, page, pageSize));
    }

    /** 创建模型
     * @param request 请求对象 */
    @PostMapping("/models")
    public Result<ModelDTO> createModel(@RequestBody @Validated ModelCreateRequest request) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(adminLLMAppService.createModel(request, userId));
    }

    /** 更新模型
     * @param id 更新的id
     * @param request 请求对象 */
    @PutMapping("/models/{id}")
    public Result<ModelDTO> updateModel(@PathVariable String id, @RequestBody @Validated ModelUpdateRequest request) {
        String userId = UserContext.getCurrentUserId();
        request.setId(id);
        return Result.success(adminLLMAppService.updateModel(request, userId));
    }

    /** 切换模型状态
     * @param id 模型ID
     * @return 操作结果 */
    @PostMapping("/models/{id}/status")
    public Result<Void> toggleModelStatus(@PathVariable String id) {
        String userId = UserContext.getCurrentUserId();
        adminLLMAppService.toggleModelStatus(id, userId);
        return Result.success();
    }

    /** 删除模型
     * @param id 模型id */
    @DeleteMapping("/models/{id}")
    public Result<Void> deleteModel(@PathVariable String id) {
        String userId = UserContext.getCurrentUserId();
        adminLLMAppService.deleteModel(id, userId);
        return Result.success();
    }

    /** 获取模型类型列表
     * @return 模型类型列表 */
    @GetMapping("/models/types")
    public Result<List<ModelType>> getModelTypes() {
        return Result.success(adminLLMAppService.getModelTypes());
    }

}
