package com.picture.backend.model.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum UserRoleEnums {
    USER("user", "普通用户"),
    ADMIN("admin", "管理员");

    private final String value;
    private final String text;
    private static final Map<String, UserRoleEnums> hashMap = new HashMap<>();
    UserRoleEnums(String value, String text) {
        this.value = value;
        this.text = text;
    }

    static {
        for (UserRoleEnums userRoleEnums : values()){
            hashMap.put(userRoleEnums.getValue(), userRoleEnums);
        }
    }
    /**
     * 根据value获取枚举
     * @param value
     * @return
     */
    public static UserRoleEnums getEnumByValue(String value) {
        return hashMap.get(value);
    }
}
