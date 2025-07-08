package org.xhy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.xhy.infrastructure.utils.PasswordUtils;

/** 应用入口类 */
@SpringBootApplication
@EnableScheduling
public class AgentXApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentXApplication.class, args);
    }
}
