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


    public void add(UserToolEntity userToolEntity) {
        userToolRepository.checkInsert(userToolEntity);
    }

    public Page<UserToolEntity> listByUserId(String userId, QueryToolRequest queryToolRequest) {
        LambdaQueryWrapper<UserToolEntity> wrapper = Wrappers.<UserToolEntity>lambdaQuery()
                .eq(UserToolEntity::getUserId, userId);
        return userToolRepository.selectPage(new Page<>(queryToolRequest.getPage(), queryToolRequest.getPageSize()), wrapper);
    }


    public UserToolEntity findByToolIdAndUserId(String toolId, String userId) {
        LambdaQueryWrapper<UserToolEntity> wrapper = Wrappers.<UserToolEntity>lambdaQuery()
                .eq(UserToolEntity::getToolId, toolId)
                .eq(UserToolEntity::getUserId, userId);
        return userToolRepository.selectOne(wrapper);
    }


    public void update(UserToolEntity userToolEntity) {
       
        userToolRepository.checkedUpdateById(userToolEntity);
    }


    public void delete(String toolId, String userId) {
        LambdaQueryWrapper<UserToolEntity> wrapper = Wrappers.<UserToolEntity>lambdaQuery()
                .eq(UserToolEntity::getToolId, toolId)
                .eq(UserToolEntity::getUserId, userId);
        userToolRepository.checkedDelete(wrapper);
    }


}
