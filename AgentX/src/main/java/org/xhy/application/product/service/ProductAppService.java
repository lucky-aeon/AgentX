package org.xhy.application.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.product.assembler.ProductAssembler;
import org.xhy.application.product.dto.ProductDTO;
import org.xhy.domain.product.model.ProductEntity;
import org.xhy.domain.product.service.ProductDomainService;
import org.xhy.domain.rule.service.RuleDomainService;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.dto.product.request.CreateProductRequest;
import org.xhy.interfaces.dto.product.request.QueryProductRequest;
import org.xhy.interfaces.dto.product.request.UpdateProductRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 商品应用服务 处理商品相关的业务流程编排 */
@Service
public class ProductAppService {

    private final ProductDomainService productDomainService;
    private final RuleDomainService ruleDomainService;
    private final LLMDomainService llmDomainService;

    public ProductAppService(ProductDomainService productDomainService, RuleDomainService ruleDomainService,
            LLMDomainService llmDomainService) {
        this.productDomainService = productDomainService;
        this.ruleDomainService = ruleDomainService;
        this.llmDomainService = llmDomainService;
    }

    /** 创建商品
     * @param request 创建商品请求
     * @return 商品DTO */
    @Transactional
    public ProductDTO createProduct(CreateProductRequest request) {
        // 验证规则是否存在
        if (!ruleDomainService.existsRule(request.getRuleId())) {
            throw new BusinessException("规则不存在: " + request.getRuleId());
        }

        // 转换为实体并创建
        ProductEntity entity = ProductAssembler.toEntity(request);
        ProductEntity createdEntity = productDomainService.createProduct(entity);

        return ProductAssembler.toDTO(createdEntity);
    }

    /** 更新商品
     * @param request 更新商品请求
     * @return 商品DTO */
    @Transactional
    public ProductDTO updateProduct(UpdateProductRequest request, String productId) {
        // 验证规则是否存在（如果有更新规则ID）
        if (request.getRuleId() != null && !ruleDomainService.existsRule(request.getRuleId())) {
            throw new BusinessException("规则不存在: " + request.getRuleId());
        }

        ProductEntity productEntity = ProductAssembler.toEntity(request, productId);

        // 保存更新
        ProductEntity updatedEntity = productDomainService.updateProduct(productEntity);

        return ProductAssembler.toDTO(updatedEntity);
    }

    /** 根据ID获取商品
     * @param productId 商品ID
     * @return 商品DTO */
    public ProductDTO getProductById(String productId) {
        ProductEntity entity = productDomainService.getProductById(productId);
        if (entity == null) {
            throw new BusinessException("商品不存在");
        }
        return ProductAssembler.toDTO(entity);
    }

    /** 根据业务主键获取商品
     * @param type 计费类型
     * @param serviceId 业务ID
     * @return 商品DTO，如果不存在则返回null */
    public ProductDTO getProductByBusinessKey(String type, String serviceId) {
        ProductEntity entity = productDomainService.findProductByBusinessKey(type, serviceId);
        return ProductAssembler.toDTO(entity);
    }

    /** 分页查询商品
     * @param request 查询请求
     * @return 商品分页结果 */
    public Page<ProductDTO> getProducts(QueryProductRequest request) {
        // 构建查询条件
        LambdaQueryWrapper<ProductEntity> wrapper = Wrappers.<ProductEntity>lambdaQuery()
                .eq(StringUtils.isNotBlank(request.getType()), ProductEntity::getType, request.getType())
                .eq(request.getStatus() != null, ProductEntity::getStatus, request.getStatus())
                .like(StringUtils.isNotBlank(request.getKeyword()), ProductEntity::getName, request.getKeyword())
                .orderByDesc(ProductEntity::getCreatedAt);

        // 分页查询
        Page<ProductEntity> entityPage = new Page<>(request.getPage(), request.getPageSize());
        entityPage = productDomainService.getProductRepository().selectPage(entityPage, wrapper);

        // 转换结果
        List<ProductDTO> dtoList = ProductAssembler.toDTOs(entityPage.getRecords());

        Page<ProductDTO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        resultPage.setRecords(dtoList);

        return resultPage;
    }

    /** 获取激活的商品列表
     * @param type 商品类型（可选）
     * @return 商品DTO列表 */
    public List<ProductDTO> getActiveProducts(String type) {
        List<ProductEntity> entities = productDomainService.getActiveProducts(type);
        List<ProductDTO> productDTOs = ProductAssembler.toDTOs(entities);

        // 为模型类型的商品填充模型信息
        enrichModelInfo(productDTOs);

        return productDTOs;
    }

