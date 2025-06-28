package org.xhy.application.container.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.container.assembler.ContainerAssembler;
import org.xhy.application.container.dto.ContainerDTO;
import org.xhy.domain.container.constant.ContainerStatus;
import org.xhy.domain.container.constant.ContainerType;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.domain.container.model.ContainerTemplate;
import org.xhy.domain.container.model.ContainerTemplateEntity;
import org.xhy.domain.container.service.ContainerDomainService;
import org.xhy.domain.container.service.ContainerTemplateDomainService;
import org.xhy.domain.user.service.UserDomainService;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.infrastructure.docker.DockerService;
import org.xhy.infrastructure.entity.Operator;
import org.xhy.infrastructure.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/** 容器应用服务 */
@Service
public class ContainerAppService {

    private static final Logger logger = LoggerFactory.getLogger(ContainerAppService.class);
    private static final String USER_VOLUME_BASE_PATH = System.getProperty("user.dir") + "/docker-volumes/users";

    private final ContainerDomainService containerDomainService;
    private final ContainerTemplateDomainService templateDomainService;
    private final UserDomainService userDomainService;
    private final DockerService dockerService;

    public ContainerAppService(ContainerDomainService containerDomainService,
            ContainerTemplateDomainService templateDomainService, UserDomainService userDomainService,
            DockerService dockerService) {
        this.containerDomainService = containerDomainService;
        this.templateDomainService = templateDomainService;
        this.userDomainService = userDomainService;
        this.dockerService = dockerService;
    }

    /** 为用户创建容器
     * 
     * @param userId 用户ID
     * @return 容器信息 */
    @Transactional
    public ContainerDTO createUserContainer(String userId) {

        // 获取MCP网关模板（即用户容器模板）
        ContainerTemplateEntity templateEntity = templateDomainService.getMcpGatewayTemplate();
        ContainerTemplate template = templateEntity.toContainerTemplate();

        // 生成容器名称
        String containerName = "mcp-gateway-user-" + userId.substring(0, 8);

        // 创建用户数据卷目录
        String volumePath = createUserVolumeDirectory(userId);

        // 创建容器实体
        ContainerEntity container = containerDomainService.createUserContainer(userId, containerName,
                template.getImage(), template.getInternalPort(), volumePath);

        // 异步创建Docker容器
        createDockerContainer(container, template);

        return ContainerAssembler.toDTO(container);
    }

    /** 获取用户容器（自动创建和启动）
     * 
     * @param userId 用户ID
     * @return 容器信息，保证返回可用容器 */
    @Transactional
    public ContainerDTO getUserContainer(String userId) {
        ContainerEntity container = containerDomainService.findUserContainer(userId);

        if (container == null) {
            // 1. 容器不存在，自动创建
            logger.info("用户容器不存在，自动创建: userId={}", userId);
            return createUserContainer(userId);
        }

        // 2. 检查容器健康状态
        if (!isContainerHealthy(container)) {
            logger.info("用户容器不健康，尝试启动: userId={}, status={}", userId, container.getStatus());
            try {
                // 启动容器
                startContainer(container.getId());
                // 重新获取最新状态
                container = containerDomainService.findUserContainer(userId);
            } catch (Exception e) {
                logger.error("启动用户容器失败: userId={}, containerId={}", userId, container.getId(), e);
                throw new BusinessException("用户容器启动失败: " + e.getMessage());
            }
        }

        // 3. 更新最后访问时间
        updateContainerLastAccessed(container);

        return ContainerAssembler.toDTO(container);
    }

    /** 检查容器是否健康 */
    private boolean isContainerHealthy(ContainerEntity container) {
        if (container == null) {
            return false;
        }

        // 检查容器状态是否为运行中
        boolean isRunning = ContainerStatus.RUNNING.equals(container.getStatus());

        // 检查必要的网络信息是否存在
        boolean hasNetworkInfo = container.getIpAddress() != null && container.getExternalPort() != null;

        return isRunning && hasNetworkInfo;
    }

