package org.xhy.interfaces.dto.container.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.xhy.domain.container.constant.ContainerType;

/** 创建容器请求 */
public class CreateContainerRequest {

    /** 容器名称 */
    @NotBlank(message = "容器名称不能为空")
    private String name;

    /** 容器类型 */
    @NotNull(message = "容器类型不能为空")
    private ContainerType type;

    /** 容器镜像 */
    private String image;

    /** 内部端口 */
    private Integer internalPort;

    /** 描述信息 */
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ContainerType getType() {
        return type;
    }

    public void setType(ContainerType type) {
        this.type = type;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getInternalPort() {
        return internalPort;
    }

    public void setInternalPort(Integer internalPort) {
        this.internalPort = internalPort;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}