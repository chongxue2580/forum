package com.example.blog_froum.enums;

/**
 * 文章状态枚举
 */
public enum ArticleStatus {
    DRAFT("草稿"),
    PENDING("待审核"),
    APPROVED("已发布"),
    REJECTED("已拒绝"),
    ARCHIVED("已归档");

    private final String description;

    ArticleStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return this.name();
    }
}
