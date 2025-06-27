package org.xhy.application.container.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.container.dto.ContainerDTO;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 容器应用服务集成测试 - 使用真实数据
 */
@SpringBootTest
@ActiveProfiles("test")
class ContainerAppServiceIntegrationTest {

    @Autowired
    private ContainerAppService containerAppService;

    private static final String TEST_USER_ID = "60ab0f71c11d3e7b79dff00639b77e36";

    /**
     * 测试创建用户容器 - 使用真实数据
     */
    @Test
    void testCreateUserContainer_WithRealData() {
        // When: 为用户创建容器
        ContainerDTO result = containerAppService.createUserContainer(TEST_USER_ID);

        // Then: 验证创建结果
        assertNotNull(result, "创建的容器不应为null");
        assertNotNull(result.getId(), "容器ID不应为null");
        assertEquals(TEST_USER_ID, result.getUserId(), "用户ID应匹配");
        assertNotNull(result.getName(), "容器名称不应为null");
        assertTrue(result.getName().contains("mcp-gateway-user"), "容器名称应包含前缀");
        assertTrue(result.getImage().contains("mcp-gateway"), "镜像名称应包含mcp-gateway");
        assertEquals(8080, result.getInternalPort(), "内部端口应为8080");
        assertNotNull(result.getExternalPort(), "外部端口不应为null");
        assertNotNull(result.getVolumePath(), "卷路径不应为null");
        assertTrue(result.getVolumePath().contains(TEST_USER_ID), "卷路径应包含用户ID");
        assertNotNull(result.getStatus(), "容器状态不应为null");
        assertNotNull(result.getType(), "容器类型不应为null");
        assertNotNull(result.getCreatedAt(), "创建时间不应为null");
        
        System.out.println("创建容器成功:");
        System.out.println("- 容器ID: " + result.getId());
        System.out.println("- 容器名称: " + result.getName());
        System.out.println("- 外部端口: " + result.getExternalPort());
        System.out.println("- 卷路径: " + result.getVolumePath());
        System.out.println("- 状态: " + result.getStatus());
        while (true){
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 测试检查用户容器健康状态 - 使用真实数据
     */
    @Test
    void testCheckUserContainerHealth_WithRealData() {
        // Given: 首先创建一个容器
        ContainerDTO createdContainer = containerAppService.createUserContainer(TEST_USER_ID);
        assertNotNull(createdContainer, "预置容器创建失败");

        // When: 检查容器健康状态
        ContainerAppService.ContainerHealthStatus healthStatus = 
            containerAppService.checkUserContainerHealth(TEST_USER_ID);

        // Then: 验证健康检查结果
        assertNotNull(healthStatus, "健康检查结果不应为null");
        assertNotNull(healthStatus.getMessage(), "健康检查消息不应为null");
        assertNotNull(healthStatus.getContainer(), "健康检查返回的容器信息不应为null");
        
        // 验证返回的容器信息
        ContainerDTO containerFromHealth = healthStatus.getContainer();
        assertEquals(createdContainer.getId(), containerFromHealth.getId(), "容器ID应匹配");
        assertEquals(TEST_USER_ID, containerFromHealth.getUserId(), "用户ID应匹配");
        
        System.out.println("容器健康检查结果:");
        System.out.println("- 是否健康: " + healthStatus.isHealthy());
        System.out.println("- 检查消息: " + healthStatus.getMessage());
        System.out.println("- 容器ID: " + containerFromHealth.getId());
        System.out.println("- 容器状态: " + containerFromHealth.getStatus());
        
        // 根据容器状态验证健康性
        if (containerFromHealth.getStatus().name().equals("RUNNING")) {
            assertTrue(healthStatus.isHealthy(), "运行中的容器应该是健康的");
        } else if (containerFromHealth.getStatus().name().equals("CREATING")) {
            // 创建中的容器可能健康也可能不健康，取决于具体实现
            System.out.println("容器正在创建中，状态待确定");
        } else {
            System.out.println("容器状态: " + containerFromHealth.getStatus() + "，健康性: " + healthStatus.isHealthy());
        }
    }

    /**
     * 测试重复创建容器 - 应返回现有容器
     */
    @Test
    void testCreateUserContainer_ShouldReturnExisting() {
        // Given: 首先创建一个容器
        ContainerDTO firstContainer = containerAppService.createUserContainer(TEST_USER_ID);
        assertNotNull(firstContainer, "第一次创建容器失败");

        // When: 再次为同一用户创建容器
        ContainerDTO secondContainer = containerAppService.createUserContainer(TEST_USER_ID);

        // Then: 应返回相同的容器（如果第一个容器是可操作的）
        assertNotNull(secondContainer, "第二次创建容器返回null");
        
        System.out.println("重复创建测试:");
        System.out.println("- 第一次创建ID: " + firstContainer.getId());
        System.out.println("- 第二次创建ID: " + secondContainer.getId());
        System.out.println("- 第一次状态: " + firstContainer.getStatus());
        System.out.println("- 第二次状态: " + secondContainer.getStatus());
        
        // 如果第一个容器是可操作的，应该返回相同的容器
        if (isOperatable(firstContainer.getStatus().name())) {
            assertEquals(firstContainer.getId(), secondContainer.getId(), "应返回相同的容器ID");
            System.out.println("✓ 正确返回了现有的可操作容器");
        } else {
            System.out.println("✓ 第一个容器不可操作，创建了新容器");
        }
    }

    /**
     * 判断容器状态是否可操作
     */
    private boolean isOperatable(String status) {
        return !"DELETING".equals(status) && !"DELETED".equals(status);
    }
}