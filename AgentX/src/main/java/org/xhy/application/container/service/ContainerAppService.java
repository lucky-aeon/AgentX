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
    private static final String USER_VOLUME_BASE_PATH = System.getProperty("user.dir") + "/data/users";

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
        if (userId == null || userId.length() < 8) {
            throw new BusinessException("用户ID无效，无法创建用户容器");
        }
        String containerName = "mcp-gateway-user-" + userId.substring(0, 8);

        // 创建用户数据卷目录
        String volumePath = createUserVolumeDirectory(userId);

        // 创建容器实体
        ContainerEntity container = containerDomainService.createUserContainer(userId, containerName,
                template.getImage(), template.getInternalPort(), volumePath);

        // 创建Docker容器
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

        // 2. 检查容器健康状态并智能恢复
        ContainerHealthCheckResult healthResult = checkContainerHealth(container);
        if (!healthResult.isHealthy()) {
            logger.info("用户容器不健康，尝试智能恢复: userId={}, issue={}", userId, healthResult.getMessage());
            try {
                ContainerRecoveryResult recoveryResult = recoverUserContainer(container, userId);
                if (!recoveryResult.isSuccess()) {
                    throw new BusinessException("用户容器恢复失败: " + recoveryResult.getMessage());
                }

                // 重新获取最新状态
                container = containerDomainService.findUserContainer(userId);
                logger.info("用户容器恢复成功: userId={}, result={}", userId, recoveryResult.getMessage());
            } catch (Exception e) {
                logger.error("用户容器智能恢复失败: userId={}, containerId={}", userId, container.getId(), e);
                throw new BusinessException("用户容器恢复失败: " + e.getMessage());
            }
        }

        // 3. 更新最后访问时间
        updateContainerLastAccessed(container);

        return ContainerAssembler.toDTO(container);
    }

    /** 检查容器是否健康（增强版：包含实际Docker状态检查）
     * 
     * @param container 容器实体
     * @return 健康检查结果详情 */
    private ContainerHealthCheckResult checkContainerHealth(ContainerEntity container) {
        if (container == null) {
            return new ContainerHealthCheckResult(false, "容器实体为空", null);
        }

        // 1. 检查数据库状态是否为运行中
        boolean dbStatusRunning = ContainerStatus.RUNNING.equals(container.getStatus());
        if (!dbStatusRunning) {
            return new ContainerHealthCheckResult(false, "容器数据库状态异常: " + container.getStatus().getDescription(),
                    container.getStatus().name());
        }

        // 2. 检查必要的网络信息是否存在
        boolean hasNetworkInfo = container.getIpAddress() != null && container.getExternalPort() != null;
        if (!hasNetworkInfo) {
            return new ContainerHealthCheckResult(false,
                    "容器缺少网络信息: ip=" + container.getIpAddress() + ", port=" + container.getExternalPort(),
                    "MISSING_NETWORK_INFO");
        }

        // 3. 检查Docker容器ID是否存在
        if (container.getDockerContainerId() == null) {
            return new ContainerHealthCheckResult(false, "容器缺少Docker容器ID", "MISSING_DOCKER_ID");
        }

        // 4. 检查实际Docker容器状态
        DockerService.ContainerActualStatus actualStatus = dockerService
                .getContainerActualStatus(container.getDockerContainerId());

        if (!actualStatus.exists()) {
            return new ContainerHealthCheckResult(false, "Docker容器不存在: " + actualStatus.getMessage(),
                    "DOCKER_CONTAINER_NOT_EXISTS");
        }

        if (!actualStatus.isRunning()) {
            return new ContainerHealthCheckResult(false, "Docker容器未运行: " + actualStatus.getStatus(),
                    "DOCKER_CONTAINER_NOT_RUNNING");
        }

        // 5. 检查网络连通性（可选，避免过于频繁的网络检查）
        boolean networkAccessible = dockerService.isContainerNetworkAccessible(container.getIpAddress(),
                container.getExternalPort());
        if (!networkAccessible) {
            logger.warn("容器网络连通性检查失败，但Docker容器运行正常: containerId={}, ip={}, port={}", container.getId(),
                    container.getIpAddress(), container.getExternalPort());
            // 网络连通性问题不一定意味着容器不健康，可能是暂时的网络问题
        }

        return new ContainerHealthCheckResult(true, "容器健康", "HEALTHY");
    }

    /** 检查容器是否健康（简化版，兼容现有代码） */
    private boolean isContainerHealthy(ContainerEntity container) {
        ContainerHealthCheckResult result = checkContainerHealth(container);
        return result.isHealthy();
    }

    /** 检查用户容器状态（增强版健康检查）
     * 
     * @param userId 用户ID
     * @return 容器状态检查结果 */
    public ContainerHealthStatus checkUserContainerHealth(String userId) {
        try {
            ContainerEntity container = containerDomainService.findUserContainer(userId);

            if (container == null) {
                logger.warn("用户容器不存在: userId={}", userId);
                return new ContainerHealthStatus(false, "用户容器不存在", null);
            }

            // 更新最后访问时间
            updateContainerLastAccessed(container);

            if (!container.isOperatable()) {
                logger.warn("容器状态不可操作: userId={}, status={}", userId, container.getStatus());
                return new ContainerHealthStatus(false, "容器状态异常: " + container.getStatus().getDescription(),
                        ContainerAssembler.toDTO(container));
            }

            // 使用增强的健康检查机制
            ContainerHealthCheckResult healthResult = checkContainerHealth(container);
            if (!healthResult.isHealthy()) {
                logger.warn("容器健康检查失败: userId={}, issue={}", userId, healthResult.getMessage());
                return new ContainerHealthStatus(false, healthResult.getMessage(), ContainerAssembler.toDTO(container));
            }

            logger.debug("用户容器健康检查通过: userId={}", userId);
            return new ContainerHealthStatus(true, "容器健康", ContainerAssembler.toDTO(container));

        } catch (Exception e) {
            logger.error("用户容器健康检查异常: userId={}", userId, e);
            return new ContainerHealthStatus(false, "健康检查失败: " + e.getMessage(), null);
        }
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

    /** 创建Docker容器 */
    private void createDockerContainer(ContainerEntity container, ContainerTemplate template) {
        try {
            String dockerContainerId = dockerService.createAndStartContainer(container.getName(), template,
                    container.getExternalPort(), container.getVolumePath(), container.getUserId());

            // 获取容器IP地址
            DockerService.ContainerInfo containerInfo = dockerService.getContainerInfo(dockerContainerId);
            String ipAddress = extractIpAddress(containerInfo, template.getNetworkMode());

            // 对于host网络模式，外部端口就是内部端口
            // TODO: 需要在ContainerDomainService中添加updateContainerExternalPort方法
            if ("host".equals(template.getNetworkMode())) {
                logger.info("host网络模式，容器外部端口应为内部端口: {}", template.getInternalPort());
            }

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
                String tempVolumePath = System.getProperty("java.io.tmpdir") + "/data/users/" + userId;
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

    /** 从容器信息中提取IP地址
     * 
     * @param containerInfo 容器信息
     * @param networkMode 网络模式
     * @return IP地址 */
    private String extractIpAddress(DockerService.ContainerInfo containerInfo, String networkMode) {
        // host网络模式直接返回127.0.0.1
        if ("host".equals(networkMode)) {
            logger.info("容器使用host网络模式，IP地址设为127.0.0.1");
            return "127.0.0.1";
        }

        // bridge网络模式返回127.0.0.1，通过端口映射访问
        if ("bridge".equals(networkMode)) {
            logger.info("容器使用bridge网络模式，IP地址设为127.0.0.1（通过端口映射访问）");
            return "127.0.0.1";
        }

        // 其他特殊网络模式从容器网络设置中提取IP
        if (containerInfo.getNetworkSettings() != null && containerInfo.getNetworkSettings().getNetworks() != null) {
            String ipAddress = containerInfo.getNetworkSettings().getNetworks().values().stream().findFirst()
                    .map(network -> network.getIpAddress()).orElse(null);

            logger.info("容器使用{}网络模式，提取容器内网IP地址: {}", networkMode, ipAddress);
            return ipAddress;
        }

        logger.warn("无法从容器网络设置中提取IP地址，网络模式: {}", networkMode);
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

    /** 获取或创建审核容器
     * 
     * @return 审核容器信息，保证返回可用的审核容器 */
    @Transactional
    public ContainerDTO getOrCreateReviewContainer() {
        // 查找现有的审核容器
        ContainerEntity reviewContainer = containerDomainService.findReviewContainer();

        if (reviewContainer == null) {
            // 审核容器不存在，创建新的
            logger.info("审核容器不存在，自动创建");
            ContainerDTO createdContainer = createReviewContainer();

            // 等待容器创建完成并获取网络信息
            return waitForContainerReady(createdContainer.getId());
        }

        // 检查审核容器健康状态并智能恢复
        ContainerHealthCheckResult healthResult = checkContainerHealth(reviewContainer);
        if (!healthResult.isHealthy()) {
            logger.info("审核容器不健康，尝试智能恢复: issue={}", healthResult.getMessage());
            try {
                ContainerRecoveryResult recoveryResult = recoverReviewContainer(reviewContainer);
                if (!recoveryResult.isSuccess()) {
                    throw new BusinessException("审核容器恢复失败: " + recoveryResult.getMessage());
                }

                // 重新获取最新状态
                reviewContainer = containerDomainService.getContainerById(reviewContainer.getId());
                logger.info("审核容器恢复成功: result={}", recoveryResult.getMessage());
            } catch (Exception e) {
                logger.error("审核容器智能恢复失败: containerId={}", reviewContainer.getId(), e);
                throw new BusinessException("审核容器恢复失败: " + e.getMessage());
            }
        }

        return ContainerAssembler.toDTO(reviewContainer);
    }

    /** 创建审核容器
     * 
     * @return 审核容器信息 */
    @Transactional
    public ContainerDTO createReviewContainer() {
        // 获取审核容器模板
        ContainerTemplateEntity templateEntity = templateDomainService.getReviewContainerTemplate();
        ContainerTemplate template = templateEntity.toContainerTemplate();

        // 生成审核容器名称
        String containerName = "mcp-gateway-review-system";

        // 创建审核容器数据卷目录
        String volumePath = createReviewVolumeDirectory();

        // 创建审核容器实体
        ContainerEntity container = containerDomainService.createReviewContainer(containerName, template.getImage(),
                template.getInternalPort(), volumePath);

        // 创建Docker容器
        createDockerContainer(container, template);

        return ContainerAssembler.toDTO(container);
    }

    /** 创建审核容器数据卷目录 */
    private String createReviewVolumeDirectory() {
        String volumePath = USER_VOLUME_BASE_PATH + "/review-system";
        File directory = new File(volumePath);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                logger.warn("无法创建审核容器数据目录: {}，尝试使用临时目录", volumePath);
                // 如果无法创建指定目录，使用临时目录
                String tempVolumePath = System.getProperty("java.io.tmpdir") + "/data/review-system";
                File tempDirectory = new File(tempVolumePath);
                if (!tempDirectory.exists()) {
                    boolean tempCreated = tempDirectory.mkdirs();
                    if (!tempCreated) {
                        throw new BusinessException("创建审核容器数据目录失败: " + tempVolumePath);
                    }
                }
                return tempVolumePath;
            }
        }

        return volumePath;
    }

    /** 从模板创建容器
     * 
     * @param templateId 模板ID
     * @return 创建的容器信息 */
    @Transactional
    public ContainerDTO createContainerFromTemplate(String templateId) {
        // 获取容器模板
        ContainerTemplateEntity templateEntity = templateDomainService.getTemplateById(templateId);
        ContainerTemplate template = templateEntity.toContainerTemplate();

        // 根据模板类型创建不同类型的容器
        if (ContainerType.REVIEW.equals(templateEntity.getType())) {
            // 审核容器
            return createReviewContainerFromTemplate(template);
        } else {
            throw new BusinessException("暂不支持从此类型模板创建容器: " + templateEntity.getType());
        }
    }

    /** 从模板创建审核容器
     * 
     * @param template 容器模板
     * @return 创建的容器信息 */
    private ContainerDTO createReviewContainerFromTemplate(ContainerTemplate template) {
        // 生成审核容器名称
        String containerName = "mcp-gateway-review-" + System.currentTimeMillis();

        // 创建审核容器数据卷目录
        String volumePath = createReviewVolumeDirectory();

        // 创建审核容器实体
        ContainerEntity container = containerDomainService.createReviewContainer(containerName, template.getImage(),
                template.getInternalPort(), volumePath);

        // 异步创建Docker容器
        createDockerContainer(container, template);

        return ContainerAssembler.toDTO(container);
    }

    /** 等待容器准备就绪（包含网络信息）
     * 
     * @param containerId 容器ID
     * @return 包含完整网络信息的容器DTO
     * @throws BusinessException 如果等待超时或容器创建失败 */
    private ContainerDTO waitForContainerReady(String containerId) {
        int maxRetries = 30; // 最多等待30秒
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Thread.sleep(1000); // 等待1秒

                ContainerEntity container = containerDomainService.getContainerById(containerId);

                // 检查容器是否处于错误状态
                if (ContainerStatus.ERROR.equals(container.getStatus())) {
                    logger.error("容器创建失败: containerId={}, status={}", containerId, container.getStatus());
                    throw new BusinessException("容器创建失败，请检查Docker环境");
                }

                // 检查容器是否已经有完整的网络信息
                if (ContainerStatus.RUNNING.equals(container.getStatus()) && container.getIpAddress() != null
                        && container.getExternalPort() != null) {
                    logger.info("容器准备就绪: containerId={}, ip={}, port={}", containerId, container.getIpAddress(),
                            container.getExternalPort());
                    return ContainerAssembler.toDTO(container);
                }

                retryCount++;
                logger.debug("等待容器准备就绪: containerId={}, retry={}/{}, status={}, ip={}", containerId, retryCount,
                        maxRetries, container.getStatus(), container.getIpAddress());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("等待容器准备被中断");
            }
        }

        // 超时后尝试获取最新状态
        ContainerEntity container = containerDomainService.getContainerById(containerId);
        logger.warn("容器等待超时，当前状态: containerId={}, status={}, ip={}, port={}", containerId, container.getStatus(),
                container.getIpAddress(), container.getExternalPort());

        throw new BusinessException("容器创建超时，请稍后重试或检查Docker环境");
    }

    /** 智能容器恢复方法
     * 
     * @param container 需要恢复的容器
     * @param userId 用户ID（用于用户容器）
     * @return 恢复结果 */
    private ContainerRecoveryResult recoverUserContainer(ContainerEntity container, String userId) {
        logger.info("开始智能恢复用户容器: userId={}, containerId={}", userId, container.getId());

        // 1. 如果dockerContainerId为null，尝试查找同名容器
        if (container.getDockerContainerId() == null) {
            return recoverContainerWithoutDockerID(container, userId);
        }

        // 2. dockerContainerId存在，检查容器实际状态
        DockerService.ContainerRecoveryResult recoveryResult = dockerService
                .forceStartContainerIfExists(container.getDockerContainerId());

        if (recoveryResult.isSuccess()) {
            // Docker容器启动成功，更新数据库状态
            containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.RUNNING, Operator.ADMIN,
                    null);
            return new ContainerRecoveryResult(true, "DOCKER_CONTAINER_STARTED", "Docker容器启动成功");
        }

        // 3. 如果Docker容器不存在，尝试重新创建
        if ("CONTAINER_NOT_EXISTS".equals(recoveryResult.getResultCode())) {
            logger.info("Docker容器不存在，重新创建: userId={}, containerId={}", userId, container.getId());
            return recreateUserContainer(container, userId);
        }

        // 4. 其他错误
        return new ContainerRecoveryResult(false, recoveryResult.getResultCode(), recoveryResult.getMessage());
    }

    /** 恢复没有Docker容器ID的容器记录 */
    private ContainerRecoveryResult recoverContainerWithoutDockerID(ContainerEntity container, String userId) {
        logger.info("容器记录存在但Docker容器ID为空，检查是否存在同名容器: userId={}", userId);

        // 1. 先检查Docker中是否存在同名容器
        String existingContainerId = dockerService.findContainerByName(container.getName());

        if (existingContainerId != null) {
            logger.info("发现现有Docker容器，复用: userId={}, dockerId={}", userId, existingContainerId);
            return reuseExistingDockerContainer(container, existingContainerId);
        } else {
            logger.info("未找到现有容器，创建新的Docker容器: userId={}", userId);
            return recreateUserContainer(container, userId);
        }
    }

    /** 复用现有Docker容器 */
    private ContainerRecoveryResult reuseExistingDockerContainer(ContainerEntity container,
            String existingContainerId) {
        try {
            // 1. 容器存在，检查状态并启动
            DockerService.ContainerRecoveryResult startResult = dockerService
                    .forceStartContainerIfExists(existingContainerId);
            if (!startResult.isSuccess()) {
                return new ContainerRecoveryResult(false, startResult.getResultCode(),
                        "启动现有容器失败: " + startResult.getMessage());
            }

            // 2. 获取容器模板以提取IP地址
            ContainerTemplateEntity templateEntity = templateDomainService.getMcpGatewayTemplate();
            ContainerTemplate template = templateEntity.toContainerTemplate();

            // 3. 更新数据库中的容器ID和状态
            containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.RUNNING, Operator.ADMIN,
                    existingContainerId);

            // 4. 更新IP地址
            DockerService.ContainerInfo containerInfo = dockerService.getContainerInfo(existingContainerId);
            String ipAddress = extractIpAddress(containerInfo, template.getNetworkMode());
            containerDomainService.updateContainerIpAddress(container.getId(), ipAddress, Operator.ADMIN);

            logger.info("成功复用现有Docker容器: dockerId={}, ip={}", existingContainerId, ipAddress);
            return new ContainerRecoveryResult(true, "REUSED_EXISTING_CONTAINER", "复用现有容器成功");

        } catch (Exception e) {
            logger.error("复用现有容器失败: dockerId={}", existingContainerId, e);
            return new ContainerRecoveryResult(false, "REUSE_FAILED", "复用现有容器失败: " + e.getMessage());
        }
    }

    /** 重新创建用户容器 */
    private ContainerRecoveryResult recreateUserContainer(ContainerEntity container, String userId) {
        try {
            // 1. 获取容器模板
            ContainerTemplateEntity templateEntity = templateDomainService.getMcpGatewayTemplate();
            ContainerTemplate template = templateEntity.toContainerTemplate();

            // 2. 创建新Docker容器
            String dockerContainerId = dockerService.createAndStartContainer(container.getName(), template,
                    container.getExternalPort(), container.getVolumePath(), userId);

            // 3. 获取容器IP
            DockerService.ContainerInfo containerInfo = dockerService.getContainerInfo(dockerContainerId);
            String ipAddress = extractIpAddress(containerInfo, template.getNetworkMode());

            // 4. 更新数据库状态
            containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.RUNNING, Operator.ADMIN,
                    dockerContainerId);
            containerDomainService.updateContainerIpAddress(container.getId(), ipAddress, Operator.ADMIN);

            logger.info("Docker容器重新创建成功: dockerId={}", dockerContainerId);
            return new ContainerRecoveryResult(true, "CONTAINER_RECREATED", "容器重新创建成功");

        } catch (Exception e) {
            logger.error("重新创建容器失败", e);
            return new ContainerRecoveryResult(false, "RECREATION_FAILED", "重新创建容器失败: " + e.getMessage());
        }
    }

    /** 智能恢复审核容器 */
    private ContainerRecoveryResult recoverReviewContainer(ContainerEntity container) {
        logger.info("开始智能恢复审核容器: containerId={}", container.getId());

        // 1. 如果dockerContainerId为null，尝试查找同名容器
        if (container.getDockerContainerId() == null) {
            return recoverReviewContainerWithoutDockerID(container);
        }

        // 2. dockerContainerId存在，检查容器实际状态
        DockerService.ContainerRecoveryResult recoveryResult = dockerService
                .forceStartContainerIfExists(container.getDockerContainerId());

        if (recoveryResult.isSuccess()) {
            // Docker容器启动成功，更新数据库状态
            containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.RUNNING, Operator.ADMIN,
                    null);
            return new ContainerRecoveryResult(true, "DOCKER_CONTAINER_STARTED", "Docker容器启动成功");
        }

        // 3. 如果Docker容器不存在，尝试重新创建
        if ("CONTAINER_NOT_EXISTS".equals(recoveryResult.getResultCode())) {
            logger.info("Docker容器不存在，重新创建审核容器: containerId={}", container.getId());
            return recreateReviewContainer(container);
        }

        // 4. 其他错误
        return new ContainerRecoveryResult(false, recoveryResult.getResultCode(), recoveryResult.getMessage());
    }

    /** 恢复没有Docker容器ID的审核容器记录 */
    private ContainerRecoveryResult recoverReviewContainerWithoutDockerID(ContainerEntity container) {
        logger.info("审核容器记录存在但Docker容器ID为空，检查是否存在同名容器");

        // 1. 先检查Docker中是否存在同名容器
        String existingContainerId = dockerService.findContainerByName(container.getName());

        if (existingContainerId != null) {
            logger.info("发现现有Docker容器，复用: dockerId={}", existingContainerId);
            return reuseExistingReviewDockerContainer(container, existingContainerId);
        } else {
            logger.info("未找到现有审核容器，创建新的Docker容器");
            return recreateReviewContainer(container);
        }
    }

    /** 复用现有审核Docker容器 */
    private ContainerRecoveryResult reuseExistingReviewDockerContainer(ContainerEntity container,
            String existingContainerId) {
        try {
            // 1. 容器存在，检查状态并启动
            DockerService.ContainerRecoveryResult startResult = dockerService
                    .forceStartContainerIfExists(existingContainerId);
            if (!startResult.isSuccess()) {
                return new ContainerRecoveryResult(false, startResult.getResultCode(),
                        "启动现有审核容器失败: " + startResult.getMessage());
            }

            // 2. 获取容器模板以提取IP地址
            ContainerTemplateEntity templateEntity = templateDomainService.getReviewContainerTemplate();
            ContainerTemplate template = templateEntity.toContainerTemplate();

            // 3. 更新数据库中的容器ID和状态
            containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.RUNNING, Operator.ADMIN,
                    existingContainerId);

            // 4. 更新IP地址
            DockerService.ContainerInfo containerInfo = dockerService.getContainerInfo(existingContainerId);
            String ipAddress = extractIpAddress(containerInfo, template.getNetworkMode());
            containerDomainService.updateContainerIpAddress(container.getId(), ipAddress, Operator.ADMIN);

            logger.info("成功复用现有审核Docker容器: dockerId={}, ip={}", existingContainerId, ipAddress);
            return new ContainerRecoveryResult(true, "REUSED_EXISTING_REVIEW_CONTAINER", "复用现有审核容器成功");

        } catch (Exception e) {
            logger.error("复用现有审核容器失败: dockerId={}", existingContainerId, e);
            return new ContainerRecoveryResult(false, "REUSE_REVIEW_FAILED", "复用现有审核容器失败: " + e.getMessage());
        }
    }

    /** 重新创建审核容器 */
    private ContainerRecoveryResult recreateReviewContainer(ContainerEntity container) {
        try {
            // 1. 获取容器模板
            ContainerTemplateEntity templateEntity = templateDomainService.getReviewContainerTemplate();
            ContainerTemplate template = templateEntity.toContainerTemplate();

            // 2. 创建新Docker容器（审核容器不需要userId）
            String dockerContainerId = dockerService.createAndStartContainer(container.getName(), template,
                    container.getExternalPort(), container.getVolumePath(), null // 审核容器没有特定用户
            );

            // 3. 获取容器IP
            DockerService.ContainerInfo containerInfo = dockerService.getContainerInfo(dockerContainerId);
            String ipAddress = extractIpAddress(containerInfo, template.getNetworkMode());

            // 4. 更新数据库状态
            containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.RUNNING, Operator.ADMIN,
                    dockerContainerId);
            containerDomainService.updateContainerIpAddress(container.getId(), ipAddress, Operator.ADMIN);

            logger.info("审核容器重新创建成功: dockerId={}", dockerContainerId);
            return new ContainerRecoveryResult(true, "REVIEW_CONTAINER_RECREATED", "审核容器重新创建成功");

        } catch (Exception e) {
            logger.error("重新创建审核容器失败", e);
            return new ContainerRecoveryResult(false, "REVIEW_RECREATION_FAILED", "重新创建审核容器失败: " + e.getMessage());
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

    /** 容器健康检查详细结果 */
    private static class ContainerHealthCheckResult {
        private final boolean healthy;
        private final String message;
        private final String issueCode;

        public ContainerHealthCheckResult(boolean healthy, String message, String issueCode) {
            this.healthy = healthy;
            this.message = message;
            this.issueCode = issueCode;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getMessage() {
            return message;
        }

        public String getIssueCode() {
            return issueCode;
        }
    }

    /** 容器恢复结果 */
    private static class ContainerRecoveryResult {
        private final boolean success;
        private final String resultCode;
        private final String message;

        public ContainerRecoveryResult(boolean success, String resultCode, String message) {
            this.success = success;
            this.resultCode = resultCode;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getResultCode() {
            return resultCode;
        }

        public String getMessage() {
            return message;
        }
    }
}