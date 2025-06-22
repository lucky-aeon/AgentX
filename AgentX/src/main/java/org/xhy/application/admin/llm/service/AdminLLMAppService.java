package org.xhy.application.admin.llm.service;

import org.springframework.stereotype.Service;
import org.xhy.application.llm.assembler.ModelAssembler;
import org.xhy.application.llm.assembler.ProviderAssembler;
import org.xhy.application.llm.dto.ModelDTO;
import org.xhy.application.llm.dto.ProviderDTO;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.model.enums.ModelType;
import org.xhy.domain.llm.model.enums.ProviderType;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.infrastructure.entity.Operator;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;
import org.xhy.interfaces.dto.llm.request.ModelCreateRequest;
import org.xhy.interfaces.dto.llm.request.ModelUpdateRequest;
import org.xhy.interfaces.dto.llm.request.ProviderCreateRequest;
import org.xhy.interfaces.dto.llm.request.ProviderUpdateRequest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminLLMAppService {

    private final LLMDomainService llmDomainService;

    public AdminLLMAppService(LLMDomainService llmDomainService) {
        this.llmDomainService = llmDomainService;
    }

    /** 创建官方服务商
     * @param providerCreateRequest 请求对象
     * @param userId 用户id */
    public ProviderDTO createProvider(ProviderCreateRequest providerCreateRequest, String userId) {
        ProviderEntity provider = ProviderAssembler.toEntity(providerCreateRequest, userId);
        provider.setIsOfficial(true);
        return ProviderAssembler.toDTO(llmDomainService.createProvider(provider));
    }

    /** 修改服务商
     * @param providerUpdateRequest 请求对象
     * @param userId 用户id */
    public ProviderDTO updateProvider(ProviderUpdateRequest providerUpdateRequest, String userId) {
        // 先获取当前服务商数据
        ProviderEntity existingProvider = llmDomainService.getProvider(providerUpdateRequest.getId());

        // 判断是否需要保留原有的密钥
        if (providerUpdateRequest.getConfig() != null && providerUpdateRequest.getConfig().getApiKey() != null
                && providerUpdateRequest.getConfig().getApiKey().matches("\\*+")) {
            // 如果传入的是掩码，使用原有的密钥
            providerUpdateRequest.getConfig().setApiKey(existingProvider.getConfig().getApiKey());
        }

        ProviderEntity provider = ProviderAssembler.toEntity(providerUpdateRequest, userId);
        provider.setAdmin();
        llmDomainService.updateProvider(provider);
        return ProviderAssembler.toDTO(llmDomainService.getProviderAggregate(provider.getId(), userId));
    }

    /** 删除服务商
     * @param providerId 服务商id
     * @param userId 用户id */
    public void deleteProvider(String providerId, String userId) {
        llmDomainService.deleteProvider(providerId, userId, Operator.ADMIN);
    }

    /** 创建模型
     * @param modelCreateRequest 模型对象
     * @param userId 用户id */
    public ModelDTO createModel(ModelCreateRequest modelCreateRequest, String userId) {
        ModelEntity entity = ModelAssembler.toEntity(modelCreateRequest, userId);
        entity.setAdmin();
        entity.setOfficial(true);
        llmDomainService.createModel(entity);
        return ModelAssembler.toDTO(entity);
    }

    /** 更新模型
     * @param modelUpdateRequest 模型请求对象
     * @param userId 用户id */
    public ModelDTO updateModel(ModelUpdateRequest modelUpdateRequest, String userId) {
        ModelEntity entity = ModelAssembler.toEntity(modelUpdateRequest, userId);
        entity.setAdmin();
        llmDomainService.updateModel(entity);
        return ModelAssembler.toDTO(entity);
    }

    /** 删除模型
     * @param modelId 模型id
     * @param userId 用户id */
    public void deleteModel(String modelId, String userId) {
        llmDomainService.deleteModel(modelId, userId, Operator.ADMIN);
    }

    /** 获取官方服务商列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 官方服务商列表 */
    public List<ProviderDTO> getOfficialProviders(String userId, Integer page, Integer pageSize) {
        // 查询官方服务商并转换为DTO（管理员需要看到所有模型，包括禁用的）
        return llmDomainService.getOfficialProvidersWithAllModels()
                .stream()
                .map(ProviderAssembler::toDTO)
                .collect(Collectors.toList());
    }

    /** 获取服务商详情
     * @param providerId 服务商ID
     * @param userId 用户ID
     * @return 服务商详情 */
    public ProviderDTO getProviderDetail(String providerId, String userId) {
        return ProviderAssembler.toDTO(llmDomainService.getProviderAggregate(providerId, userId));
    }

    /** 切换服务商状态
     * @param providerId 服务商ID
     * @param userId 用户ID */
    public void toggleProviderStatus(String providerId, String userId) {
        llmDomainService.updateProviderStatus(providerId, userId);
    }

    /** 获取支持的协议列表
     * @return 协议列表 */
    public List<ProviderProtocol> getProviderProtocols() {
        return Arrays.asList(ProviderProtocol.values());
    }

    /** 获取官方模型列表
     * @param userId 用户ID
     * @param providerId 服务商ID（可选）
     * @param modelType 模型类型（可选）
     * @param page 页码
     * @param pageSize 每页大小
     * @return 官方模型列表 */
    public List<ModelDTO> getOfficialModels(String userId, String providerId, ModelType modelType, Integer page, Integer pageSize) {
        // 查询官方模型，显示所有状态的模型（不过滤状态）
        return llmDomainService.getOfficialProvidersWithAllModels().stream()
                .flatMap(provider -> provider.getModels().stream()
                        .filter(model -> modelType == null || model.getType() == modelType) // 按类型过滤
                        .filter(model -> providerId == null || provider.getId().equals(providerId))) // 按服务商过滤
                .map(ModelAssembler::toDTO)
                .collect(Collectors.toList());
    }

    /** 切换模型状态
     * @param modelId 模型ID
     * @param userId 用户ID */
    public void toggleModelStatus(String modelId, String userId) {
        llmDomainService.updateModelStatus(modelId, userId);
    }

    /** 获取模型类型列表
     * @return 模型类型列表 */
    public List<ModelType> getModelTypes() {
        return Arrays.asList(ModelType.values());
    }
}
