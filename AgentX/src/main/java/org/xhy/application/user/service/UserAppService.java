package org.xhy.application.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.xhy.application.user.assembler.UserAssembler;
import org.xhy.application.user.dto.UserDTO;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.service.UserDomainService;
import org.xhy.interfaces.dto.user.request.ChangePasswordRequest;
import org.xhy.interfaces.dto.user.request.QueryUserRequest;
import org.xhy.interfaces.dto.user.request.UserUpdateRequest;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;

@Service
public class UserAppService {

    private final UserDomainService userDomainService;

    public UserAppService(UserDomainService userDomainService) {
        this.userDomainService = userDomainService;
    }

    /** 获取用户信息 */
    public UserDTO getUserInfo(String id) {
        UserEntity userEntity = userDomainService.getUserInfo(id);
        return UserAssembler.toDTO(userEntity);
    }

    /** 修改用户信息 */
    public void updateUserInfo(UserUpdateRequest userUpdateRequest, String userId) {
        UserEntity user = UserAssembler.toEntity(userUpdateRequest, userId);
        userDomainService.updateUserInfo(user);
    }

    /** 修改用户密码
     * 
     * @param request 修改密码请求
     * @param userId 用户ID */
    public void changePassword(ChangePasswordRequest request, String userId) {
        // 验证确认密码
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("新密码和确认密码不一致");
        }

        // 调用领域服务修改密码
        userDomainService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
    }

    /** 分页获取用户列表
     * 
     * @param queryUserRequest 查询条件
     * @return 用户分页数据 */
    public Page<UserDTO> getUsers(QueryUserRequest queryUserRequest) {
        Page<UserEntity> userPage = userDomainService.getUsers(queryUserRequest);

        // 转换为DTO
        List<UserDTO> userDTOList = userPage.getRecords().stream().map(UserAssembler::toDTO).toList();

        // 创建返回的分页对象
        Page<UserDTO> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        resultPage.setRecords(userDTOList);
        return resultPage;
    }
}
