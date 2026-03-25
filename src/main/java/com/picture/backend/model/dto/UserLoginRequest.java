package com.picture.backend.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginRequest {
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}
