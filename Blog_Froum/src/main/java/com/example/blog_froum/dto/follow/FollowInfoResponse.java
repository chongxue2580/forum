package com.example.blog_froum.dto.follow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowInfoResponse {
    private long count;

    @JsonProperty("isFollowed")
    private boolean isFollowed;
}
