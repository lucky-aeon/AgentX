package org.xhy.domain.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.xhy.domain.product.constant.BillingType;
import org.xhy.domain.product.model.ProductEntity;
import org.xhy.domain.product.repository.ProductRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;
import java.util.Set;
import java.util.Collections;

/** 商品领域服务 处理商品相关的核心业务逻辑 */
@Service
public class ProductDomainService {

    private final ProductRepository productRepository;

    public ProductDomainService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** 获取产品仓储（供应用层使用）
     * @return 产品仓储 */
    public ProductRepository getProductRepository() {
        return productRepository;
    }

    /** 根据业务主键查找商品 这是计费系统的核心查询方法
     * @param type 计费类型
     * @param serviceId 业务ID
     * @return 商品实体，如果不存在则返回null */
    public ProductEntity findProductByBusinessKey(String type, String serviceId) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        if (serviceId == null || serviceId.trim().isEmpty()) {
            return null;
        }

        BillingType billingType;
        try {
            billingType = BillingType.fromCode(type);
        } catch (IllegalArgumentException e) {
            return null;
        }

        LambdaQueryWrapper<ProductEntity> wrapper = Wrappers.<ProductEntity>lambdaQuery()
                .eq(ProductEntity::getType, billingType).eq(ProductEntity::getServiceId, serviceId);

        return productRepository.selectOne(wrapper);
    }

    /** 创建商品
     * @param product 商品实体
     * @return 创建后的商品实体 */
    public ProductEntity createProduct(ProductEntity product) {
        // 验证商品信息
        product.validate();

        // 检查业务主键是否已存在
        ProductEntity existing = findProductByBusinessKey(product.getType().getCode(), product.getServiceId());
        if (existing != null) {
            throw new BusinessException("该业务类型和业务ID的商品已存在");
        }

        // 设置默认状态为激活
        if (product.getStatus() == null) {
            product.activate();
        }

        productRepository.insert(product);
        return product;
    }

    /** 更新商品
     * @param product 商品实体
     * @return 更新后的商品实体 */
    public ProductEntity updateProduct(ProductEntity product) {
        if (product.getId() == null || product.getId().trim().isEmpty()) {
            throw new BusinessException("商品ID不能为空");
        }

        // 检查是否存在
        ProductEntity existing = getProductById(product.getId());
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }

        // 检查业务主键是否与其他商品冲突
        ProductEntity conflictProduct = findProductByBusinessKey(product.getType().getCode(), product.getServiceId());
        if (conflictProduct != null && !conflictProduct.getId().equals(product.getId())) {
            throw new BusinessException("该业务类型和业务ID的商品已存在");
        }

        productRepository.checkedUpdateById(product);
        return product;
    }

    /** 根据ID获取商品
     * @param productId 商品ID
     * @return 商品实体，如果不存在则返回null */
    public ProductEntity getProductById(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return null;
        }
        return productRepository.selectById(productId);
    }

    /** 获取指定类型的激活商品
     * @param type 计费类型
     * @return 激活的商品列表 */
    public List<ProductEntity> getActiveProducts(String type) {
        LambdaQueryWrapper<ProductEntity> wrapper = Wrappers.<ProductEntity>lambdaQuery()
                .eq(ProductEntity::getStatus, 1).orderByDesc(ProductEntity::getCreatedAt);

        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(ProductEntity::getType, type);
        }

        return productRepository.selectList(wrapper);
    }

    /** 更新商品状态
     * @param productId 商品ID
     * @param status 新状态 */
    public void updateProductStatus(String productId, Integer status) {
        ProductEntity product = getProductById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        product.updateStatus(status);

        LambdaUpdateWrapper<ProductEntity> updateWrapper = Wrappers.<ProductEntity>lambdaUpdate()
                .eq(ProductEntity::getId, productId).set(ProductEntity::getStatus, status);

        productRepository.checkedUpdate(updateWrapper);
    }

    /** 删除商品（软删除）
     * @param productId 商品ID */
    public void deleteProduct(String productId) {
        ProductEntity product = getProductById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        productRepository.deleteById(productId);
    }

    /** 获取所有商品
     * @return 商品列表 */
    public List<ProductEntity> getAllProducts() {
        LambdaQueryWrapper<ProductEntity> wrapper = Wrappers.<ProductEntity>lambdaQuery()
                .orderByDesc(ProductEntity::getCreatedAt);

        return productRepository.selectList(wrapper);
    }

    /** 检查商品是否存在
     * @param productId 商品ID
     * @return 是否存在 */
    public boolean existsProduct(String productId) {
        return getProductById(productId) != null;
    }

    /** 检查业务标识是否存在
     * @param type 计费类型
     * @param serviceId 业务ID
     * @return 是否存在 */
    public boolean existsByBusinessKey(String type, String serviceId) {
        return findProductByBusinessKey(type, serviceId) != null;
    }

    /** 检查商品是否存在且激活
     * @param type 计费类型
     * @param serviceId 业务ID
     * @return 是否存在且激活 */
    public boolean isProductActiveByBusinessKey(String type, String serviceId) {
        ProductEntity product = findProductByBusinessKey(type, serviceId);
        return product != null && product.isActive();
    }

    /** 批量根据ID获取商品 - 避免循环查询
     * @param productIds 商品ID集合
     * @return 商品列表 */
    public List<ProductEntity> getProductsByIds(Set<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<ProductEntity> wrapper = Wrappers.<ProductEntity>lambdaQuery().in(ProductEntity::getId,
                productIds);

        return productRepository.selectList(wrapper);
    }
}