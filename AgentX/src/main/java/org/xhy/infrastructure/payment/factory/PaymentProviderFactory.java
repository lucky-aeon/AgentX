package org.xhy.infrastructure.payment.factory;

import org.springframework.stereotype.Component;
import org.xhy.domain.order.constant.PaymentPlatform;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.payment.provider.PaymentProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 支付提供商工厂 */
@Component
public class PaymentProviderFactory {

    /** 支付提供商缓存 */
    private final Map<PaymentPlatform, PaymentProvider> providerCache = new ConcurrentHashMap<>();

    /** 所有支付提供商列表 */
    private final List<PaymentProvider> allProviders;

    public PaymentProviderFactory(List<PaymentProvider> providers) {
        this.allProviders = providers;
        initializeProviders();
    }

    /** 初始化支付提供商 */
    private void initializeProviders() {
        for (PaymentProvider provider : allProviders) {
            if (provider.isConfigured()) {
                providerCache.put(provider.getPaymentPlatform(), provider);
            }
        }
    }

    /** 获取支付提供商
     * 
     * @param paymentPlatform 支付平台
     * @return 支付提供商
     * @throws BusinessException 不支持的支付平台 */
    public PaymentProvider getProvider(PaymentPlatform paymentPlatform) {
        if (paymentPlatform == null) {
            throw new BusinessException("支付平台不能为空");
        }

        PaymentProvider provider = providerCache.get(paymentPlatform);
        if (provider == null) {
            throw new BusinessException("不支持的支付平台: " + paymentPlatform);
        }

        return provider;
    }

    /** 检查支付平台是否可用
     * 
     * @param paymentPlatform 支付平台
     * @return 是否可用 */
    public boolean isAvailable(PaymentPlatform paymentPlatform) {
        if (paymentPlatform == null) {
            return false;
        }
        return providerCache.containsKey(paymentPlatform);
    }

    /** 获取所有可用的支付平台
     * 
     * @return 支付平台列表 */
    public List<PaymentPlatform> getAvailablePaymentPlatforms() {
        return new ArrayList<>(providerCache.keySet());
    }

    /** 获取所有可用的支付提供商
     * 
     * @return 支付提供商列表 */
    public List<PaymentProvider> getAvailableProviders() {
        return providerCache.values().stream().sorted(Comparator.comparing(PaymentProvider::getDisplayInfo))
                .collect(java.util.stream.Collectors.toList());
    }

    /** 检查支付平台是否支持特定功能
     * 
     * @param paymentPlatform 支付平台
     * @param feature 功能名称
     * @return 是否支持 */
    public boolean supportsFeature(PaymentPlatform paymentPlatform, String feature) {
        if (!isAvailable(paymentPlatform)) {
            return false;
        }

        PaymentProvider provider = providerCache.get(paymentPlatform);
        return provider.supportsFeature(feature);
    }

    /** 重新加载支付提供商配置 */
    public void reloadProviders() {
        providerCache.clear();
        initializeProviders();
    }

    /** 手动注册支付提供商
     * 
     * @param provider 支付提供商 */
    public void registerProvider(PaymentProvider provider) {
        if (provider == null) {
            throw new BusinessException("支付提供商不能为空");
        }

        if (!provider.isConfigured()) {
            throw new BusinessException("支付提供商配置不完整: " + provider.getProviderName());
        }

        providerCache.put(provider.getPaymentPlatform(), provider);
    }

    /** 注销支付提供商
     * 
     * @param paymentPlatform 支付平台 */
    public void unregisterProvider(PaymentPlatform paymentPlatform) {
        if (paymentPlatform != null) {
            providerCache.remove(paymentPlatform);
        }
    }

    /** 获取提供商状态信息 */
    public Map<PaymentPlatform, String> getProviderStatus() {
        Map<PaymentPlatform, String> statusMap = new ConcurrentHashMap<>();

        for (PaymentProvider provider : allProviders) {
            PaymentPlatform platform = provider.getPaymentPlatform();
            String status = provider.isConfigured() ? "已配置" : "未配置";
            if (providerCache.containsKey(platform)) {
                status += " (可用)";
            } else {
                status += " (不可用)";
            }
            statusMap.put(platform, status);
        }

        return statusMap;
    }

    /** 获取提供商数量 */
    public int getProviderCount() {
        return providerCache.size();
    }

    /** 检查是否有可用的支付提供商 */
    public boolean hasAvailableProviders() {
        return !providerCache.isEmpty();
    }

    @Override
    public String toString() {
        return "PaymentProviderFactory{" + "availableProviders=" + providerCache.size() + ", totalProviders="
                + allProviders.size() + ", methods=" + providerCache.keySet() + '}';
    }
}