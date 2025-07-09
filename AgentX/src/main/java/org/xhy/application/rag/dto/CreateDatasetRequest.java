package org.xhy.application.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建数据集请求
 * @author shilong.zang
 * @date 2024-12-09
 */
public class CreateDatasetRequest {

    /**
     * 数据集名称
     */
    @NotBlank(message = "数据集名称不能为空")
    @Size(max = 100, message = "数据集名称不能超过100个字符")
    private String name;

    /**
     * 数据集图标
     */
    @Size(max = 500, message = "图标URL不能超过500个字符")
    private String icon;

    /**
     * 数据集说明
     */
    @Size(max = 1000, message = "数据集说明不能超过1000个字符")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}