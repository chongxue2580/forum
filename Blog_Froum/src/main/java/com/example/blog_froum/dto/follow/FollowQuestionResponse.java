package com.example.blog_froum.dto.follow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowQuestionResponse {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer followCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime followedAt;
}
