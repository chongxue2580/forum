package com.example.blog_froum.enums;

/**
 * 用户封禁类型。
 */
public enum UserBanType {
    NONE("NONE", "未封禁"),
    LOGIN("LOGIN", "禁止登录"),
    CONTENT("CONTENT", "禁止发布内容");

    private final String code;
    private final String description;

    UserBanType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserBanType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return NONE;
        }
        for (UserBanType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ban type: " + code);
    }
}
