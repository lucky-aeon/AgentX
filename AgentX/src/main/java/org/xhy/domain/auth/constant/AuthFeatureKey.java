package org.xhy.domain.auth.constant;

/** 认证功能键枚举 */
public enum AuthFeatureKey {

    // 登录方式常量
    NORMAL_LOGIN("NORMAL_LOGIN", "普通登录"), GITHUB_LOGIN("GITHUB_LOGIN", "GitHub登录"), COMMUNITY_LOGIN("COMMUNITY_LOGIN",
            "敲鸭登录"),

    // 注册功能常量
    USER_REGISTER("USER_REGISTER", "用户注册");

    private final String code;
    private final String name;

    AuthFeatureKey(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static AuthFeatureKey fromCode(String code) {
        for (AuthFeatureKey key : values()) {
            if (key.code.equals(code)) {
                return key;
            }
        }
        throw new IllegalArgumentException("Unknown auth feature key: " + code);
    }
}