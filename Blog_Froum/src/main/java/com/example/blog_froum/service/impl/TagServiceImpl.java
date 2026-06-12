package com.example.blog_froum.service.impl;

import com.example.blog_froum.dto.tag.TagRequest;
import com.example.blog_froum.dto.tag.TagResponse;
import com.example.blog_froum.entity.Tag;
import com.example.blog_froum.repository.TagRepository;
import com.example.blog_froum.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService {

    private static final String DEFAULT_STATUS = "APPROVED";

    @Autowired
    private TagRepository tagRepository;

    @Override
    public Page<TagResponse> getTags(String status, Pageable pageable) {
        Page<Tag> tagPage;
        if (status == null || status.trim().isEmpty()) {
            tagPage = tagRepository.findAllByOrderByUsageCountDesc(pageable);
        } else {
            tagPage = tagRepository.findByStatusOrderByUsageCountDesc(normalizeStatus(status), pageable);
        }

        return tagPage.map(TagResponse::fromEntity);
    }

    @Override
    public List<TagResponse> getApprovedTags() {
        return tagRepository.findByStatus("APPROVED").stream()
                .map(TagResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<TagResponse> getPopularTags(int limit) {
        return tagRepository.findByStatusOrderByUsageCountDesc("APPROVED", PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(TagResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public TagResponse getTagById(Long id) {
        return tagRepository.findById(id)
                .map(TagResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
    }

    @Override
    @Transactional
    public TagResponse createTag(TagRequest request) {
        String name = normalizeName(request.getName());
        if (tagRepository.existsByName(name)) {
            throw new RuntimeException("标签名称已存在");
        }

        Tag tag = new Tag();
        tag.setName(name);
        tag.setDescription(trimToNull(request.getDescription()));
        tag.setColor(trimToNull(request.getColor()));
        tag.setStatus(normalizeStatusOrDefault(request.getStatus()));
        tag.setUsageCount(0);
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUpdatedAt(LocalDateTime.now());

        return TagResponse.fromEntity(tagRepository.save(tag));
    }

    @Override
    @Transactional
    public TagResponse updateTag(Long id, TagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        String name = normalizeName(request.getName());
        if (tagRepository.existsByNameAndIdNot(name, id)) {
            throw new RuntimeException("标签名称已存在");
        }

        tag.setName(name);
        tag.setDescription(trimToNull(request.getDescription()));
        tag.setColor(trimToNull(request.getColor()));
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            tag.setStatus(normalizeStatus(request.getStatus()));
        }
        tag.setUpdatedAt(LocalDateTime.now());

        return TagResponse.fromEntity(tagRepository.save(tag));
    }

    @Override
    @Transactional
    public void approveTag(Long id) {
        updateStatus(id, "APPROVED");
    }

    @Override
    @Transactional
    public void rejectTag(Long id) {
        updateStatus(id, "REJECTED");
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        if (tag.getUsageCount() != null && tag.getUsageCount() > 0) {
            throw new RuntimeException("该标签已被使用，无法删除");
        }

        tagRepository.delete(tag);
    }

    private void updateStatus(Long id, String status) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
        tag.setStatus(status);
        tag.setUpdatedAt(LocalDateTime.now());
        tagRepository.save(tag);
    }

    private String normalizeStatusOrDefault(String status) {
        return status == null || status.trim().isEmpty() ? DEFAULT_STATUS : normalizeStatus(status);
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"PENDING".equals(normalized) && !"APPROVED".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new RuntimeException("标签状态无效");
        }
        return normalized;
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
