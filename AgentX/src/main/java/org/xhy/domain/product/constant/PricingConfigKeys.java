package org.xhy.domain.product.constant;

/** 价格配置键值常量 定义ProductEntity中pricingConfig字段的标准键名 */
public final class PricingConfigKeys {

    /** 私有构造函数，防止实例化 */
    private PricingConfigKeys() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ========== 模型Token计费相关 ==========

    /** 输入Token百万级单价 */
    public static final String INPUT_COST_PER_MILLION = "input_cost_per_million";

    /** 输出Token百万级单价 */
    public static final String OUTPUT_COST_PER_MILLION = "output_cost_per_million";

    // ========== 按次计费相关 ==========

    /** 单次使用费用 */
    public static final String COST_PER_UNIT = "cost_per_unit";

    /** 基础费用 */
    public static final String BASE_COST = "base_cost";

    // ========== 按时长计费相关 ==========

    /** 每小时费用 */
    public static final String COST_PER_HOUR = "cost_per_hour";

    /** 每分钟费用 */
    public static final String COST_PER_MINUTE = "cost_per_minute";

    /** 每秒费用 */
    public static final String COST_PER_SECOND = "cost_per_second";

    // ========== 分层计费相关 ==========

    /** 第一层级价格 */
    public static final String TIER_1_COST = "tier_1_cost";

    /** 第一层级阈值 */
    public static final String TIER_1_THRESHOLD = "tier_1_threshold";

    /** 第二层级价格 */
    public static final String TIER_2_COST = "tier_2_cost";

    /** 第二层级阈值 */
    public static final String TIER_2_THRESHOLD = "tier_2_threshold";

    /** 第三层级价格 */
    public static final String TIER_3_COST = "tier_3_cost";

    // ========== 存储计费相关 ==========

    /** 每GB每天费用 */
    public static final String COST_PER_GB_PER_DAY = "cost_per_gb_per_day";

    /** 每MB费用 */
    public static final String COST_PER_MB = "cost_per_mb";
}