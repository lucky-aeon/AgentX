package org.xhy.infrastructure.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** JSON工具类，用于处理JSON转换 */
public class JsonUtils {

    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        // 注册Java 8时间模块
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 忽略未知属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 只包含非空属性
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /** 将对象转换为JSON字符串
     *
     * @param obj 要转换的对象
     * @return JSON字符串，失败返回"{}" */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return "{}";
        }

        try {
            System.out.println("JsonUtils Debug - toJsonString input: " + obj + " (type: " + obj.getClass() + ")");
            String result = objectMapper.writeValueAsString(obj);
            System.out.println("JsonUtils Debug - toJsonString result: " + result);
            return result;
        } catch (Exception e) {
            log.error("JSON序列化失败: {}, 错误: {}", obj.getClass().getSimpleName(), e.getMessage(), e);
            return "{}";
        }
    }

    /** 将JSON字符串转换为指定对象
     *
     * @param json JSON字符串
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 转换后的对象，失败返回null */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("JSON反序列化失败: {}, 错误: {}", clazz.getSimpleName(), e.getMessage(), e);
            return null;
        }
    }

    /** 将JSON字符串转换为List
     *
     * @param json JSON字符串
     * @param clazz 元素类型
     * @param <T> 泛型类型
     * @return 转换后的List，失败返回空List */
    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.error("JSON数组反序列化失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /** 将JSON字符串转换为Map<String, Object>
     *
     * @param json JSON字符串
     * @return 转换后的Map，失败返回null */
    public static Map<String, Object> parseMap(String json) {
        if (json == null || json.isEmpty()) {
            System.out.println("JsonUtils Debug - parseMap input is null or empty");
            return null;
        }

        try {
            System.out.println("JsonUtils Debug - parseMap input: " + json);
            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            System.out.println("JsonUtils Debug - parseMap result: " + result);
            return result;
        } catch (Exception e) {
            log.error("JSON Map反序列化失败: {}", e.getMessage(), e);
            System.out.println("JsonUtils Debug - parseMap failed for input: " + json + ", error: " + e.getMessage());
            return null;
        }
    }
}