package org.xhy.application.scheduledtask.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.scheduledtask.dto.ScheduledTaskDTO;
import org.xhy.domain.scheduledtask.constant.RepeatType;
import org.xhy.domain.scheduledtask.constant.ScheduleTaskStatus;
import org.xhy.domain.scheduledtask.model.RepeatConfig;
import org.xhy.interfaces.dto.scheduledtask.request.CreateScheduledTaskRequest;
import org.xhy.interfaces.dto.scheduledtask.request.UpdateScheduledTaskRequest;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 定时任务应用服务测试 */
@SpringBootTest
@Transactional
@Rollback(value = false)
public class ScheduledTaskAppServiceTest {

    @Resource
    private ScheduledTaskAppService scheduledTaskAppService;

    // 测试数据
    private static final String TEST_USER_ID = "1fec531705a7bda022cb2cf3650d0d17";
    private static final String TEST_AGENT_ID = "0decdc8e161ef8e2638a136388ab6c40";
    private static final String TEST_SESSION_ID = "68de188585dbb1c6d8e23345c580a80d";

    private CreateScheduledTaskRequest createRequest;
    private String createdTaskId;

    @BeforeEach
    void setUp() {
        // 准备创建任务的请求数据
        createRequest = new CreateScheduledTaskRequest();
        createRequest.setContent("测试定时任务内容");
        createRequest.setAgentId(TEST_AGENT_ID);
        createRequest.setSessionId(TEST_SESSION_ID);
        createRequest.setRepeatType(RepeatType.DAILY);

        // 设置重复配置
        RepeatConfig repeatConfig = new RepeatConfig();
        repeatConfig.setExecuteDateTime(LocalDateTime.now().plusHours(1));
        repeatConfig.setInterval(1);
        repeatConfig.setTimeUnit("DAYS");
        repeatConfig.setExecuteTime("09:00");
        repeatConfig.setEndDateTime(LocalDateTime.now().plusDays(30));
        createRequest.setRepeatConfig(repeatConfig);
    }

    @Test
    void testCreateScheduledTask() {

        RepeatConfig newRepeatConfig = new RepeatConfig();
        newRepeatConfig.setExecuteDateTime(LocalDateTime.now().plusHours(2));
        newRepeatConfig.setInterval(1);
        newRepeatConfig.setTimeUnit("WEEKS");
        newRepeatConfig.setExecuteTime("10:00");
        newRepeatConfig.setWeekdays(List.of(1, 3, 5)); // 周一、三、五
        createRequest.setRepeatConfig(newRepeatConfig);
        // 执行创建
        ScheduledTaskDTO result = scheduledTaskAppService.createScheduledTask(createRequest, TEST_USER_ID);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(TEST_AGENT_ID, result.getAgentId());
        assertEquals(TEST_SESSION_ID, result.getSessionId());
        assertEquals("测试定时任务内容", result.getContent());
        assertEquals(RepeatType.DAILY, result.getRepeatType());
        assertEquals(ScheduleTaskStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getNextExecuteTime());

        // 保存任务ID供后续测试使用
        createdTaskId = result.getId();

        System.out.println("✅ 创建定时任务测试通过，任务ID: " + createdTaskId);
    }

    @Test
    void testGetScheduledTaskList() {
        // 先创建一个任务
        ScheduledTaskDTO created = scheduledTaskAppService.createScheduledTask(createRequest, TEST_USER_ID);

        // 获取任务列表
        List<ScheduledTaskDTO> taskList = scheduledTaskAppService.getUserTasks(TEST_USER_ID);

        // 验证结果
        assertNotNull(taskList);
        assertFalse(taskList.isEmpty());

        // 验证创建的任务在列表中
        boolean found = taskList.stream().anyMatch(task -> task.getId().equals(created.getId()));
        assertTrue(found, "创建的任务应该在列表中");

        System.out.println("✅ 获取定时任务列表测试通过，任务数量: " + taskList.size());
    }

    @Test
    void testGetScheduledTaskDetail() {
        // 先创建一个任务
        ScheduledTaskDTO created = scheduledTaskAppService.createScheduledTask(createRequest, TEST_USER_ID);

        // 获取任务详情
        ScheduledTaskDTO task = scheduledTaskAppService.getTask(created.getId(), TEST_USER_ID);

        // 验证结果
        assertNotNull(task);
        assertEquals(created.getId(), task.getId());
        assertEquals(TEST_USER_ID, task.getUserId());
        assertEquals(TEST_AGENT_ID, task.getAgentId());
        assertEquals(TEST_SESSION_ID, task.getSessionId());
        assertEquals("测试定时任务内容", task.getContent());
        assertEquals(RepeatType.DAILY, task.getRepeatType());
        assertEquals(ScheduleTaskStatus.ACTIVE, task.getStatus());

        System.out.println("✅ 获取定时任务详情测试通过，任务ID: " + task.getId());
    }

