package org.xhy.application.llm.service;

import org.springframework.stereotype.Service;
import org.xhy.application.llm.assembler.ProviderAssembler;
import org.xhy.application.llm.dto.ProviderDTO;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.service.LlmDomainService;
import org.xhy.interfaces.dto.llm.ProviderCreateRequest;
import org.xhy.interfaces.dto.llm.ProviderUpdateRequest;

@Service
public class LLMAppService {

    private final LlmDomainService llmDomainService;

    public LLMAppService(LlmDomainService llmDomainService) {
        this.llmDomainService = llmDomainService;
    }


    /**
     * 创建服务商
     * @param providerCreateRequest 请求对象
     * @param userId 用户id
     * @return ProviderDTO
     */
    public ProviderDTO createProvider(ProviderCreateRequest providerCreateRequest,String userId) {
        ProviderEntity provider = ProviderAssembler.toEntity(providerCreateRequest, userId);
        String providerId = llmDomainService.createProvider(provider);
        provider.setId(providerId);
        return ProviderAssembler.toDTO(provider);
    }

    /**
     * 更新服务商
     * @param providerUpdateRequest 更新对象
     * @param userId 用户id
     * @return ProviderDTO
     */
    public ProviderDTO updateProvider(ProviderUpdateRequest providerUpdateRequest, String userId) {
        ProviderEntity provider = ProviderAssembler.toEntity(providerUpdateRequest, userId);
        llmDomainService.updateProvider(provider);
        return ProviderAssembler.toDTO(provider);
    }


    /**
     * 获取服务商
     * @param providerId 服务商id
     * @return ProviderDTO
     */
    public ProviderDTO getProvider(String providerId, String userId) {
        ProviderEntity provider = llmDomainService.getProvider(providerId,userId);
        return ProviderAssembler.toDTO(provider);
    }

    /**
     * 删除服务商
     * @param providerId 服务商id
     * @param userId 用户id
     */
    public void deleteProvider(String providerId, String userId) {
        llmDomainService.deleteProvider(providerId,userId);
    }
}
