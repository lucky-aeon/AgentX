package org.xhy.domain.plugins.constant;

/**
 * 插件类型
 */
public enum PluginType {
    SSE("SSE"),
    STDIO("STDIO");

    private final String name;

    PluginType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static PluginType fromName(String name) {
        for (PluginType type : PluginType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }
}
