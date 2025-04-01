package org.xhy.application.admin.llm.service;


import org.springframework.stereotype.Service;
import org.xhy.application.llm.assembler.ModelAssembler;
import org.xhy.application.llm.assembler.ProviderAssembler;
import org.xhy.application.llm.dto.ModelDTO;
import org.xhy.application.llm.dto.ProviderDTO;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.service.LlmDomainService;
import org.xhy.infrastructure.entity.Operator;
import org.xhy.interfaces.dto.llm.ModelCreateRequest;
import org.xhy.interfaces.dto.llm.ModelUpdateRequest;
import org.xhy.interfaces.dto.llm.ProviderCreateRequest;
import org.xhy.interfaces.dto.llm.ProviderUpdateRequest;

@Service
public class AdminLLMAppService {

    private final LlmDomainService llmDomainService;

    public AdminLLMAppService(LlmDomainService llmDomainService) {
        this.llmDomainService = llmDomainService;
    }


    /**
     * 创建官方服务商
     * @param providerCreateRequest 请求对象
     * @param userId 用户id
     */
    public ProviderDTO createProvider(ProviderCreateRequest providerCreateRequest,String userId) {
        ProviderEntity provider = ProviderAssembler.toEntity(providerCreateRequest, userId);
        provider.setIsOfficial(true);
        return ProviderAssembler.toDTO(llmDomainService.createProvider(provider));
    }

    /**
     * 修改服务商
     * @param providerUpdateRequest 请求对象
     * @param userId 用户id
     */
    public ProviderDTO updateProvider(ProviderUpdateRequest providerUpdateRequest, String userId) {
        ProviderEntity provider = ProviderAssembler.toEntity(providerUpdateRequest, userId);
        provider.setAdmin();
        llmDomainService.updateProvider(provider);
        return null;
    }

    /**
     * 删除服务商
     * @param providerId 服务商id
     * @param userId 用户id
     */
    public void deleteProvider(String providerId,String userId) {
        llmDomainService.deleteProvider(providerId, userId,Operator.ADMIN);
    }

    /**
     * 创建模型
     * @param modelCreateRequest 模型对象
     * @param userId 用户id
     */
    public ModelDTO createModel(ModelCreateRequest modelCreateRequest, String userId) {
        ModelEntity entity = ModelAssembler.toEntity(modelCreateRequest, userId);
        entity.setAdmin();
        entity.setOfficial(true);
        llmDomainService.createModel(entity);
        return ModelAssembler.toDTO(entity);
    }

    /**
     * 更新模型
     * @param modelUpdateRequest 模型请求对象
     * @param userId 用户id
     */
    public ModelDTO updateModel(ModelUpdateRequest modelUpdateRequest, String userId) {
        ModelEntity entity = ModelAssembler.toEntity(modelUpdateRequest, userId);
        entity.setAdmin();
        llmDomainService.updateModel(entity);
        return ModelAssembler.toDTO(entity);
    }

    /**
     * 删除模型
     * @param modelId 模型id
     * @param userId 用户id
     */
    public void deleteModel(String modelId,String userId) {
        llmDomainService.deleteModel(modelId, userId,Operator.ADMIN);
    }
}
