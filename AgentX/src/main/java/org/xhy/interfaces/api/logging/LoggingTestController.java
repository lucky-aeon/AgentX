package org.xhy.interfaces.api.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.interfaces.api.common.Result;

import java.util.Map;

/** 日志测试控制器 用于演示统一JSON日志格式化输出功能 */
@RestController
@RequestMapping("/test/logging")
public class LoggingTestController {

    private static final Logger logger = LoggerFactory.getLogger(LoggingTestController.class);

    /** GET请求测试 */
    @GetMapping("/get")
    public Result<String> testGet(@RequestParam(required = false) String message) {
        logger.info("处理GET请求，参数: {}", message);
        return Result.success("GET请求处理成功，消息: " + message);
    }

    /** POST请求测试 */
    @PostMapping("/post")
    public Result<String> testPost(@RequestBody Map<String, Object> data) {
        logger.info("处理POST请求，数据: {}", data);
        return Result.success("POST请求处理成功");
    }

    /** 异常测试 */
    @GetMapping("/error")
    public Result<String> testError() {
        logger.error("模拟异常情况");
        throw new RuntimeException("这是一个测试异常");
    }

    /** 慢请求测试 */
    @GetMapping("/slow")
    public Result<String> testSlow() throws InterruptedException {
        logger.info("开始处理慢请求");
        Thread.sleep(2000); // 模拟耗时操作
        logger.info("慢请求处理完成");
        return Result.success("慢请求处理成功");
    }
}
