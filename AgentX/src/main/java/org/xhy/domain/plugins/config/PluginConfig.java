package org.xhy.domain.plugins.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import org.xhy.domain.plugins.constant.PluginType;

/**
 * 插件配置
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "configType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SsePluginConfig.class, name = "SSE"),
    @JsonSubTypes.Type(value = StdioPluginConfig.class, name = "STDIO")
})
public abstract class PluginConfig {

    /**
     * 插件类型
     */
    @NotBlank
    private String configType;
    
    /**
     * 验证配置是否有效
     * @return true 如果配置有效
     */
    public abstract boolean validate();

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    /**
     * 获取无效配置类型的错误消息
     * @param invalidType 无效的配置类型
     * @return 错误消息
     */
    public static String getInvalidTypeMessage(String invalidType) {
        return String.format("无效的配置类型 '%s'，支持的配置类型为: %s", 
            invalidType, getSupportedTypesDescription());
    }

    /**
     * 获取所有支持的配置类型描述
     */
    private static String getSupportedTypesDescription() {
        StringBuilder sb = new StringBuilder("[");
        PluginType[] types = PluginType.values();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(types[i].getName());
        }
        sb.append("]");
        return sb.toString();
    }
}
