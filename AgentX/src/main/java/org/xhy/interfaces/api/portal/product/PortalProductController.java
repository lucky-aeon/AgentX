package org.xhy.interfaces.api.portal.product;

import org.springframework.web.bind.annotation.*;
import org.xhy.application.product.dto.ProductDTO;
import org.xhy.application.product.service.ProductAppService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

/** 门户商品控制器 负责处理用户对计费商品的查询操作 */
@RestController
@RequestMapping("/products")
public class PortalProductController {

    private final ProductAppService productAppService;

    public PortalProductController(ProductAppService productAppService) {
        this.productAppService = productAppService;
    }

    /** 根据ID获取商品详情
     * 
     * @param productId 商品ID
     * @return 商品详情 */
    @GetMapping("/{productId}")
    public Result<ProductDTO> getProductById(@PathVariable String productId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(productAppService.getProductById(productId));
    }

    /** 根据业务标识获取商品
     * 
     * @param type 计费类型
     * @param serviceId 服务ID
     * @return 商品详情 */
    @GetMapping("/business")
    public Result<ProductDTO> getProductByBusinessKey(@RequestParam String type, @RequestParam String serviceId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(productAppService.getProductByBusinessKey(type, serviceId));
    }

    /** 获取指定类型的活跃商品列表
     * 
     * @param type 计费类型（可选）
     * @return 活跃商品列表 */
    @GetMapping("/active")
    public Result<List<ProductDTO>> getActiveProducts(@RequestParam(required = false) String type) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(productAppService.getActiveProducts(type));
    }

    /** 检查商品是否存在且激活
     * 
     * @param type 计费类型
     * @param serviceId 服务ID
     * @return 是否存在且激活 */
    @GetMapping("/business/active")
    public Result<Boolean> isProductActive(@RequestParam String type, @RequestParam String serviceId) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(productAppService.isProductActive(type, serviceId));
    }
}