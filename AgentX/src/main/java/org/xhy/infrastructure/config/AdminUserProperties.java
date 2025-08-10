package org.xhy.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** AgentX管理员用户配置属性
 * 
 * 支持通过环境变量配置管理员和测试用户信息
 * 
 * @author xhy */
@Component
@ConfigurationProperties(prefix = "agentx")
public class AdminUserProperties {

    /** 管理员用户配置 */
    private AdminConfig admin = new AdminConfig();

    /** 测试用户配置 */
    private TestConfig test = new TestConfig();

    public AdminConfig getAdmin() {
        return admin;
    }

    public void setAdmin(AdminConfig admin) {
        this.admin = admin;
    }

    public TestConfig getTest() {
        return test;
    }

    public void setTest(TestConfig test) {
        this.test = test;
    }

    /** 管理员用户配置 */
    public static class AdminConfig {
        /** 管理员邮箱 */
        private String email = "admin@agentx.ai";

        /** 管理员密码 */
        private String password = "admin123";

        /** 管理员昵称 */
        private String nickname = "AgentX管理员";

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }

    /** 测试用户配置 */
    public static class TestConfig {
        /** 是否启用测试用户 */
        private Boolean enabled = true;

        /** 测试用户邮箱 */
        private String email = "test@agentx.ai";

        /** 测试用户密码 */
        private String password = "test123";

        /** 测试用户昵称 */
        private String nickname = "测试用户";

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }
}