package org.xhy.domain.tool.model.config;

import java.util.Map;
import java.util.Objects;

/** 工具定义 */
public class ToolDefinition {
    /** 工具名称 */
    private String name;

    /** 工具描述 */
    private String description;

    /** 参数定义 */
    private Map<String, Object> parameters;

    /** 是否启用 */
    private Boolean enabled;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    <function>\n");
        sb.append("      <name>").append(name).append("</name>\n");
        sb.append("      <description>").append(description).append("</description>\n");
        
        if (parameters != null && parameters.containsKey("properties")) {
            Object propsObject = parameters.get("properties");
            if (propsObject instanceof Map) {
                Map<String, Object> properties = (Map<String, Object>) propsObject;
                if (!properties.isEmpty()) {
                    sb.append("      <parameters>\n");
                    properties.forEach((paramName, paramDetails) -> {
                        if (paramDetails instanceof Map) {
                            Map<String, String> detailsMap = (Map<String, String>) paramDetails;
                            sb.append("        <parameter>\n");
                            sb.append("          <name>").append(paramName).append("</name>\n");
                            sb.append("          <type>").append(detailsMap.get("type")).append("</type>\n");
                            sb.append("          <description>").append(detailsMap.get("description")).append("</description>\n");
                            sb.append("        </parameter>\n");
                        }
                    });
                    sb.append("      </parameters>\n");
                }
            }
        }
        
        sb.append("    </function>\n");
        return sb.toString();
    }
}