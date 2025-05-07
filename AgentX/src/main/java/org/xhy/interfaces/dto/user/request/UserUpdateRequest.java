package org.xhy.interfaces.dto.user.request;

import jakarta.validation.constraints.NotBlank;

public class UserUpdateRequest {

    @NotBlank(message = "昵称不可未空")
    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
