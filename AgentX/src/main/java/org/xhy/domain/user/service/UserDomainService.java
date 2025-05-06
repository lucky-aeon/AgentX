package org.xhy.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.repository.UserRepository;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.utils.PasswordUtils;

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

    /** 注册 密码加密存储 */
    public UserEntity register(String email, String phone, String password) {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(email);
        userEntity.setPhone(phone);
        userEntity.setPassword(PasswordUtils.encode(password));
        userEntity.valid();
        checkAccountExist(userEntity.getEmail(), userEntity.getPhone());

        // 生成昵称
        String nickname = generateNickname();
        userEntity.setNickname(nickname);
        userRepository.insert(userEntity);
        return userEntity;
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
     * @param email 邮箱账号
     * @param phone 手机号账号 */
    public void checkAccountExist(String email, String phone) {
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getEmail, email).or()
                .eq(UserEntity::getPhone, phone);
        if (userRepository.exists(wrapper)) {
            throw new BusinessException("账号已存在,不可重复账注册");
        }
    }

    /** 随机生成用户昵称
     * @return 用户昵称 */
    private String generateNickname() {
        return "agent-x" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