    @Test
    void testUpdateScheduledTask() {
        // 先创建一个任务
        ScheduledTaskDTO created = scheduledTaskAppService.createScheduledTask(createRequest, TEST_USER_ID);

        // 准备更新请求
        UpdateScheduledTaskRequest updateRequest = new UpdateScheduledTaskRequest();
        updateRequest.setId(created.getId());
        updateRequest.setContent("修改后的任务内容");
        updateRequest.setRepeatType(RepeatType.WEEKLY);

        // 设置新的重复配置
        RepeatConfig newRepeatConfig = new RepeatConfig();
        newRepeatConfig.setExecuteDateTime(LocalDateTime.now().plusHours(2));
        newRepeatConfig.setInterval(1);
        newRepeatConfig.setTimeUnit("WEEKS");
        newRepeatConfig.setExecuteTime("10:00");
        newRepeatConfig.setWeekdays(List.of(1, 3, 5)); // 周一、三、五
        updateRequest.setRepeatConfig(newRepeatConfig);

        // 执行更新
        ScheduledTaskDTO updated = scheduledTaskAppService.updateScheduledTask(updateRequest, TEST_USER_ID);

        // 验证结果
        assertNotNull(updated);
        assertEquals(created.getId(), updated.getId());
        assertEquals("修改后的任务内容", updated.getContent());
        assertEquals(RepeatType.WEEKLY, updated.getRepeatType());
        assertNotNull(updated.getRepeatConfig());
        assertEquals(List.of(1, 3, 5), updated.getRepeatConfig().getWeekdays());

        System.out.println("✅ 修改定时任务测试通过，任务ID: " + updated.getId());
    }

    @Test
    void testPauseScheduledTask() {
        // 先创建一个任务
        ScheduledTaskDTO created = scheduledTaskAppService.createScheduledTask(createRequest, TEST_USER_ID);

        // 暂停任务
        ScheduledTaskDTO paused = scheduledTaskAppService.pauseTask(created.getId(), TEST_USER_ID);

        // 验证结果
        assertNotNull(paused);
        assertEquals(created.getId(), paused.getId());
        assertEquals(ScheduleTaskStatus.PAUSED, paused.getStatus());

        System.out.println("✅ 暂停定时任务测试通过，任务ID: " + paused.getId());
    }

    @Test
    void testResumeScheduledTask() {
        // 先创建一个任务
        ScheduledTaskDTO created = scheduledTaskAppService.createScheduledTask(createRequest, TEST_USER_ID);

        // 先暂停任务
        scheduledTaskAppService.pauseTask(created.getId(), TEST_USER_ID);

        // 恢复任务
        ScheduledTaskDTO resumed = scheduledTaskAppService.resumeTask(created.getId(), TEST_USER_ID);

        // 验证结果
        assertNotNull(resumed);
        assertEquals(created.getId(), resumed.getId());
        assertEquals(ScheduleTaskStatus.ACTIVE, resumed.getStatus());

        System.out.println("✅ 启动定时任务测试通过，任务ID: " + resumed.getId());
    }

    @Test
    void testDeleteScheduledTask() {
        // 先创建一个任务
        ScheduledTaskDTO created = scheduledTaskAppService.createScheduledTask(createRequest, TEST_USER_ID);
        String taskId = created.getId();

        // 删除任务
        assertDoesNotThrow(() -> {
            scheduledTaskAppService.deleteTask(taskId, TEST_USER_ID);
        });

        // 验证任务已被删除 - 尝试获取应该抛出异常或返回null
        assertThrows(Exception.class, () -> {
            scheduledTaskAppService.getTask(taskId, TEST_USER_ID);
        });

        System.out.println("✅ 删除定时任务测试通过，任务ID: " + taskId);
    }

    @Test
    void testCreateOnceTask() {
        // 创建一次性任务
        CreateScheduledTaskRequest onceRequest = new CreateScheduledTaskRequest();
        onceRequest.setContent("一次性任务内容");
        onceRequest.setAgentId(TEST_AGENT_ID);
        onceRequest.setSessionId(TEST_SESSION_ID);
        onceRequest.setRepeatType(RepeatType.NONE);

        RepeatConfig onceConfig = new RepeatConfig();
        onceConfig.setExecuteDateTime(LocalDateTime.now().plusMinutes(30));
        onceRequest.setRepeatConfig(onceConfig);

        // 执行创建
        ScheduledTaskDTO result = scheduledTaskAppService.createScheduledTask(onceRequest, TEST_USER_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(RepeatType.NONE, result.getRepeatType());
        assertNotNull(result.getRepeatConfig());

        System.out.println("✅ 创建一次性任务测试通过，任务ID: " + result.getId());
    }

    @Test
    void testCreateWorkdaysTask() {
        // 创建工作日重复任务
        CreateScheduledTaskRequest workdaysRequest = new CreateScheduledTaskRequest();
        workdaysRequest.setContent("工作日任务内容");
        workdaysRequest.setAgentId(TEST_AGENT_ID);
        workdaysRequest.setSessionId(TEST_SESSION_ID);
        workdaysRequest.setRepeatType(RepeatType.WORKDAYS);

        RepeatConfig workdaysConfig = new RepeatConfig();
        workdaysConfig.setExecuteDateTime(LocalDateTime.now().plusHours(1));
        workdaysConfig.setExecuteTime("08:30");
        workdaysRequest.setRepeatConfig(workdaysConfig);

        // 执行创建
        ScheduledTaskDTO result = scheduledTaskAppService.createScheduledTask(workdaysRequest, TEST_USER_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(RepeatType.WORKDAYS, result.getRepeatType());
        assertNotNull(result.getRepeatConfig());
        assertEquals("08:30", result.getRepeatConfig().getExecuteTime());

        System.out.println("✅ 创建工作日重复任务测试通过，任务ID: " + result.getId());
    }
}