package org.xhy.domain.memory.model;

/** 记忆类型 */
public enum MemoryType {
    PROFILE,
    TASK,
    FACT,
    EPISODIC;

    public static MemoryType safeOf(String name) {
        if (name == null) {
            return FACT;
        }
        try {
            return MemoryType.valueOf(name.trim().toUpperCase());
        } catch (Exception ignore) {
            return FACT;
        }
    }
}