    /** 检查用户容器状态
     * 
     * @param userId 用户ID
     * @return 容器状态检查结果 */
    public ContainerHealthStatus checkUserContainerHealth(String userId) {
        ContainerEntity container = containerDomainService.findUserContainer(userId);

        if (container == null) {
            return new ContainerHealthStatus(false, "用户容器不存在", null);
        }

        // 更新最后访问时间
        updateContainerLastAccessed(container);

        if (!container.isOperatable()) {
            return new ContainerHealthStatus(false, "容器状态异常: " + container.getStatus().getDescription(),
                    ContainerAssembler.toDTO(container));
        }

        // 检查Docker容器状态
        if (container.getDockerContainerId() != null) {
            try {
                String dockerStatus = dockerService.getContainerStatus(container.getDockerContainerId());
                if (!"running".equals(dockerStatus)) {
                    return new ContainerHealthStatus(false, "Docker容器未运行: " + dockerStatus,
                            ContainerAssembler.toDTO(container));
                }
            } catch (Exception e) {
                logger.error("检查Docker容器状态失败: {}", container.getDockerContainerId(), e);
                return new ContainerHealthStatus(false, "Docker容器检查失败", ContainerAssembler.toDTO(container));
            }
        }

        return new ContainerHealthStatus(true, "容器健康", ContainerAssembler.toDTO(container));
    }

    /** 启动容器
     * 
     * @param containerId 容器ID */
    @Transactional
    public void startContainer(String containerId) {
        ContainerEntity container = getContainerById(containerId);

        if (container.getDockerContainerId() == null) {
            throw new BusinessException("容器未完成初始化，无法启动");
        }

        try {
            dockerService.startContainer(container.getDockerContainerId());
            containerDomainService.updateContainerStatus(containerId, ContainerStatus.RUNNING, Operator.ADMIN, null);
        } catch (Exception e) {
            logger.error("启动容器失败: {}", containerId, e);
            containerDomainService.markContainerError(containerId, "启动失败: " + e.getMessage(), Operator.ADMIN);
            throw new BusinessException("启动容器失败");
        }
    }

    /** 停止容器
     * 
     * @param containerId 容器ID */
    @Transactional
    public void stopContainer(String containerId) {
        ContainerEntity container = getContainerById(containerId);

        if (container.getDockerContainerId() == null) {
            throw new BusinessException("容器未完成初始化，无法停止");
        }

        try {
            dockerService.stopContainer(container.getDockerContainerId());
            containerDomainService.updateContainerStatus(containerId, ContainerStatus.STOPPED, Operator.ADMIN, null);
        } catch (Exception e) {
            logger.error("停止容器失败: {}", containerId, e);
            containerDomainService.markContainerError(containerId, "停止失败: " + e.getMessage(), Operator.ADMIN);
            throw new BusinessException("停止容器失败");
        }
    }

    /** 删除容器
     * 
     * @param containerId 容器ID */
    @Transactional
    public void deleteContainer(String containerId) {
        ContainerEntity container = getContainerById(containerId);

        try {
            // 删除Docker容器
            if (container.getDockerContainerId() != null) {
                dockerService.removeContainer(container.getDockerContainerId(), true);
            }

            // 删除用户数据卷目录（仅审核容器）
            if (ContainerType.REVIEW.equals(container.getType()) && container.getVolumePath() != null) {
                deleteVolumeDirectory(container.getVolumePath());
            }

            // 物理删除容器记录
            containerDomainService.physicalDeleteContainer(containerId);

        } catch (Exception e) {
            logger.error("删除容器失败: {}", containerId, e);
            containerDomainService.markContainerError(containerId, "删除失败: " + e.getMessage(), Operator.ADMIN);
            throw new BusinessException("删除容器失败");
        }
    }

    /** 分页查询容器
     * 
     * @param page 分页参数
     * @param keyword 搜索关键词
     * @param status 容器状态
     * @param type 容器类型
     * @return 分页结果 */
    public Page<ContainerDTO> getContainersPage(Page<ContainerEntity> page, String keyword, ContainerStatus status,
            ContainerType type) {
        Page<ContainerEntity> entityPage = containerDomainService.getContainersPage(page, keyword, status, type);

        // 转换为DTO并添加用户昵称信息
        Page<ContainerDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());

        List<ContainerDTO> dtoList = entityPage.getRecords().stream().map(entity -> {
            ContainerDTO dto = ContainerAssembler.toDTO(entity);
            // 获取用户昵称
            if (entity.getUserId() != null) {
                try {
                    UserEntity user = userDomainService.getUserInfo(entity.getUserId());
                    if (user != null && user.getNickname() != null) {
                        dto.setUserNickname(user.getNickname());
                    }
                } catch (Exception e) {
                    logger.warn("获取用户昵称失败, userId: {}", entity.getUserId(), e);
                }
            }
            return dto;
        }).collect(java.util.stream.Collectors.toList());

        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    /** 获取容器统计信息
     * 
     * @return 统计信息 */
    public ContainerDomainService.ContainerStatistics getStatistics() {
        return containerDomainService.getStatistics();
    }

