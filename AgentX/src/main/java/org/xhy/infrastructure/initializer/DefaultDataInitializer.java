package org.xhy.infrastructure.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.xhy.domain.user.model.UserEntity;
import org.xhy.domain.user.service.UserDomainService;
import org.xhy.infrastructure.config.AdminUserProperties;
import org.xhy.infrastructure.utils.PasswordUtils;

/** 默认数据初始化器 在应用启动时自动初始化默认用户数据
 * 
 * @author xhy */
@Component
@Order(100) // 确保在其他初始化器之后执行
public class DefaultDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultDataInitializer.class);

    private final UserDomainService userDomainService;
    private final AdminUserProperties adminUserProperties;

    public DefaultDataInitializer(UserDomainService userDomainService, AdminUserProperties adminUserProperties) {
        this.userDomainService = userDomainService;
        this.adminUserProperties = adminUserProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始初始化AgentX默认数据...");

        try {
            initializeDefaultUsers();
            log.info("AgentX默认数据初始化完成！");
        } catch (Exception e) {
            log.error("AgentX默认数据初始化失败", e);
            // 不抛出异常，避免影响应用启动
        }
    }

    /** 初始化默认用户 */
    private void initializeDefaultUsers() {
        log.info("正在初始化默认用户...");

        // 初始化管理员用户
        initializeAdminUser();

        // 初始化测试用户
        initializeTestUser();

        log.info("默认用户初始化完成");
    }

    /** 初始化管理员用户 */
    private void initializeAdminUser() {
        AdminUserProperties.AdminConfig adminConfig = adminUserProperties.getAdmin();
        String adminEmail = adminConfig.getEmail();

        try {
            // 检查管理员用户是否已存在
            UserEntity existingAdmin = userDomainService.findUserByAccount(adminEmail);
            if (existingAdmin != null) {
                log.info("管理员用户已存在，跳过初始化: {}", adminEmail);
                return;
            }

            // 创建管理员用户
            UserEntity adminUser = new UserEntity();
            adminUser.setId("admin-user-uuid-001");
            adminUser.setNickname(adminConfig.getNickname());
            adminUser.setEmail(adminEmail);
            adminUser.setPhone("");
            // 使用项目中的密码加密方法
            adminUser.setPassword(PasswordUtils.encode(adminConfig.getPassword()));
            // 设置为管理员
            adminUser.setIsAdmin(true);

            // 直接插入，绕过业务校验（因为是系统初始化）
            userDomainService.createDefaultUser(adminUser);

            log.info("管理员用户初始化成功: {} (昵称: {})", adminEmail, adminConfig.getNickname());

        } catch (Exception e) {
            log.error("管理员用户初始化失败: {}", adminEmail, e);
        }
    }

    /** 初始化测试用户 */
    private void initializeTestUser() {
        AdminUserProperties.TestConfig testConfig = adminUserProperties.getTest();

        // 检查是否启用测试用户
        if (!testConfig.getEnabled()) {
            log.info("测试用户功能已禁用，跳过初始化");
            return;
        }

        String testEmail = testConfig.getEmail();

        try {
            // 检查测试用户是否已存在
            UserEntity existingTest = userDomainService.findUserByAccount(testEmail);
            if (existingTest != null) {
                log.info("测试用户已存在，跳过初始化: {}", testEmail);
                return;
            }

            // 创建测试用户
            UserEntity testUser = new UserEntity();
            testUser.setId("test-user-uuid-001");
            testUser.setNickname(testConfig.getNickname());
            testUser.setEmail(testEmail);
            testUser.setPhone("");
            // 使用项目中的密码加密方法
            testUser.setPassword(PasswordUtils.encode(testConfig.getPassword()));
            // 设置为普通用户
            testUser.setIsAdmin(false);

            // 直接插入，绕过业务校验（因为是系统初始化）
            userDomainService.createDefaultUser(testUser);

            log.info("测试用户初始化成功: {} (昵称: {})", testEmail, testConfig.getNickname());

        } catch (Exception e) {
            log.error("测试用户初始化失败: {}", testEmail, e);
        }
    }
}