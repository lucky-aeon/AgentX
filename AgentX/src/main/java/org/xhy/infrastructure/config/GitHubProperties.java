package org.xhy.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GitHub配置属性类
 * 用于集中管理所有GitHub相关的配置参数
 */
@Configuration
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {

    private Target target = new Target();

    public static class Target {
        private String username; // 目标仓库的用户名/组织名
        private String repoName; // 目标仓库名称
        private String token; // 访问令牌

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRepoName() {
            return repoName;
        }

        public void setRepoName(String repoName) {
            this.repoName = repoName;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }
}