    /** 获取容器日志
     * 
     * @param containerId 容器ID
     * @param lines 获取日志行数（可选）
     * @return 日志内容 */
    public String getContainerLogs(String containerId, Integer lines) {
        ContainerEntity container = getContainerById(containerId);

        if (container.getDockerContainerId() == null) {
            throw new BusinessException("容器未完成初始化，无法获取日志");
        }

        try {
            // 默认获取最后100行日志
            Integer logLines = lines != null ? lines : 100;
            return dockerService.getContainerLogs(container.getDockerContainerId(), logLines, false);
        } catch (Exception e) {
            logger.error("获取容器日志失败: {}", containerId, e);
            throw new BusinessException("获取容器日志失败: " + e.getMessage());
        }
    }

    /** 在容器中执行命令
     * 
     * @param containerId 容器ID
     * @param command 要执行的命令
     * @return 执行结果 */
    public String executeContainerCommand(String containerId, String command) {
        ContainerEntity container = getContainerById(containerId);

        if (container.getDockerContainerId() == null) {
            throw new BusinessException("容器未完成初始化，无法执行命令");
        }

        // 检查容器是否可以执行命令
        if (!dockerService.canExecuteCommands(container.getDockerContainerId())) {
            throw new BusinessException("容器未运行，无法执行命令");
        }

        try {
            // 解析命令字符串为数组
            String[] commandArray = parseCommand(command);
            return dockerService.executeCommand(container.getDockerContainerId(), commandArray);
        } catch (Exception e) {
            logger.error("容器命令执行失败: {} -> {}", containerId, command, e);
            throw new BusinessException("命令执行失败: " + e.getMessage());
        }
    }

    /** 获取容器的系统信息
     * 
     * @param containerId 容器ID
     * @return 系统信息 */
    public String getContainerSystemInfo(String containerId) {
        return executeContainerCommand(containerId, "uname -a && whoami && pwd && ls -la");
    }

    /** 获取容器的进程信息
     * 
     * @param containerId 容器ID
     * @return 进程信息 */
    public String getContainerProcessInfo(String containerId) {
        return executeContainerCommand(containerId, "ps aux");
    }

    /** 获取容器的网络信息
     * 
     * @param containerId 容器ID
     * @return 网络信息 */
    public String getContainerNetworkInfo(String containerId) {
        return executeContainerCommand(containerId, "netstat -tuln");
    }

    /** 检查容器内MCP网关状态
     * 
     * @param containerId 容器ID
     * @return MCP网关状态 */
    public String checkMcpGatewayStatus(String containerId) {
        try {
            // 检查MCP网关进程
            String processCheck = executeContainerCommand(containerId, "ps aux | grep -v grep | grep mcp");

            // 检查端口占用
            String portCheck = executeContainerCommand(containerId, "netstat -tuln | grep :8080");

            // 检查服务健康状态
            String healthCheck = executeContainerCommand(containerId,
                    "curl -s http://localhost:8080/health || echo 'Health check failed'");

            return "=== MCP网关进程 ===\n" + processCheck + "\n\n=== 端口占用 ===\n" + portCheck + "\n\n=== 健康检查 ===\n"
                    + healthCheck;

        } catch (Exception e) {
            logger.error("检查MCP网关状态失败: {}", containerId, e);
            return "检查MCP网关状态失败: " + e.getMessage();
        }
    }

