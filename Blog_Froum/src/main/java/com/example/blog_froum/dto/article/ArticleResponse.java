package com.example.blog_froum.dto.article;

import com.example.blog_froum.dto.category.CategoryResponse;
import com.example.blog_froum.dto.user.UserResponse;
import com.example.blog_froum.entity.Article;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private UserResponse author;
    private CategoryResponse category;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isOfficial;
    private Boolean isPinned;
    private Boolean isFeatured;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> tags;

    public static ArticleResponse fromEntity(Article article) {
        Objects.requireNonNull(article, "article");
        
        List<String> tagList = Collections.emptyList();
        if (article.getTagsString() != null && !article.getTagsString().isEmpty()) {
            tagList = Arrays.stream(article.getTagsString().split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList());
        }

        return ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .summary(article.getSummary())
                .content(article.getContent())
                .coverImage(article.getCoverImage())
                .author(article.getAuthor() != null ? UserResponse.fromUser(article.getAuthor()) : null)
                .category(article.getCategory() != null ? CategoryResponse.fromEntity(article.getCategory()) : null)
                .viewCount(article.getViewCount())
                .likeCount(article.getLikeCount())
                .commentCount(article.getCommentCount())
                .isOfficial(article.getIsOfficial())
                .isPinned(article.getIsPinned())
                .isFeatured(article.getIsFeatured())
                .status(article.getStatus())
                .publishedAt(article.getPublishedAt())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .tags(tagList)
                .build();
    }
} 
