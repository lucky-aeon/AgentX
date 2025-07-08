package org.xhy.infrastructure.highavailability.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;

/** 高可用初始化器 在应用启动时初始化高可用项目和同步模型
 * 
 * @author xhy
 * @since 1.0.0 */
@Component
public class HighAvailabilityInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(HighAvailabilityInitializer.class);

    private final HighAvailabilityDomainService highAvailabilityDomainService;

    public HighAvailabilityInitializer(HighAvailabilityDomainService highAvailabilityDomainService) {
        this.highAvailabilityDomainService = highAvailabilityDomainService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("开始高可用系统初始化...");

        try {
            // 1. 初始化项目
            highAvailabilityDomainService.initializeProject();

            // 2. 批量同步现有模型
            highAvailabilityDomainService.syncAllModelsToGateway();

            logger.info("高可用系统初始化完成");

        } catch (Exception e) {
            logger.error("高可用系统初始化失败", e);
            // 初始化失败不阻止应用启动
        }
    }
}