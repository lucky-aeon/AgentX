package org.xhy.domain.memory.constant;

/** 记忆向量的元数据键名常量（与向量存储中的 metadata 对应） */
public interface MemoryMetadataConstant {

    /** 归属用户ID */
    String USER_ID = "USER_ID";

    /** 记忆条目ID（对应 memory_items.id） */
    String ITEM_ID = "ITEM_ID";

    /** 记忆类型：PROFILE/TASK/FACT/EPISODIC */
    String MEMORY_TYPE = "MEMORY_TYPE";

    /** 标签数组 */
    String TAGS = "TAGS";

    /** 状态（可选，1=active） */
    String STATUS = "STATUS";
}
