//package org.xhy.domain.agent.service.mcp;
//
//import dev.langchain4j.agent.tool.ToolSpecification;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * 工具规范转换器，用于将ToolSpecification对象转换为可序列化的DTO对象
// */
//public class ToolSpecificationConverter {
//
//    /**
//     * 将ToolSpecification列表转换为ToolDto列表
//     */
//    public static List<ToolDto> convert(List<ToolSpecification> specifications) {
//        if (specifications == null) {
//            return new ArrayList<>();
//        }
//
//        List<ToolDto> result = new ArrayList<>();
//        for (ToolSpecification spec : specifications) {
//            result.add(convertSingle(spec));
//        }
//        return result;
//    }
//
//    /**
//     * 转换单个ToolSpecification对象
//     */
//    public static ToolDto convertSingle(ToolSpecification spec) {
//        ToolDto dto = new ToolDto();
//        dto.setName(spec.name());
//        dto.setDescription(spec.description());
//
//        // 处理参数
//        ParametersDto paramsDto = extractParameters(spec);
//        dto.setParameters(paramsDto);
//
//        return dto;
//    }
//
//    /**
//     * 提取参数信息
//     */
//    private static ParametersDto extractParameters(ToolSpecification spec) {
//        ParametersDto paramsDto = new ParametersDto();
//
//        if (spec == null || spec.parameters() == null) {
//            return paramsDto;
//        }
//
//        try {
//            // 获取JsonSchema的字符串表示
//            Object parameters = spec.parameters();
//
//            // 处理属性
//            Map<String, PropertyDto> propertiesDto = new HashMap<>();
//
//            // 通过toString方法解析属性和描述
//            String parametersStr = parameters.toString();
//
//            // 解析properties部分
//            if (parametersStr.contains("properties = {")) {
//                int startProps = parametersStr.indexOf("properties = {") + 13;
//                int endProps = findClosingBracket(parametersStr, startProps);
//                if (endProps > startProps) {
//                    String propsStr = parametersStr.substring(startProps, endProps);
//                    Map<String, String> props = parseProperties(propsStr);
//
//                    for (Map.Entry<String, String> entry : props.entrySet()) {
//                        propertiesDto.put(entry.getKey(), new PropertyDto(entry.getValue()));
//                    }
//                }
//            }
//            paramsDto.setProperties(propertiesDto);
//
//            // 解析required部分
//            if (parametersStr.contains("required = [")) {
//                int startReq = parametersStr.indexOf("required = [") + 12;
//                int endReq = parametersStr.indexOf("]", startReq);
//                if (endReq > startReq) {
//                    String reqStr = parametersStr.substring(startReq, endReq);
//                    List<String> required = parseRequired(reqStr);
//                    paramsDto.setRequired(required);
//                }
//            }
//
//        } catch (Exception e) {
//            System.err.println("提取参数时出错: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return paramsDto;
//    }
//
//    /**
//     * 查找闭合括号位置
//     */
//    private static int findClosingBracket(String str, int start) {
//        int count = 1;
//        for (int i = start; i < str.length(); i++) {
//            char c = str.charAt(i);
//            if (c == '{') {
//                count++;
//            } else if (c == '}') {
//                count--;
//                if (count == 0) {
//                    return i;
//                }
//            }
//        }
//        return -1;
//    }
//
//    /**
//     * 解析属性字符串
//     */
//    private static Map<String, String> parseProperties(String propsStr) {
//        Map<String, String> result = new HashMap<>();
//
//        String[] parts = propsStr.split(",\\s*");
//        for (String part : parts) {
//            if (part.contains("=")) {
//                String[] keyValue = part.split("=", 2);
//                String key = keyValue[0].trim();
//                String value = keyValue[1].trim();
//
//                // 尝试提取描述
//                String description = extractDescription(value);
//                result.put(key, description);
//            }
//        }
//
//        return result;
//    }
//
//    /**
//     * 提取描述信息
//     */
//    private static String extractDescription(String value) {
//        if (value.contains("description = \"")) {
//            int descStart = value.indexOf("description = \"") + 15;
//            int descEnd = value.indexOf("\"", descStart);
//            if (descEnd > descStart) {
//                return value.substring(descStart, descEnd);
//            }
//        }
//        return null;
//    }
//
//    /**
//     * 解析required字段
//     */
//    private static List<String> parseRequired(String reqStr) {
//        List<String> result = new ArrayList<>();
//        String[] parts = reqStr.split(",\\s*");
//        for (String part : parts) {
//            if (!part.isEmpty()) {
//                result.add(part.trim());
//            }
//        }
//        return result;
//    }
//
//    /**
//     * 工具DTO
//     */
//    public static class ToolDto {
//        private String name;
//        private String description;
//        private ParametersDto parameters;
//
//        public String getName() { return name; }
//        public void setName(String name) { this.name = name; }
//
//        public String getDescription() { return description; }
//        public void setDescription(String description) { this.description = description; }
//
//        public ParametersDto getParameters() { return parameters; }
//        public void setParameters(ParametersDto parameters) { this.parameters = parameters; }
//    }
//
//    /**
//     * 参数DTO
//     */
//    public static class ParametersDto {
//        private Map<String, PropertyDto> properties;
//        private List<String> required;
//
//        public Map<String, PropertyDto> getProperties() { return properties; }
//        public void setProperties(Map<String, PropertyDto> properties) { this.properties = properties; }
//
//        public List<String> getRequired() { return required; }
//        public void setRequired(List<String> required) { this.required = required; }
//    }
//
//    /**
//     * 属性DTO
//     */
//    public static class PropertyDto {
//        private String description;
//
//        public PropertyDto() {}
//
//        public PropertyDto(String description) {
//            this.description = description;
//        }
//
//        public String getDescription() { return description; }
//        public void setDescription(String description) { this.description = description; }
//    }
//}