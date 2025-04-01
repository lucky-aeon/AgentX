package org.xhy.application.admin.llm.service;


import org.springframework.stereotype.Service;
import org.xhy.application.llm.assembler.ProviderAssembler;
import org.xhy.application.llm.dto.ProviderDTO;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.service.LlmDomainService;
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

    public ProviderDTO updateProvider(ProviderUpdateRequest providerUpdateRequest, String userId) {
        ProviderEntity provider = ProviderAssembler.toEntity(providerUpdateRequest, userId);

//        llmDomainService.updateProvider();
        return null;
    }
}
