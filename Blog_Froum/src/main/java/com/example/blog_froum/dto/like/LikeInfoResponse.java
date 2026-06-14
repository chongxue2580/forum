package com.example.blog_froum.dto.like;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeInfoResponse {
    private long count;

    @JsonProperty("isLiked")
    private boolean isLiked;
}
