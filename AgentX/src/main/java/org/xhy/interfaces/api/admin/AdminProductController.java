package org.xhy.interfaces.api.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.product.dto.ProductDTO;
import org.xhy.application.product.service.ProductAppService;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.product.request.CreateProductRequest;
import org.xhy.interfaces.dto.product.request.QueryProductRequest;
import org.xhy.interfaces.dto.product.request.UpdateProductRequest;

import java.util.List;

/** 管理员商品管理控制器 负责处理管理员对计费商品的管理操作 */
@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductAppService productAppService;

    public AdminProductController(ProductAppService productAppService) {
        this.productAppService = productAppService;
    }

    /** 分页获取商品列表
     * 
     * @param queryProductRequest 查询参数
     * @return 商品分页列表 */
    @GetMapping
    public Result<Page<ProductDTO>> getProducts(QueryProductRequest queryProductRequest) {
        return Result.success(productAppService.getProducts(queryProductRequest));
    }

    /** 获取所有商品列表
     * 
     * @return 商品列表 */
    @GetMapping("/all")
    public Result<List<ProductDTO>> getAllProducts() {
        return Result.success(productAppService.getAllProducts());
    }

    /** 根据ID获取商品详情
     * 
     * @param productId 商品ID
     * @return 商品详情 */
    @GetMapping("/{productId}")
    public Result<ProductDTO> getProductById(@PathVariable String productId) {
        return Result.success(productAppService.getProductById(productId));
    }

    /** 根据业务标识获取商品
     * 
     * @param type 计费类型
     * @param serviceId 服务ID
     * @return 商品详情 */
    @GetMapping("/business")
    public Result<ProductDTO> getProductByBusinessKey(@RequestParam String type, @RequestParam String serviceId) {
        return Result.success(productAppService.getProductByBusinessKey(type, serviceId));
    }

    /** 创建商品
     * 
     * @param request 创建商品请求
     * @return 创建的商品 */
    @PostMapping
    public Result<ProductDTO> createProduct(@RequestBody @Validated CreateProductRequest request) {
        ProductDTO product = productAppService.createProduct(request);
        return Result.success(product);
    }

    /** 更新商品
     * 
     * @param request 更新商品请求
     * @return 更新后的商品 */
    @PutMapping("/{productId}")
    public Result<ProductDTO> updateProduct(@RequestBody @Validated UpdateProductRequest request,
            @PathVariable String productId) {
        ProductDTO product = productAppService.updateProduct(request, productId);
        return Result.success(product);
    }

    /** 删除商品
     * 
     * @param productId 商品ID
     * @return 操作结果 */
    @DeleteMapping("/{productId}")
    public Result<Void> deleteProduct(@PathVariable String productId) {
        productAppService.deleteProduct(productId);
        return Result.success();
    }

    /** 启用商品
     * 
     * @param productId 商品ID
     * @return 更新后的商品 */
    @PostMapping("/{productId}/enable")
    public Result<ProductDTO> enableProduct(@PathVariable String productId) {
        ProductDTO product = productAppService.enableProduct(productId);
        return Result.success(product);
    }

    /** 禁用商品
     * 
     * @param productId 商品ID
     * @return 更新后的商品 */
    @PostMapping("/{productId}/disable")
    public Result<ProductDTO> disableProduct(@PathVariable String productId) {
        ProductDTO product = productAppService.disableProduct(productId);
        return Result.success(product);
    }

    /** 检查商品是否存在
     * 
     * @param productId 商品ID
     * @return 是否存在 */
    @GetMapping("/{productId}/exists")
    public Result<Boolean> existsProduct(@PathVariable String productId) {
        return Result.success(productAppService.existsProduct(productId));
    }

    /** 检查业务标识是否存在
     * 
     * @param type 计费类型
     * @param serviceId 服务ID
     * @return 是否存在 */
    @GetMapping("/business/exists")
    public Result<Boolean> existsByBusinessKey(@RequestParam String type, @RequestParam String serviceId) {
        return Result.success(productAppService.existsByBusinessKey(type, serviceId));
    }
}