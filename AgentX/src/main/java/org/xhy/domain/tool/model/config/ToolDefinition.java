package org.xhy.domain.tool.model.config;

import java.io.Serializable;

/**
 * 工具定义
 */
public class ToolDefinition implements Serializable {
    private String name;
    private String description;
    private ToolParameter parameters;

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

    public ToolParameter getParameters() {
        return parameters;
    }

    public void setParameters(ToolParameter parameters) {
        this.parameters = parameters;
    }
} 