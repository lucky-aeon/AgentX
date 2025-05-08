package org.xhy.interfaces.dto.user.response;

public class CaptchaResponse {
    private String uuid;
    private String imageBase64;

    public CaptchaResponse() {
    }

    public CaptchaResponse(String uuid, String imageBase64) {
        this.uuid = uuid;
        this.imageBase64 = imageBase64;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}