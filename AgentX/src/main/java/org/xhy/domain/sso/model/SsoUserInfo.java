package org.xhy.domain.sso.model;

public class SsoUserInfo {
    private String id;
    private String name;
    private String email;
    private String avatar;
    private String desc;
    private SsoProvider provider;

    public SsoUserInfo() {
    }

    public SsoUserInfo(String id, String name, String email, String avatar, String desc, SsoProvider provider) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.avatar = avatar;
        this.desc = desc;
        this.provider = provider;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public SsoProvider getProvider() {
        return provider;
    }

    public void setProvider(SsoProvider provider) {
        this.provider = provider;
    }
}