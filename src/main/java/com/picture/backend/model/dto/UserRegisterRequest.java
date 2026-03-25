package com.picture.backend.model.dto;

import lombok.Data;

import java.io.Serializable;
@Data
public class UserRegisterRequest {

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
