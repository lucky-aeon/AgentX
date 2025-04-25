package org.xhy;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用入口类
 */
@SpringBootApplication
@EnableFileStorage
public class AgentXApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentXApplication.class, args);
    }
}
