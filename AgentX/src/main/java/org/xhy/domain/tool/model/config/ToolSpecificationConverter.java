package org.xhy.domain.tool.model.config;

import dev.langchain4j.agent.tool.ToolSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/** 工具规范转换器，用于将ToolSpecification对象转换为可序列化的DTO对象 */
public class ToolSpecificationConverter {
    private static final Logger log = LoggerFactory.getLogger(ToolSpecificationConverter.class);

    /** 将ToolSpecification列表转换为ToolDefinition列表 */
    public static List<ToolDefinition> convert(List<ToolSpecification> specifications) {
        if (specifications == null) {
            return new ArrayList<>();
        }

        List<ToolDefinition> result = new ArrayList<>();
        for (ToolSpecification spec : specifications) {
            try {
                result.add(convertSingle(spec));
            } catch (Exception e) {
                log.error("转换工具规范失败: {}", spec.name(), e);
            }
        }
        return result;
    }

    /** 转换单个ToolSpecification对象 */
    public static ToolDefinition convertSingle(ToolSpecification spec) {
        if (spec == null) {
            throw new IllegalArgumentException("工具规范不能为空");
        }

        ToolDefinition dto = new ToolDefinition();
        dto.setName(spec.name());
        dto.setDescription(spec.description());
        dto.setEnabled(true);

        // 处理参数
        Map<String, Object> parametersMap = new HashMap<>();
        if (spec.parameters() != null) {
            ToolParameter toolParameter = extractParametersReflectively(spec);
            parametersMap.put("properties", toolParameter.getProperties());
            parametersMap.put("required", toolParameter.getRequired());
        }

        dto.setParameters(parametersMap);
        return dto;
    }

    /** 使用反射机制提取参数信息，更加健壮 */
    private static ToolParameter extractParametersReflectively(ToolSpecification spec) {
        ToolParameter toolParameter = new ToolParameter();
        Map<String, ParameterProperty> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        try {
            // 获取参数对象
            Object parameters = spec.parameters();

            // 尝试获取properties字段
            Map<String, Object> propertiesMap = getFieldValueSafely(parameters, "properties", Map.class);
            if (propertiesMap != null) {
                for (Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
                    String propertyName = entry.getKey();
                    Object propertyValue = entry.getValue();

                    // 获取属性的description
                    String description = null;
                    if (propertyValue != null) {
                        Object descObj = getFieldValueSafely(propertyValue, "description", Object.class);
                        description = descObj != null ? descObj.toString() : null;
                    }

                    properties.put(propertyName, new ParameterProperty(description));
                }
            }

            // 尝试获取required字段
            Object requiredObj = getFieldValueSafely(parameters, "required", Object.class);
            if (requiredObj instanceof Collection) {
                for (Object item : (Collection<?>) requiredObj) {
                    if (item != null) {
                        required.add(item.toString());
                    }
                }
            } else if (requiredObj instanceof Object[]) {
                for (Object item : (Object[]) requiredObj) {
                    if (item != null) {
                        required.add(item.toString());
                    }
                }
            }

        } catch (Exception e) {
            log.error("反射提取参数失败", e);
        }

        toolParameter.setProperties(properties);
        toolParameter.setRequired(required.toArray(new String[0]));
        return toolParameter;
    }

    /** 安全地获取对象字段值，处理可能的异常 */
    @SuppressWarnings("unchecked")
    private static <T> T getFieldValueSafely(Object object, String fieldName, Class<T> expectedType) {
        if (object == null || fieldName == null) {
            return null;
        }

        // 1. 首先尝试通过getter方法获取
        try {
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Method getter = object.getClass().getMethod(getterName);
            Object result = getter.invoke(object);
            if (expectedType.isInstance(result)) {
                return (T) result;
            }
        } catch (Exception ignored) {
            // 如果getter方法不存在或调用失败，继续尝试其他方法
        }

        // 2. 尝试通过直接字段访问获取
        try {
            Field field = findField(object.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object result = field.get(object);
                if (expectedType.isInstance(result)) {
                    return (T) result;
                }
            }
        } catch (Exception ignored) {
            // 如果字段访问失败，继续尝试其他方法
        }

        // 3. 如果对象是Map，尝试从Map中获取
        if (object instanceof Map) {
            Object result = ((Map<?, ?>) object).get(fieldName);
            if (expectedType.isInstance(result)) {
                return (T) result;
            }
        }

        return null;
    }

    /** 在类的层次结构中查找字段，包括父类和接口 */
    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> searchType = clazz;
        while (searchType != null && !Object.class.equals(searchType)) {
            try {
                return searchType.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // 继续在父类中查找
                searchType = searchType.getSuperclass();
            }
        }
        return null;
    }
}