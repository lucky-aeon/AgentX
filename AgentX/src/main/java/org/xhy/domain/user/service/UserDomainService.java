package org.xhy.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.langchain4j.service.output.ServiceOutputParser;
import org.springframework.stereotype.Service;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.repository.ToolRepository;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.repository.UserRepository;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.utils.PasswordUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class UserDomainService {

    private final UserRepository userRepository;

    public UserDomainService(UserRepository userRepository) {
        this.userRepository = userRepository;
        
    }

    /** 获取用户信息 */
    public UserEntity getUserInfo(String id) {
        return userRepository.selectById(id);
    }

    /** 根据邮箱或手机号查找用户
     * @param account 邮箱或手机号
     * @return 用户实体，如果不存在则返回null */
    public UserEntity findUserByAccount(String account) {
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getEmail, account)
                .or().eq(UserEntity::getPhone, account);

        return userRepository.selectOne(wrapper);
    }

    /** 根据GitHub ID查找用户
     * @param githubId GitHub ID
     * @return 用户实体，如果不存在则返回null */
    public UserEntity findUserByGithubId(String githubId) {
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getGithubId,
                githubId);
        return userRepository.selectOne(wrapper);
    }

    /** 注册 密码加密存储 */
    public UserEntity register(String email, String phone, String password) {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(email);
        userEntity.setPhone(phone);
        userEntity.setPassword(PasswordUtils.encode(password));
        userEntity.valid();
        checkAccountExist(userEntity.getEmail());

        // 生成昵称
        String nickname = generateNickname();
        userEntity.setNickname(nickname);
        userRepository.checkInsert(userEntity);
        return userEntity;
    }

    /** 加密密码
     * @param password 原始密码
     * @return 加密后的密码 */
    public String encryptPassword(String password) {
        return PasswordUtils.encode(password);
    }

    public UserEntity login(String account, String password) {
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getEmail, account)
                .or().eq(UserEntity::getPhone, account);

        UserEntity userEntity = userRepository.selectOne(wrapper);

        if (userEntity == null || !PasswordUtils.matches(password, userEntity.getPassword())) {
            throw new BusinessException("账号密码错误");
        }
        return userEntity;
    }

    /** 检查账号是否存在，邮箱 or 手机号任意值
     * @param email 邮箱账号*/
    public void checkAccountExist(String email) {
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getEmail, email).or()
            ;
        if (userRepository.exists(wrapper)) {
            throw new BusinessException("账号已存在,不可重复账注册");
        }
    }

    /** 随机生成用户昵称
     * @return 用户昵称 */
    private String generateNickname() {
        return "agent-x" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    public void updateUserInfo(UserEntity user) {
        userRepository.checkedUpdateById(user);
    }

    /** 更新用户密码
     * @param userId 用户ID
     * @param newPassword 新密码 */
    public void updatePassword(String userId, String newPassword) {
        UserEntity user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 加密新密码
        String encodedPassword = PasswordUtils.encode(newPassword);
        user.setPassword(encodedPassword);

        userRepository.checkedUpdateById(user);
    }

    public List<UserEntity> getByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userRepository.selectByIds(userIds);
    }
}
