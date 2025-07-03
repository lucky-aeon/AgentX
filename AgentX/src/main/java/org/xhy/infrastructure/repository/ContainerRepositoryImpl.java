package org.xhy.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import org.xhy.domain.container.constant.ContainerStatus;
import org.xhy.domain.container.constant.ContainerType;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.domain.container.repository.ContainerRepository;

import java.util.List;

/** 容器仓储实现 */
@Mapper
@Repository
public interface ContainerRepositoryImpl extends ContainerRepository {

    @Override
    default ContainerEntity findByUserIdAndType(String userId, ContainerType type) {
        LambdaQueryWrapper<ContainerEntity> wrapper = Wrappers.<ContainerEntity>lambdaQuery()
                .eq(ContainerEntity::getUserId, userId).eq(ContainerEntity::getType, type)
                .orderByDesc(ContainerEntity::getCreatedAt);

        return selectOne(wrapper);
    }

    @Override
    default ContainerEntity findByDockerContainerId(String dockerContainerId) {
        LambdaQueryWrapper<ContainerEntity> wrapper = Wrappers.<ContainerEntity>lambdaQuery()
                .eq(ContainerEntity::getDockerContainerId, dockerContainerId);

        return selectOne(wrapper);
    }

    @Override
    default ContainerEntity findByExternalPort(Integer externalPort) {
        LambdaQueryWrapper<ContainerEntity> wrapper = Wrappers.<ContainerEntity>lambdaQuery()
                .eq(ContainerEntity::getExternalPort, externalPort);

        return selectOne(wrapper);
    }

    @Override
    default List<ContainerEntity> findByUserId(String userId) {
        LambdaQueryWrapper<ContainerEntity> wrapper = Wrappers.<ContainerEntity>lambdaQuery()
                .eq(ContainerEntity::getUserId, userId).orderByDesc(ContainerEntity::getCreatedAt);

        return selectList(wrapper);
    }

    @Override
    default List<ContainerEntity> findByStatus(ContainerStatus status) {
        LambdaQueryWrapper<ContainerEntity> wrapper = Wrappers.<ContainerEntity>lambdaQuery()
                .eq(ContainerEntity::getStatus, status);

        return selectList(wrapper);
    }

    @Override
    default Page<ContainerEntity> selectPageWithConditions(Page<ContainerEntity> page, String keyword,
            ContainerStatus status, ContainerType type) {
        LambdaQueryWrapper<ContainerEntity> wrapper = Wrappers.<ContainerEntity>lambdaQuery();

        // 关键词搜索：容器名称、用户ID、Docker容器ID
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(ContainerEntity::getName, keyword).or().like(ContainerEntity::getUserId, keyword)
                    .or().like(ContainerEntity::getDockerContainerId, keyword));
        }

        // 状态过滤
        if (status != null) {
            wrapper.eq(ContainerEntity::getStatus, status);
        }

        // 类型过滤
        if (type != null) {
            wrapper.eq(ContainerEntity::getType, type);
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(ContainerEntity::getCreatedAt);

        return selectPage(page, wrapper);
    }

    @Override
    default boolean isPortOccupied(Integer port) {
        LambdaQueryWrapper<ContainerEntity> wrapper = Wrappers.<ContainerEntity>lambdaQuery()
                .eq(ContainerEntity::getExternalPort, port).ne(ContainerEntity::getStatus, ContainerStatus.DELETED);

        return selectCount(wrapper) > 0;
    }

    @Override
    @Select("SELECT COUNT(*) FROM user_containers WHERE status = 2")
    long countRunningContainers();

    default long countByStatus(ContainerStatus status) {
        LambdaQueryWrapper<ContainerEntity> wrapper = Wrappers.<ContainerEntity>lambdaQuery()
                .eq(ContainerEntity::getStatus, status);
        return selectCount(wrapper);
    }

    @Override
    default long countByUserId(String userId) {
        LambdaQueryWrapper<ContainerEntity> wrapper = Wrappers.<ContainerEntity>lambdaQuery()
                .eq(ContainerEntity::getUserId, userId);

        return selectCount(wrapper);
    }
}