    /** 为模型类型商品填充模型信息
     * @param productDTOs 商品DTO列表 */
    private void enrichModelInfo(List<ProductDTO> productDTOs) {
        // 筛选出模型类型的商品
        List<ProductDTO> modelProducts = productDTOs.stream().filter(product -> "MODEL_USAGE".equals(product.getType()))
                .collect(Collectors.toList());

        if (modelProducts.isEmpty()) {
            return;
        }

        // 收集所有需要查询的模型ID
        Set<String> modelIds = modelProducts.stream().map(ProductDTO::getServiceId).collect(Collectors.toSet());

        try {
            // 批量查询模型信息
            List<ModelEntity> models = llmDomainService.getModelsByIds(modelIds);
            Map<String, ModelEntity> modelMap = models.stream()
                    .collect(Collectors.toMap(ModelEntity::getId, model -> model));

            // 收集需要查询的服务商ID
            Set<String> providerIds = models.stream().map(ModelEntity::getProviderId).collect(Collectors.toSet());

            // 批量查询服务商信息
            Map<String, ProviderEntity> providerMap = providerIds.stream()
                    .collect(Collectors.toMap(providerId -> providerId, providerId -> {
                        try {
                            return llmDomainService.findProviderById(providerId);
                        } catch (Exception e) {
                            return null; // 如果服务商不存在，返回null
                        }
                    }));

            // 为每个模型商品填充信息
            for (ProductDTO product : modelProducts) {
                ModelEntity model = modelMap.get(product.getServiceId());
                if (model != null) {
                    product.setModelName(model.getName());
                    product.setModelId(model.getModelId());

                    // 设置服务商名称
                    ProviderEntity provider = providerMap.get(model.getProviderId());
                    if (provider != null) {
                        product.setProviderName(provider.getName());
                    }
                }
            }
        } catch (Exception e) {
            // 如果查询模型信息失败，不影响商品查询，只是没有模型信息
            // 可以记录日志但不抛出异常
        }
    }

    /** 更新商品状态
     * @param productId 商品ID
     * @param status 新状态 */
    @Transactional
    public void updateProductStatus(String productId, Integer status) {
        productDomainService.updateProductStatus(productId, status);
    }

    /** 删除商品
     * @param productId 商品ID */
    @Transactional
    public void deleteProduct(String productId) {
        productDomainService.deleteProduct(productId);
    }

    /** 启用商品
     * @param productId 商品ID
     * @return 商品DTO */
    @Transactional
    public ProductDTO enableProduct(String productId) {
        ProductEntity entity = productDomainService.getProductById(productId);
        if (entity == null) {
            throw new BusinessException("商品不存在");
        }

        entity.activate();
        ProductEntity updatedEntity = productDomainService.updateProduct(entity);

        return ProductAssembler.toDTO(updatedEntity);
    }

    /** 禁用商品
     * @param productId 商品ID
     * @return 商品DTO */
    @Transactional
    public ProductDTO disableProduct(String productId) {
        ProductEntity entity = productDomainService.getProductById(productId);
        if (entity == null) {
            throw new BusinessException("商品不存在");
        }

        entity.deactivate();
        ProductEntity updatedEntity = productDomainService.updateProduct(entity);

        return ProductAssembler.toDTO(updatedEntity);
    }

    /** 获取所有商品列表
     * @return 商品DTO列表 */
    public List<ProductDTO> getAllProducts() {
        List<ProductEntity> entities = productDomainService.getAllProducts();
        return ProductAssembler.toDTOs(entities);
    }

    /** 检查商品是否存在
     * @param productId 商品ID
     * @return 是否存在 */
    public boolean existsProduct(String productId) {
        return productDomainService.existsProduct(productId);
    }

    /** 检查业务标识是否存在
     * @param type 计费类型
     * @param serviceId 业务ID
     * @return 是否存在 */
    public boolean existsByBusinessKey(String type, String serviceId) {
        return productDomainService.existsByBusinessKey(type, serviceId);
    }

    /** 检查商品是否存在且激活
     * @param type 计费类型
     * @param serviceId 业务ID
     * @return 是否存在且激活 */
    public boolean isProductActive(String type, String serviceId) {
        ProductEntity product = productDomainService.findProductByBusinessKey(type, serviceId);
        return product != null && product.isActive();
    }
}