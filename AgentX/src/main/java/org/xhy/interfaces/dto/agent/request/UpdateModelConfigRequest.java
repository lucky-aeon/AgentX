package org.xhy.interfaces.dto.agent.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.xhy.domain.shared.enums.TokenOverflowStrategyEnum;

/** 保存模型配置请求对象 */
public class UpdateModelConfigRequest {

    /** 模型ID */
    @NotBlank(message = "模型ID不能为空")
    private String modelId;

    /** 温度参数，范围0-2 */
    @Min(value = 0, message = "temperature最小值为0")
    @Max(value = 2, message = "temperature最大值为2")
    private Double temperature;

    /** Top P参数，范围0-1 */
    @Min(value = 0, message = "topP最小值为0")
    @Max(value = 1, message = "topP最大值为1")
    private Double topP;

    /** topK */
    private Integer topK;
    /** 最大Token数，适用于滑动窗口和摘要策略 */
    @Min(value = 1, message = "maxTokens最小值为1")
    private Integer maxTokens;

    /** 策略类型 */
    private TokenOverflowStrategyEnum strategyType = TokenOverflowStrategyEnum.NONE;
    /** 预留缓冲比例，适用于滑动窗口策略 范围0-1之间的小数，表示预留的空间比例 */
    private Double reserveRatio;
    /** 摘要触发阈值（消息数量），适用于摘要策略 */
    private Integer summaryThreshold;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public TokenOverflowStrategyEnum getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(TokenOverflowStrategyEnum strategyType) {
        this.strategyType = strategyType;
    }

    public Double getReserveRatio() {
        return reserveRatio;
    }

    public void setReserveRatio(Double reserveRatio) {
        this.reserveRatio = reserveRatio;
    }

    public Integer getSummaryThreshold() {
        return summaryThreshold;
    }

    public void setSummaryThreshold(Integer summaryThreshold) {
        this.summaryThreshold = summaryThreshold;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }
}