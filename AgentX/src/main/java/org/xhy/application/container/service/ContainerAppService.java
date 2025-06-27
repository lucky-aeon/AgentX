package org.xhy.application.container.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.container.assembler.ContainerAssembler;
import org.xhy.application.container.dto.ContainerDTO;
import org.xhy.interfaces.dto.container.request.CreateContainerRequest;
import org.xhy.domain.container.constant.ContainerStatus;
import org.xhy.domain.container.constant.ContainerType;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.domain.container.model.ContainerTemplate;
import org.xhy.domain.container.model.ContainerTemplateEntity;
import org.xhy.domain.container.service.ContainerDomainService;
import org.xhy.domain.container.service.ContainerTemplateDomainService;
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
    private final DockerService dockerService;

    public ContainerAppService(ContainerDomainService containerDomainService, 
                             ContainerTemplateDomainService templateDomainService,
                             DockerService dockerService) {
        this.containerDomainService = containerDomainService;
        this.templateDomainService = templateDomainService;
        this.dockerService = dockerService;
    }

    /** 为用户创建容器
     * 
     * @param userId 用户ID
     * @return 容器信息 */
    @Transactional
    public ContainerDTO createUserContainer(String userId) {
        // 检查用户是否已有容器
        ContainerEntity existingContainer = containerDomainService.getUserContainer(userId);
        if (existingContainer != null && existingContainer.isOperatable()) {
            return ContainerAssembler.toDTO(existingContainer);
        }

        // 获取MCP网关模板（即用户容器模板）
        ContainerTemplateEntity templateEntity = templateDomainService.getMcpGatewayTemplate();
        ContainerTemplate template = templateEntity.toContainerTemplate();
        
        // 生成容器名称
        String containerName = "mcp-gateway-user-" + userId.substring(0, 8);
        
        // 创建用户数据卷目录
        String volumePath = createUserVolumeDirectory(userId);
        
        // 创建容器实体
        ContainerEntity container = containerDomainService.createUserContainer(
                userId, containerName, template.getImage(), template.getInternalPort(), volumePath);

        // 异步创建Docker容器
        createDockerContainerAsync(container, template);

        return ContainerAssembler.toDTO(container);
    }

    /** 创建审核容器
     * 
     * @param request 创建请求
     * @param userId 操作用户ID
     * @return 容器信息 */
    @Transactional
    public ContainerDTO createReviewContainer(CreateContainerRequest request, String userId) {
        // 使用MCP网关模板
        ContainerTemplateEntity templateEntity = templateDomainService.getMcpGatewayTemplate();
        ContainerTemplate template = templateEntity.toContainerTemplate();
        
        // 生成容器名称
        String containerName = "mcp-gateway-review-" + System.currentTimeMillis();
        
        // 创建审核容器实体
        ContainerEntity container = containerDomainService.createReviewContainer(
                userId, containerName, template.getImage(), template.getInternalPort());

        // 异步创建Docker容器
        createDockerContainerAsync(container, template);

        return ContainerAssembler.toDTO(container);
    }

    /** 使用指定模板为用户创建容器
     * 
     * @param userId 用户ID
     * @param templateId 模板ID
     * @return 容器信息 */
    @Transactional
    public ContainerDTO createUserContainerWithTemplate(String userId, String templateId) {
        // 检查用户是否已有容器
        ContainerEntity existingContainer = containerDomainService.getUserContainer(userId);
        if (existingContainer != null && existingContainer.isOperatable()) {
            return ContainerAssembler.toDTO(existingContainer);
        }

        // 获取指定模板
        ContainerTemplateEntity templateEntity = templateDomainService.getTemplateById(templateId);
        ContainerTemplate template = templateEntity.toContainerTemplate();
        
        // 生成容器名称
        String containerName = "mcp-gateway-user-" + userId.substring(0, 8);
        
        // 创建用户数据卷目录
        String volumePath = createUserVolumeDirectory(userId);
        
        // 创建容器实体
        ContainerEntity container = containerDomainService.createUserContainer(
                userId, containerName, template.getImage(), template.getInternalPort(), volumePath);

        // 异步创建Docker容器
        createDockerContainerAsync(container, template);

        return ContainerAssembler.toDTO(container);
    }

    /** 使用指定模板创建审核容器
     * 
     * @param request 创建请求
     * @param userId 操作用户ID
     * @param templateId 模板ID
     * @return 容器信息 */
    @Transactional
    public ContainerDTO createReviewContainerWithTemplate(CreateContainerRequest request, String userId, String templateId) {
        // 获取指定模板
        ContainerTemplateEntity templateEntity = templateDomainService.getTemplateById(templateId);
        ContainerTemplate template = templateEntity.toContainerTemplate();
        
        // 生成容器名称
        String containerName = "mcp-gateway-review-" + System.currentTimeMillis();
        
        // 创建审核容器实体
        ContainerEntity container = containerDomainService.createReviewContainer(
                userId, containerName, template.getImage(), template.getInternalPort());

        // 异步创建Docker容器
        createDockerContainerAsync(container, template);

        return ContainerAssembler.toDTO(container);
    }

    /** 获取用户容器
     * 
     * @param userId 用户ID
     * @return 容器信息，可能为null */
    public ContainerDTO getUserContainer(String userId) {
        ContainerEntity container = containerDomainService.getUserContainer(userId);
        return container != null ? ContainerAssembler.toDTO(container) : null;
    }

    /** 检查用户容器状态
     * 
     * @param userId 用户ID
     * @return 容器状态检查结果 */
    public ContainerHealthStatus checkUserContainerHealth(String userId) {
        ContainerEntity container = containerDomainService.getUserContainer(userId);
        
        if (container == null) {
            return new ContainerHealthStatus(false, "用户容器不存在", null);
        }

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
                return new ContainerHealthStatus(false, "Docker容器检查失败", 
                                               ContainerAssembler.toDTO(container));
            }
        }

        return new ContainerHealthStatus(true, "容器健康", ContainerAssembler.toDTO(container));
    }

    /** 启动容器
     * 
     * @param containerId 容器ID
     * @param operator 操作者 */
    @Transactional
    public void startContainer(String containerId, Operator operator) {
        ContainerEntity container = getContainerById(containerId);
        
        if (container.getDockerContainerId() == null) {
            throw new BusinessException("容器未完成初始化，无法启动");
        }

        try {
            dockerService.startContainer(container.getDockerContainerId());
            containerDomainService.updateContainerStatus(containerId, ContainerStatus.RUNNING, operator, null);
        } catch (Exception e) {
            logger.error("启动容器失败: {}", containerId, e);
            containerDomainService.markContainerError(containerId, "启动失败: " + e.getMessage(), operator);
            throw new BusinessException("启动容器失败");
        }
    }

    /** 停止容器
     * 
     * @param containerId 容器ID
     * @param operator 操作者 */
    @Transactional
    public void stopContainer(String containerId, Operator operator) {
        ContainerEntity container = getContainerById(containerId);
        
        if (container.getDockerContainerId() == null) {
            throw new BusinessException("容器未完成初始化，无法停止");
        }

        try {
            dockerService.stopContainer(container.getDockerContainerId());
            containerDomainService.updateContainerStatus(containerId, ContainerStatus.STOPPED, operator, null);
        } catch (Exception e) {
            logger.error("停止容器失败: {}", containerId, e);
            containerDomainService.markContainerError(containerId, "停止失败: " + e.getMessage(), operator);
            throw new BusinessException("停止容器失败");
        }
    }

    /** 删除容器
     * 
     * @param containerId 容器ID
     * @param operator 操作者 */
    @Transactional
    public void deleteContainer(String containerId, Operator operator) {
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
            containerDomainService.markContainerError(containerId, "删除失败: " + e.getMessage(), operator);
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
    public Page<ContainerDTO> getContainersPage(Page<ContainerEntity> page, String keyword, 
                                               ContainerStatus status, ContainerType type) {
        Page<ContainerEntity> entityPage = containerDomainService.getContainersPage(page, keyword, status, type);
        return ContainerAssembler.toDTOPage(entityPage);
    }

    /** 获取容器统计信息
     * 
     * @return 统计信息 */
    public ContainerDomainService.ContainerStatistics getStatistics() {
        return containerDomainService.getStatistics();
    }

    /** 异步创建Docker容器 */
    private void createDockerContainerAsync(ContainerEntity container, ContainerTemplate template) {
        // 在实际应用中，这里应该使用异步任务队列
        // 为了简化，这里使用新线程模拟异步处理
        new Thread(() -> {
            try {
                String dockerContainerId = dockerService.createAndStartContainer(
                        container.getName(), template, container.getExternalPort(), 
                        container.getVolumePath(), container.getUserId());

                // 获取容器IP地址
                DockerService.ContainerInfo containerInfo = dockerService.getContainerInfo(dockerContainerId);
                String ipAddress = extractIpAddress(containerInfo);

                // 更新容器状态
                containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.RUNNING, 
                                                           Operator.ADMIN, dockerContainerId);
                containerDomainService.updateContainerIpAddress(container.getId(), ipAddress, Operator.ADMIN);

                logger.info("容器创建成功: {} -> {}", container.getName(), dockerContainerId);

            } catch (Exception e) {
                logger.error("容器创建失败: {}", container.getName(), e);
                containerDomainService.markContainerError(container.getId(), e.getMessage(), Operator.ADMIN);
            }
        }).start();
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
        if (containerInfo.getNetworkSettings() != null && 
            containerInfo.getNetworkSettings().getNetworks() != null) {
            
            return containerInfo.getNetworkSettings().getNetworks().values().stream()
                    .findFirst()
                    .map(network -> network.getIpAddress())
                    .orElse(null);
        }
        return null;
    }

    /** 根据ID获取容器 */
    private ContainerEntity getContainerById(String containerId) {
        return containerDomainService.getContainerById(containerId);
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