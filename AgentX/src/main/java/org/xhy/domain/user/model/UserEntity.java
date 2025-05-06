package org.xhy.domain.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.xhy.infrastructure.entity.BaseEntity;
import org.xhy.infrastructure.exception.BusinessException;

@TableName("users")
public class UserEntity extends BaseEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String nickname;
    private String email;
    private String phone;
    private String password;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void valid() {
        if (StringUtils.isEmpty(email) && StringUtils.isNotEmpty(phone)) {
            throw new BusinessException("必须使用邮箱或者手机号来作为账号");
        }
    }
}
