package org.xhy.domain.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.xhy.domain.tool.model.UserToolEntity;
import org.xhy.domain.tool.repository.UserToolRepository;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.dto.tool.request.QueryToolRequest;


/**
 * 用户已安装工具 service
 */
@Service
public class UserToolDomainService {

    private final UserToolRepository userToolRepository;

    public UserToolDomainService(UserToolRepository userToolRepository) {
        this.userToolRepository = userToolRepository;
    }

    public  void checkUserToolExist(String userId, String toolVersionId) {
        LambdaQueryWrapper<UserToolEntity> wrapper = Wrappers.<UserToolEntity>lambdaQuery()
                .eq(UserToolEntity::getUserId, userId)
                .eq(UserToolEntity::getToolVersionId, toolVersionId);
        UserToolEntity userToolEntity = userToolRepository.selectOne(wrapper);
        if (userToolEntity != null) {
            throw new BusinessException("工具已安装");
        }
    }

    public void add(UserToolEntity userToolEntity) {
        userToolRepository.checkInsert(userToolEntity);
    }

    public Page<UserToolEntity> listByUserId(String userId, QueryToolRequest queryToolRequest) {
        LambdaQueryWrapper<UserToolEntity> wrapper = Wrappers.<UserToolEntity>lambdaQuery()
                .eq(UserToolEntity::getUserId, userId);
        return userToolRepository.selectPage(new Page<>(queryToolRequest.getPage(), queryToolRequest.getPageSize()), wrapper);
    }


}
