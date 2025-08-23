package org.xhy.infrastructure.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.container.model.ContainerTemplate;
import org.xhy.infrastructure.exception.BusinessException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Docker容器管理服务 */
@Service
public class DockerService {

    private static final Logger logger = LoggerFactory.getLogger(DockerService.class);
    private DockerClient dockerClient;

    @PostConstruct
    public void init() {
        try {
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost("unix:///var/run/docker.sock").build();

            ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig()).maxConnections(100).connectionTimeout(Duration.ofSeconds(30))
                    .responseTimeout(Duration.ofSeconds(45)).build();

            dockerClient = DockerClientImpl.getInstance(config, httpClient);

            logger.info("Docker客户端初始化成功");
        } catch (Exception e) {
            logger.error("Docker客户端初始化失败", e);
            throw new BusinessException("Docker服务不可用");
        }
    }

    @PreDestroy
    public void destroy() {
        if (dockerClient != null) {
            try {
                dockerClient.close();
                logger.info("Docker客户端已关闭");
            } catch (Exception e) {
                logger.error("关闭Docker客户端失败", e);
            }
        }
    }

    /** 创建并启动容器
     * 
     * @param containerName 容器名称
     * @param template 容器模板
     * @param externalPort 外部端口
     * @param volumePath 数据卷路径
     * @param userId 用户ID
     * @return Docker容器ID */
    public String createAndStartContainer(String containerName, ContainerTemplate template, Integer externalPort,
            String volumePath, String userId) {
        try {
            // 首先拉取镜像
            pullImageIfNotExists(template.getImage());

            // 构建环境变量
            String[] envVars = buildEnvironmentVariables(template, userId);

            // 构建数据卷挂载
            Volume volume = new Volume(template.getVolumeMountPath());
            Bind bind = new Bind(volumePath, volume);

            // 检查网络模式，host模式不需要端口绑定
            boolean isHostNetwork = "host".equals(template.getNetworkMode());

            CreateContainerCmd createCmd = dockerClient.createContainerCmd(template.getImage()).withName(containerName)
                    .withEnv(envVars).withBinds(bind).withRestartPolicy(RestartPolicy.unlessStoppedRestart())
                    .withNetworkMode(template.getNetworkMode());

            HostConfig hostConfig = HostConfig.newHostConfig().withBinds(bind)
                    .withMemory(template.getMemoryLimit() * 1024L * 1024L) // MB to bytes
                    .withCpuQuota(Math.round(template.getCpuLimit() * 100000L)) // CPU限制
                    .withRestartPolicy(RestartPolicy.unlessStoppedRestart()).withNetworkMode(template.getNetworkMode());

            if (!isHostNetwork) {
                // 非host网络模式才需要端口绑定
                ExposedPort exposedPort = ExposedPort.tcp(template.getInternalPort());
                Ports portBindings = new Ports();
                portBindings.bind(exposedPort, Ports.Binding.bindPort(externalPort));

                createCmd.withExposedPorts(exposedPort).withPortBindings(portBindings);
                hostConfig.withPortBindings(portBindings);

                logger.info("容器使用桥接网络模式，端口映射: {}:{}", externalPort, template.getInternalPort());
            } else {
                logger.info("容器使用host网络模式，直接使用宿主机网络");
            }

            // 创建容器
            CreateContainerResponse container = createCmd.withHostConfig(hostConfig).exec();

            String containerId = container.getId();
            logger.info("成功创建容器: {} -> {}", containerName, containerId);

            // 启动容器
            dockerClient.startContainerCmd(containerId).exec();
            logger.info("成功启动容器: {}", containerId);

            return containerId;

        } catch (DockerException e) {
            logger.error("容器创建或启动失败: {}", containerName, e);
            throw new BusinessException("容器创建失败: " + e.getMessage());
        }
    }

    /** 停止容器
     * 
     * @param containerId Docker容器ID */
    public void stopContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).withTimeout(10) // 10秒超时
                    .exec();
            logger.info("成功停止容器: {}", containerId);
        } catch (Exception e) {
            logger.error("容器停止失败: {}", containerId, e);
        }
    }

    /** 启动容器
     * 
     * @param containerId Docker容器ID */
    public void startContainer(String containerId) {
        try {
            // 检查容器当前状态
            String currentStatus = getContainerStatus(containerId);

            if ("running".equalsIgnoreCase(currentStatus)) {
                logger.info("容器已运行，无需启动: {}", containerId);
                return;
            }

            // 只有在容器未运行时才启动
            dockerClient.startContainerCmd(containerId).exec();
            logger.info("成功启动容器: {}", containerId);
        } catch (DockerException e) {
            logger.error("容器启动失败: {}", containerId, e);
            throw new BusinessException("容器启动失败: " + e.getMessage());
        }
    }

    /** 删除容器
     * 
     * @param containerId Docker容器ID
     * @param force 是否强制删除 */
    public void removeContainer(String containerId, boolean force) {
        try {
            if (force) {
                // 先停止容器
                try {
                    stopContainer(containerId);
                } catch (Exception e) {
                    logger.warn("容器停止异常，继续删除: {}", containerId);
                }
            }

            dockerClient.removeContainerCmd(containerId).withForce(force).exec();
            logger.info("成功删除容器: {}", containerId);
        } catch (DockerException e) {
            logger.error("删除容器失败: {}", containerId, e);
        }
    }

    /** 获取容器信息
     * 
     * @param containerId Docker容器ID
     * @return 容器信息 */
    public ContainerInfo getContainerInfo(String containerId) {
        try {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();

            ContainerInfo info = new ContainerInfo();
            info.setContainerId(containerId);
            info.setName(response.getName());
            info.setState(response.getState());
            info.setNetworkSettings(response.getNetworkSettings());

            return info;
        } catch (DockerException e) {
            logger.error("获取容器信息失败: {}", containerId, e);
            throw new BusinessException("获取容器信息失败: " + e.getMessage());
        }
    }

    /** 获取容器状态
     * 
     * @param containerId Docker容器ID
     * @return 容器状态 */
    public String getContainerStatus(String containerId) {
        try {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
            return response.getState().getStatus();
        } catch (DockerException e) {
            logger.error("获取容器状态失败: {}", containerId, e);
            return "unknown";
        }
    }

    /** 检查容器是否存在
     * 
     * @param containerId Docker容器ID
     * @return 是否存在 */
    public boolean containerExists(String containerId) {
        try {
            dockerClient.inspectContainerCmd(containerId).exec();
            return true;
        } catch (DockerException e) {
            return false;
        }
    }

    /** 获取容器统计信息
     * 
     * @param containerId Docker容器ID
     * @return 统计信息 */
    public ContainerStats getContainerStats(String containerId) {
        try {
            // 使用同步方式获取统计信息
            final Statistics[] result = new Statistics[1];
            try {
                dockerClient.statsCmd(containerId).withNoStream(true)
                        .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<Statistics>() {
                            @Override
                            public void onNext(Statistics stats) {
                                result[0] = stats;
                            }
                        }).awaitCompletion();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("获取容器统计信息被中断");
            }

            Statistics stats = result[0];

            ContainerStats containerStats = new ContainerStats();
            containerStats.setContainerId(containerId);

            // 计算CPU使用率
            if (stats.getCpuStats() != null && stats.getPreCpuStats() != null) {
                double cpuUsage = calculateCpuUsage(stats);
                containerStats.setCpuUsage(cpuUsage);
            }

            // 计算内存使用率
            if (stats.getMemoryStats() != null) {
                double memoryUsage = calculateMemoryUsage(stats);
                containerStats.setMemoryUsage(memoryUsage);
            }

            return containerStats;
        } catch (DockerException e) {
            logger.error("获取容器统计信息失败: {}", containerId, e);
            return null;
        }
    }

    /** 拉取镜像（如果不存在） */
    private void pullImageIfNotExists(String image) {
        try {
            // 检查镜像是否存在 提一下：拉取 docker 镜像的时候尽量使用国内源
            List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(image).exec();

            if (images.isEmpty()) {
                logger.info("开始拉取镜像: {}", image);
                try {
                    dockerClient.pullImageCmd(image).exec(new CustomPullImageResultCallback()).awaitCompletion();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException("镜像拉取被中断");
                }
                logger.info("完成镜像拉取: {}", image);
            }
        } catch (Exception e) {
            logger.error("镜像拉取失败: {}", image, e);
            throw new BusinessException("镜像拉取失败: " + e.getMessage());
        }
    }

    /** 构建环境变量数组 */
    private String[] buildEnvironmentVariables(ContainerTemplate template, String userId) {
        Map<String, String> envMap = new HashMap<>();

        // 添加模板中的环境变量
        if (template.getEnvironment() != null) {
            envMap.putAll(template.getEnvironment());
        }

        // 添加用户相关的环境变量
        // 注释掉 USER_ID，可能导致容器退出
        // envMap.put("USER_ID", userId);
        envMap.put("TZ", "Asia/Shanghai");

        return envMap.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
    }

    /** 计算CPU使用率 */
    private double calculateCpuUsage(Statistics stats) {
        CpuStatsConfig cpuStats = stats.getCpuStats();
        CpuStatsConfig preCpuStats = stats.getPreCpuStats();

        if (cpuStats == null || preCpuStats == null) {
            return 0.0;
        }

        Long cpuDelta = cpuStats.getCpuUsage().getTotalUsage() - preCpuStats.getCpuUsage().getTotalUsage();
        Long systemDelta = cpuStats.getSystemCpuUsage() - preCpuStats.getSystemCpuUsage();

        if (systemDelta > 0 && cpuDelta > 0) {
            return (double) cpuDelta / systemDelta * cpuStats.getOnlineCpus() * 100.0;
        }

        return 0.0;
    }

    /** 计算内存使用率 */
    private double calculateMemoryUsage(Statistics stats) {
        MemoryStatsConfig memoryStats = stats.getMemoryStats();
        if (memoryStats == null || memoryStats.getLimit() == null || memoryStats.getUsage() == null) {
            return 0.0;
        }

        return (double) memoryStats.getUsage() / memoryStats.getLimit() * 100.0;
    }

    /** 容器信息类 */
    public static class ContainerInfo {
        private String containerId;
        private String name;
        private InspectContainerResponse.ContainerState state;
        private NetworkSettings networkSettings;

        public String getContainerId() {
            return containerId;
        }

        public void setContainerId(String containerId) {
            this.containerId = containerId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public InspectContainerResponse.ContainerState getState() {
            return state;
        }

        public void setState(InspectContainerResponse.ContainerState state) {
            this.state = state;
        }

        public NetworkSettings getNetworkSettings() {
            return networkSettings;
        }

        public void setNetworkSettings(NetworkSettings networkSettings) {
            this.networkSettings = networkSettings;
        }
    }

    /** 获取容器日志
     * 
     * @param containerId Docker容器ID
     * @param lines 获取日志行数，null表示获取所有
     * @param follow 是否持续跟踪日志
     * @return 日志内容 */
    public String getContainerLogs(String containerId, Integer lines, boolean follow) {
        try {
            StringBuilder logs = new StringBuilder();

            dockerClient.logContainerCmd(containerId).withStdOut(true).withStdErr(true).withTimestamps(true)
                    .withTail(lines).withFollowStream(follow)
                    .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            if (frame != null && frame.getPayload() != null) {
                                logs.append(new String(frame.getPayload()));
                            }
                        }
                    }).awaitCompletion();

            return logs.toString();
        } catch (Exception e) {
            logger.error("获取容器日志失败: {}", containerId, e);
            throw new BusinessException("获取容器日志失败: " + e.getMessage());
        }
    }

    /** 在容器中执行命令
     * 
     * @param containerId Docker容器ID
     * @param command 要执行的命令
     * @return 执行结果 */
    public String executeCommand(String containerId, String[] command) {
        try {
            String execId = dockerClient.execCreateCmd(containerId).withAttachStdout(true).withAttachStderr(true)
                    .withCmd(command).exec().getId();

            StringBuilder output = new StringBuilder();

            dockerClient.execStartCmd(execId).exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame frame) {
                    if (frame != null && frame.getPayload() != null) {
                        output.append(new String(frame.getPayload()));
                    }
                }
            }).awaitCompletion();

            return output.toString();
        } catch (Exception e) {
            logger.error("容器命令执行失败: {} -> {}", containerId, String.join(" ", command), e);
            throw new BusinessException("容器命令执行失败: " + e.getMessage());
        }
    }

    /** 检查容器是否可以执行命令
     * 
     * @param containerId Docker容器ID
     * @return 是否可以执行命令 */
    public boolean canExecuteCommands(String containerId) {
        try {
            String status = getContainerStatus(containerId);
            return "running".equalsIgnoreCase(status);
        } catch (Exception e) {
            logger.warn("检查容器命令执行能力失败: {}", containerId, e);
            return false;
        }
    }

    /** 获取Docker客户端（用于WebTerminal）
     * 
     * @return Docker客户端 */
    public DockerClient getDockerClient() {
        return dockerClient;
    }

    /** 容器统计信息类 */
    public static class ContainerStats {
        private String containerId;
        private Double cpuUsage;
        private Double memoryUsage;

        public String getContainerId() {
            return containerId;
        }

        public void setContainerId(String containerId) {
            this.containerId = containerId;
        }

        public Double getCpuUsage() {
            return cpuUsage;
        }

        public void setCpuUsage(Double cpuUsage) {
            this.cpuUsage = cpuUsage;
        }

        public Double getMemoryUsage() {
            return memoryUsage;
        }

        public void setMemoryUsage(Double memoryUsage) {
            this.memoryUsage = memoryUsage;
        }
    }
}