    /** 异步创建Docker容器 */
    private void createDockerContainer(ContainerEntity container, ContainerTemplate template) {
        try {
            String dockerContainerId = dockerService.createAndStartContainer(container.getName(), template,
                    container.getExternalPort(), container.getVolumePath(), container.getUserId());

            // 获取容器IP地址
            DockerService.ContainerInfo containerInfo = dockerService.getContainerInfo(dockerContainerId);
            String ipAddress = extractIpAddress(containerInfo);

            // 更新容器状态
            containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.RUNNING, Operator.ADMIN,
                    dockerContainerId);
            containerDomainService.updateContainerIpAddress(container.getId(), ipAddress, Operator.ADMIN);

            logger.info("容器创建成功: {} -> {}", container.getName(), dockerContainerId);

        } catch (Exception e) {
            logger.error("容器创建失败: {}", container.getName(), e);
            containerDomainService.markContainerError(container.getId(), e.getMessage(), Operator.ADMIN);
        }
    }

    /** 创建用户数据卷目录 */
    private String createUserVolumeDirectory(String userId) {
        String volumePath = USER_VOLUME_BASE_PATH + "/" + userId;
        File directory = new File(volumePath);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                logger.warn("无法创建用户数据目录: {}，尝试使用临时目录", volumePath);
                // 如果无法创建指定目录，使用临时目录
                String tempVolumePath = System.getProperty("java.io.tmpdir") + "/agentx-docker-volumes/users/" + userId;
                File tempDirectory = new File(tempVolumePath);
                if (!tempDirectory.exists()) {
                    boolean tempCreated = tempDirectory.mkdirs();
                    if (!tempCreated) {
                        throw new BusinessException("创建用户数据目录失败: " + tempVolumePath);
                    }
                }
                return tempVolumePath;
            }
        }

        return volumePath;
    }

    /** 删除数据卷目录 */
    private void deleteVolumeDirectory(String volumePath) {
        try {
            File directory = new File(volumePath);
            if (directory.exists()) {
                deleteRecursively(directory);
            }
        } catch (Exception e) {
            logger.error("删除数据卷目录失败: {}", volumePath, e);
        }
    }

    /** 递归删除目录 */
    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    /** 提取容器IP地址 */
    private String extractIpAddress(DockerService.ContainerInfo containerInfo) {
        if (containerInfo.getNetworkSettings() != null && containerInfo.getNetworkSettings().getNetworks() != null) {

            return containerInfo.getNetworkSettings().getNetworks().values().stream().findFirst()
                    .map(network -> network.getIpAddress()).orElse(null);
        }
        return null;
    }

    /** 根据ID获取容器 */
    private ContainerEntity getContainerById(String containerId) {
        return containerDomainService.getContainerById(containerId);
    }

    /** 解析命令字符串为数组
     * 
     * @param command 命令字符串
     * @return 命令数组 */
    private String[] parseCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            throw new BusinessException("命令不能为空");
        }

        // 简单的命令解析，支持基本的shell命令
        // 对于复杂命令，可以使用更完善的解析器
        command = command.trim();

        // 如果命令包含管道、重定向等，使用sh -c执行
        if (command.contains("|") || command.contains("&&") || command.contains("||") || command.contains(">")
                || command.contains("<")) {
            return new String[]{"sh", "-c", command};
        }

        // 简单命令直接分割
        return command.split("\\s+");
    }

    /** 更新容器最后访问时间
     * 
     * @param container 容器实体 */
    @Transactional
    public void updateContainerLastAccessed(ContainerEntity container) {
        try {
            // 如果容器是暂停状态，先恢复运行
            if (container.isSuspended()) {
                resumeSuspendedContainer(container);
            }

            // 更新最后访问时间
            containerDomainService.updateContainerLastAccessed(container.getId());
            logger.debug("更新容器最后访问时间: {}", container.getName());

        } catch (Exception e) {
            logger.warn("更新容器访问时间失败: {}", container.getName(), e);
        }
    }

    /** 恢复暂停的容器
     * 
     * @param container 暂停的容器 */
    private void resumeSuspendedContainer(ContainerEntity container) {
        try {
            logger.info("恢复暂停的容器: {}", container.getName());

            // 启动Docker容器
            if (container.getDockerContainerId() != null) {
                dockerService.startContainer(container.getDockerContainerId());
            }

            // 更新容器状态为运行中
            containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.RUNNING, Operator.ADMIN,
                    null);

            logger.info("容器恢复成功: {}", container.getName());

        } catch (Exception e) {
            logger.error("恢复容器失败: {}", container.getName(), e);
            containerDomainService.markContainerError(container.getId(), "恢复失败: " + e.getMessage(), Operator.ADMIN);
            throw new BusinessException("容器恢复失败");
        }
    }

    /** 容器健康状态检查结果 */
    public static class ContainerHealthStatus {
        private final boolean healthy;
        private final String message;
        private final ContainerDTO container;

        public ContainerHealthStatus(boolean healthy, String message, ContainerDTO container) {
            this.healthy = healthy;
            this.message = message;
            this.container = container;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getMessage() {
            return message;
        }

        public ContainerDTO getContainer() {
            return container;
        }
    }
}