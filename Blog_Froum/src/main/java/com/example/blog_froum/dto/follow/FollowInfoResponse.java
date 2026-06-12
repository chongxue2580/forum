package com.example.blog_froum.dto.follow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowInfoResponse {
    private long count;
    private boolean isFollowed;
}
