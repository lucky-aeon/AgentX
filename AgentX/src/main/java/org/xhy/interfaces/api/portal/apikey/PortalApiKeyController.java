package org.xhy.interfaces.api.portal.apikey;

import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.apikey.dto.ApiKeyDTO;
import org.xhy.application.apikey.service.ApiKeyAppService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.apikey.request.CreateApiKeyRequest;
import org.xhy.interfaces.dto.apikey.request.QueryApiKeyRequest;
import org.xhy.interfaces.dto.apikey.request.UpdateApiKeyStatusRequest;
import org.xhy.interfaces.dto.apikey.response.ApiKeyResponse;

import java.util.List;
import java.util.stream.Collectors;

/** API密钥管理控制器 */
@RestController
@RequestMapping("/api-keys")
public class PortalApiKeyController {

    private final ApiKeyAppService apiKeyAppService;

    public PortalApiKeyController(ApiKeyAppService apiKeyAppService) {
        this.apiKeyAppService = apiKeyAppService;
    }

    /** 创建API密钥
     *
     * @param request 创建请求
     * @return 创建的API密钥 */
    @PostMapping
    public Result<ApiKeyResponse> createApiKey(@RequestBody @Validated CreateApiKeyRequest request) {
        String userId = UserContext.getCurrentUserId();
        ApiKeyDTO apiKeyDTO = apiKeyAppService.createApiKey(request.getAgentId(), request.getName(), userId);

        ApiKeyResponse response = new ApiKeyResponse();
        BeanUtils.copyProperties(apiKeyDTO, response);

        return Result.success(response);
    }

    /** 获取用户的API密钥列表
     *
     * @param queryRequest 查询条件
     * @return API密钥列表 */
    @GetMapping
    public Result<List<ApiKeyResponse>> getUserApiKeys(QueryApiKeyRequest queryRequest) {
        String userId = UserContext.getCurrentUserId();
        List<ApiKeyDTO> apiKeys = apiKeyAppService.getUserApiKeys(userId, queryRequest);

        List<ApiKeyResponse> responses = apiKeys.stream().map(dto -> {
            ApiKeyResponse response = new ApiKeyResponse();
            BeanUtils.copyProperties(dto, response);
            return response;
        }).collect(Collectors.toList());

        return Result.success(responses);
    }

    /** 获取Agent的API密钥列表
     *
     * @param agentId Agent ID
     * @return API密钥列表 */
    @GetMapping("/agent/{agentId}")
    public Result<List<ApiKeyResponse>> getAgentApiKeys(@PathVariable String agentId) {
        String userId = UserContext.getCurrentUserId();
        List<ApiKeyDTO> apiKeys = apiKeyAppService.getAgentApiKeys(agentId, userId);

        List<ApiKeyResponse> responses = apiKeys.stream().map(dto -> {
            ApiKeyResponse response = new ApiKeyResponse();
            BeanUtils.copyProperties(dto, response);
            return response;
        }).collect(Collectors.toList());

        return Result.success(responses);
    }

    /** 获取API密钥详情
     *
     * @param apiKeyId API密钥ID
     * @return API密钥详情 */
    @GetMapping("/{apiKeyId}")
    public Result<ApiKeyResponse> getApiKey(@PathVariable String apiKeyId) {
        String userId = UserContext.getCurrentUserId();
        ApiKeyDTO apiKeyDTO = apiKeyAppService.getApiKey(apiKeyId, userId);

        ApiKeyResponse response = new ApiKeyResponse();
        BeanUtils.copyProperties(apiKeyDTO, response);

        return Result.success(response);
    }

    /** 更新API密钥状态
     *
     * @param apiKeyId API密钥ID
     * @param request 更新请求
     * @return 操作结果 */
    @PutMapping("/{apiKeyId}/status")
    public Result<Void> updateApiKeyStatus(@PathVariable String apiKeyId,
            @RequestBody @Validated UpdateApiKeyStatusRequest request) {
        String userId = UserContext.getCurrentUserId();
        apiKeyAppService.updateApiKeyStatus(apiKeyId, request.getStatus(), userId);

        return Result.success();
    }

    /** 删除API密钥
     *
     * @param apiKeyId API密钥ID
     * @return 操作结果 */
    @DeleteMapping("/{apiKeyId}")
    public Result<Void> deleteApiKey(@PathVariable String apiKeyId) {
        String userId = UserContext.getCurrentUserId();
        apiKeyAppService.deleteApiKey(apiKeyId, userId);

        return Result.success();
    }

    /** 重置API密钥
     *
     * @param apiKeyId API密钥ID
     * @return 新的API密钥 */
    @PostMapping("/{apiKeyId}/reset")
    public Result<ApiKeyResponse> resetApiKey(@PathVariable String apiKeyId) {
        String userId = UserContext.getCurrentUserId();
        ApiKeyDTO apiKeyDTO = apiKeyAppService.resetApiKey(apiKeyId, userId);

        ApiKeyResponse response = new ApiKeyResponse();
        BeanUtils.copyProperties(apiKeyDTO, response);

        return Result.success(response);
    }
}