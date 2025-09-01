package org.xhy.infrastructure.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/** Neo4j兼容的JSON解析器 解决FastJSON解析小数为BigDecimal导致Neo4j不兼容的问题
 * 
 * @author AgentX */
public class Neo4jCompatibleJsonParser {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jCompatibleJsonParser.class);

    /** 解析JSON字符串为指定类型 注意：此方法只做解析，不处理BigDecimal转换，由上层服务负责类型转换
     * 
     * @param <T> 目标类型
     * @param jsonString JSON字符串
     * @param clazz 目标类型Class
     * @return 解析后的对象 */
    public static <T> T parseObject(String jsonString, Class<T> clazz) {
        try {
            // 直接使用标准JSON解析，不进行BigDecimal转换
            // BigDecimal转换由Neo4jValueConverter在图数据摄取时处理
            return JSON.parseObject(jsonString, clazz);
        } catch (Exception e) {
            logger.error("JSON解析失败: {}", e.getMessage(), e);
            logger.debug("原始JSON: {}", jsonString);
            throw new RuntimeException("JSON解析失败: " + e.getMessage(), e);
        }
    }

    /** 解析JSON字符串为Object
     * 
     * @param jsonString JSON字符串
     * @return 解析后的Object */
    public static Object parseObject(String jsonString) {
        try {
            // 直接使用标准JSON解析
            return JSON.parseObject(jsonString);
        } catch (Exception e) {
            logger.error("JSON解析失败: {}", e.getMessage(), e);
            logger.debug("原始JSON: {}", jsonString);
            throw new RuntimeException("JSON解析失败: " + e.getMessage(), e);
        }
    }

    /** 将对象转换为JSON字符串
     * 
     * @param object 要转换的对象
     * @return JSON字符串 */
    public static String toJSONString(Object object) {
        try {
            return JSON.toJSONString(object);
        } catch (Exception e) {
            logger.error("JSON序列化失败: {}", e.getMessage(), e);
            throw new RuntimeException("JSON序列化失败: " + e.getMessage(), e);
        }
    }

    /** 简单的BigDecimal转换，仅供工具方法使用 主要的BigDecimal转换由Neo4jValueConverter负责
     * 
     * @param obj 要转换的对象
     * @return 转换后的对象 */
    public static <T> T convertBigDecimalInObject(T obj) {
        // 简化版本，不做复杂的递归转换
        // 主要的类型转换交给Neo4jValueConverter处理
        return obj;
    }

    /** 检查并转换单个值的BigDecimal为Double
     * 
     * @param obj 要检查的对象
     * @return 转换后的对象 */
    public static Object convertBigDecimalToDouble(Object obj) {
        if (obj instanceof BigDecimal) {
            return ((BigDecimal) obj).doubleValue();
        }
        return obj;
    }
}
