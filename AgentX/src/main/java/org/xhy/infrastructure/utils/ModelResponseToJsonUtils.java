package org.xhy.infrastructure.utils;

public class ModelResponseToJsonUtils {


    public static <T> T toJson(String text,Class<T> classz) {
        String json = text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1);
        return JsonUtils.parseObject(json,classz);
    }
}
