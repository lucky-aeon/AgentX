package org.xhy.domain.tool.model.config;

import java.io.Serializable;

/**
 * 参数属性
 */
public class ParameterProperty implements Serializable {
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
} 