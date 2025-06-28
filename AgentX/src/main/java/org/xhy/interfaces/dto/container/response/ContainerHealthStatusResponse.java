package org.xhy.interfaces.dto.container.response;

import org.xhy.application.container.dto.ContainerDTO;

/** 容器健康状态响应 */
public class ContainerHealthStatusResponse {

    /** 是否健康 */
    private boolean healthy;

    /** 状态消息 */
    private String message;

    /** 容器信息 */
    private ContainerDTO container;

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ContainerDTO getContainer() {
        return container;
    }

    public void setContainer(ContainerDTO container) {
        this.container = container;
    }
}