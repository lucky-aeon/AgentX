package org.xhy.interfaces.dto.container.response;

/** 容器统计响应 */
public class ContainerStatisticsResponse {

    /** 总容器数 */
    private long totalContainers;

    /** 运行中容器数 */
    private long runningContainers;

    public long getTotalContainers() {
        return totalContainers;
    }

    public void setTotalContainers(long totalContainers) {
        this.totalContainers = totalContainers;
    }

    public long getRunningContainers() {
        return runningContainers;
    }

    public void setRunningContainers(long runningContainers) {
        this.runningContainers = runningContainers;
    }
}