package org.xhy.infrastructure.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 * 基于Fastjson封装，提供常用的JSON操作
 */
public class JsonUtils {

    /**
     * 将对象转换为JSON字符串
     *
     * @param object 要转换的对象
     * @return JSON字符串
     */
    public static String toJsonString(Object object) {
        return JSON.toJSONString(object, SerializerFeature.DisableCircularReferenceDetect);
    }

    /**
     * 将JSON字符串转换为指定类型的对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 转换后的对象
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }

    /**
     * 将JSON字符串转换为JSONObject
     *
     * @param json JSON字符串
     * @return JSONObject对象
     */
    public static JSONObject parseObject(String json) {
        return JSON.parseObject(json);
    }

    /**
     * 将JSON字符串解析为指定类型的对象列表
     *
     * @param json  JSON数组字符串
     * @param clazz 列表元素类型
     * @param <T>   元素类型泛型
     * @return 对象列表
     */
    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        return JSON.parseArray(json, clazz);
    }

    /**
     * 将JSON字符串解析为JSONArray
     *
     * @param json JSON数组字符串
     * @return JSONArray对象
     */
    public static JSONArray parseArray(String json) {
        return JSON.parseArray(json);
    }

    /**
     * 将Map转换为指定类型的对象
     *
     * @param map   Map对象
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 转换后的对象
     */
    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        return parseObject(toJsonString(map), clazz);
    }

    /**
     * 将对象转换为Map
     *
     * @param object 要转换的对象
     * @return Map对象
     */
    public static Map<String, Object> objectToMap(Object object) {
        return parseObject(toJsonString(object), Map.class);
    }

    /**
     * 检查JSON字符串是否有效
     *
     * @param json 要检查的JSON字符串
     * @return 如果是有效的JSON则返回true，否则返回false
     */
    public static boolean isValidJson(String json) {
        try {
            JSON.parse(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}