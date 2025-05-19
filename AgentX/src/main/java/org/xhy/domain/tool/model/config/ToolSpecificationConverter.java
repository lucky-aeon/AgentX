package org.xhy.domain.tool.model.config;

import dev.langchain4j.agent.tool.ToolSpecification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具规范转换器，用于将ToolSpecification对象转换为可序列化的DTO对象
 */
public class ToolSpecificationConverter {

    /**
     * 将ToolSpecification列表转换为ToolDefinition列表
     */
    public static List<ToolDefinition> convert(List<ToolSpecification> specifications) {
        if (specifications == null) {
            return new ArrayList<>();
        }

        List<ToolDefinition> result = new ArrayList<>();
        for (ToolSpecification spec : specifications) {
            result.add(convertSingle(spec));
        }
        return result;
    }

    /**
     * 转换单个ToolSpecification对象
     */
    public static ToolDefinition convertSingle(ToolSpecification spec) {
        ToolDefinition dto = new ToolDefinition();
        dto.setName(spec.name());
        dto.setDescription(spec.description());
        
        // 处理参数
        ToolParameter paramsDto = extractParameters(spec);
        // 将ToolParameter转换为Map<String, Object>格式以匹配ToolDefinition的参数类型
        Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put("properties", paramsDto.getProperties());
        parametersMap.put("required", paramsDto.getRequired());
        
        dto.setParameters(parametersMap);
        dto.setEnabled(true);
        
        return dto;
    }

    /**
     * 提取参数信息
     */
    private static ToolParameter extractParameters(ToolSpecification spec) {
        ToolParameter paramsDto = new ToolParameter();

        if (spec == null || spec.parameters() == null) {
            return paramsDto;
        }

        try {
            // 获取JsonSchema的字符串表示
            Object parameters = spec.parameters();
            
            // 处理属性
            Map<String, ParameterProperty> propertiesDto = new HashMap<>();
            
            // 通过toString方法解析属性和描述
            String parametersStr = parameters.toString();
            
            // 解析properties部分
            if (parametersStr.contains("properties = {")) {
                int startProps = parametersStr.indexOf("properties = {") + 13;
                int endProps = findClosingBracket(parametersStr, startProps);
                if (endProps > startProps) {
                    String propsStr = parametersStr.substring(startProps, endProps);
                    Map<String, String> props = parseProperties(propsStr);
                    
                    for (Map.Entry<String, String> entry : props.entrySet()) {
                        propertiesDto.put(entry.getKey(), new ParameterProperty(entry.getValue()));
                    }
                }
            }
            paramsDto.setProperties(propertiesDto);
            
            // 解析required部分
            if (parametersStr.contains("required = [")) {
                int startReq = parametersStr.indexOf("required = [") + 12;
                int endReq = parametersStr.indexOf("]", startReq);
                if (endReq > startReq) {
                    String reqStr = parametersStr.substring(startReq, endReq);
                    List<String> required = parseRequired(reqStr);
                    paramsDto.setRequired(required.toArray(new String[0]));
                }
            }
            
        } catch (Exception e) {
            System.err.println("提取参数时出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        return paramsDto;
    }
    
    /**
     * 查找闭合括号位置
     */
    private static int findClosingBracket(String str, int start) {
        int count = 1;
        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '{') {
                count++;
            } else if (c == '}') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * 解析属性字符串
     */
    private static Map<String, String> parseProperties(String propsStr) {
        Map<String, String> result = new HashMap<>();
        
        String[] parts = propsStr.split(",\\s*");
        for (String part : parts) {
            if (part.contains("=")) {
                String[] keyValue = part.split("=", 2);
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                
                // 尝试提取描述
                String description = extractDescription(value);
                result.put(key, description);
            }
        }
        
        return result;
    }
    
    /**
     * 提取描述信息
     */
    private static String extractDescription(String value) {
        if (value.contains("description = \"")) {
            int descStart = value.indexOf("description = \"") + 15;
            int descEnd = value.indexOf("\"", descStart);
            if (descEnd > descStart) {
                return value.substring(descStart, descEnd);
            }
        }
        return null;
    }
    
    /**
     * 解析required字段
     */
    private static List<String> parseRequired(String reqStr) {
        List<String> result = new ArrayList<>();
        String[] parts = reqStr.split(",\\s*");
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.add(part.trim());
            }
        }
        return result;
    }
} 