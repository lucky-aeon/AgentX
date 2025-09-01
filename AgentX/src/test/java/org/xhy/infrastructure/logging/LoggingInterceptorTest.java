package org.xhy.infrastructure.logging;

import org.junit.jupiter.api.Test;
import org.xhy.infrastructure.logging.interceptor.LoggingInterceptor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 日志拦截器测试 */
public class LoggingInterceptorTest {

    @Test
    public void testCompactJson() throws Exception {
        LoggingInterceptor interceptor = new LoggingInterceptor();

        // 使用反射调用私有方法
        Method compactJsonMethod = LoggingInterceptor.class.getDeclaredMethod("compactJson", String.class);
        compactJsonMethod.setAccessible(true);

        // 测试格式化的JSON
        String formattedJson = "{\n  \"name\": \"test\",\n  \"age\": 25,\n  \"address\": {\n    \"city\": \"北京\",\n    \"street\": \"长安街\"\n  }\n}";
        String result = (String) compactJsonMethod.invoke(interceptor, formattedJson);

        // 验证结果是单行JSON
        String expected = "{\"name\":\"test\",\"age\":25,\"address\":{\"city\":\"北京\",\"street\":\"长安街\"}}";
        assertEquals(expected, result);
        System.out.println("原JSON: " + formattedJson.replace("\n", "\\n"));
        System.out.println("压缩后: " + result);
    }

    @Test
    public void testCompactJsonWithInvalidJson() throws Exception {
        LoggingInterceptor interceptor = new LoggingInterceptor();

        // 使用反射调用私有方法
        Method compactJsonMethod = LoggingInterceptor.class.getDeclaredMethod("compactJson", String.class);
        compactJsonMethod.setAccessible(true);

        // 测试无效JSON（会回退到简单的空格替换）
        String invalidJson = "这不是   一个    JSON\n字符串";
        String result = (String) compactJsonMethod.invoke(interceptor, invalidJson);

        // 验证结果是去除多余空格的字符串
        String expected = "这不是 一个 JSON 字符串";
        assertEquals(expected, result);
        System.out.println("原字符串: " + invalidJson.replace("\n", "\\n"));
        System.out.println("处理后: " + result);
    }
}
