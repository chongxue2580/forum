package com.example.blog_froum.service;

import com.example.blog_froum.dto.tag.TagRequest;
import com.example.blog_froum.dto.tag.TagResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TagService {
    Page<TagResponse> getTags(String status, Pageable pageable);

    List<TagResponse> getApprovedTags();

    List<TagResponse> getPopularTags(int limit);

    TagResponse getTagById(Long id);

    TagResponse createTag(TagRequest request);

    TagResponse updateTag(Long id, TagRequest request);

    void approveTag(Long id);

    void rejectTag(Long id);

    void deleteTag(Long id);
}
