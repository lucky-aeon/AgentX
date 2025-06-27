package org.xhy.domain.container.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.xhy.domain.container.constant.ContainerStatus;
import org.xhy.domain.container.constant.ContainerType;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

import java.util.List;

/** 容器仓储接口 */
public interface ContainerRepository extends MyBatisPlusExtRepository<ContainerEntity> {

    /** 根据用户ID和容器类型查找容器
     * 
     * @param userId 用户ID
     * @param type 容器类型
     * @return 容器实体 */
    ContainerEntity findByUserIdAndType(String userId, ContainerType type);

    /** 根据Docker容器ID查找容器
     * 
     * @param dockerContainerId Docker容器ID
     * @return 容器实体 */
    ContainerEntity findByDockerContainerId(String dockerContainerId);

    /** 根据外部端口查找容器
     * 
     * @param externalPort 外部端口
     * @return 容器实体 */
    ContainerEntity findByExternalPort(Integer externalPort);

    /** 获取用户的所有容器
     * 
     * @param userId 用户ID
     * @return 容器列表 */
    List<ContainerEntity> findByUserId(String userId);

    /** 获取指定状态的容器列表
     * 
     * @param status 容器状态
     * @return 容器列表 */
    List<ContainerEntity> findByStatus(ContainerStatus status);

    /** 分页查询容器
     * 
     * @param page 分页参数
     * @param keyword 搜索关键词
     * @param status 容器状态
     * @param type 容器类型
     * @return 分页结果 */
    Page<ContainerEntity> selectPageWithConditions(Page<ContainerEntity> page, 
                                                  String keyword, 
                                                  ContainerStatus status, 
                                                  ContainerType type);

    /** 检查端口是否被占用
     * 
     * @param port 端口号
     * @return 是否被占用 */
    boolean isPortOccupied(Integer port);

    /** 获取运行中的容器数量
     * 
     * @return 运行中容器数量 */
    long countRunningContainers();

    /** 获取用户容器数量
     * 
     * @param userId 用户ID
     * @return 容器数量 */
    long countByUserId(String userId);
}