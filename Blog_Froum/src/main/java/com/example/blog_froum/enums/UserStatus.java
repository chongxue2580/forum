package com.example.blog_froum.enums;

/**
 * 用户状态枚举
 */
public enum UserStatus {
    ACTIVE("ACTIVE", "正常"),
    DISABLED("DISABLED", "禁用"),
    LOCKED("LOCKED", "锁定");

    private final String code;
    private final String description;

    UserStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserStatus fromCode(String code) {
        for (UserStatus status : UserStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}
