package org.xhy.domain.user.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.xhy.domain.user.model.UserEntity;

@SpringBootTest
public class UserDomainServiceTest {

    @Autowired
    private UserDomainService userDomainService;

    @Test
    public void testRegisterWithPhone() {
        // 准备测试数据
        String phone = "";
        String email = "xhyovo@qq.com";
        String password = "123456";

        // 执行注册方法
        UserEntity result = userDomainService.register(email, phone, password);

        // 验证结果
        assertNotNull(result);
        assertEquals(phone, result.getPhone());
        assertNotNull(result.getPassword());
        assertNotEquals(password, result.getPassword()); // 验证密码已加密

        System.out.println("注册成功！用户ID: " + result.getId());
    }
}
