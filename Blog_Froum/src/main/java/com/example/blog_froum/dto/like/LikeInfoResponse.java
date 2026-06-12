package com.example.blog_froum.dto.like;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeInfoResponse {
    private long count;
    private boolean isLiked;
}
