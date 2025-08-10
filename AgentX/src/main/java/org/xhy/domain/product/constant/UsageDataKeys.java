package org.xhy.domain.product.constant;

/** 用量数据键值常量 定义BillingContext中usageData字段的标准键名 */
public final class UsageDataKeys {

    /** 私有构造函数，防止实例化 */
    private UsageDataKeys() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ========== 模型调用相关 ==========

    /** 输入Token数量 */
    public static final String INPUT_TOKENS = "input";

    /** 输出Token数量 */
    public static final String OUTPUT_TOKENS = "output";

    // ========== 按次计费相关 ==========

    /** 使用数量 */
    public static final String QUANTITY = "quantity";

    /** 调用次数 */
    public static final String CALLS = "calls";

    // ========== 按时长计费相关 ==========

    /** 使用时长（秒） */
    public static final String DURATION_SECONDS = "duration_seconds";

    /** 开始时间戳 */
    public static final String START_TIME = "start_time";

    /** 结束时间戳 */
    public static final String END_TIME = "end_time";

    // ========== 存储相关 ==========

    /** 文件大小（字节） */
    public static final String FILE_SIZE_BYTES = "file_size_bytes";

    /** 存储时长（天） */
    public static final String STORAGE_DAYS = "storage_days";
}