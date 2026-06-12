package com.example.blog_froum.service.impl;

import com.example.blog_froum.dto.category.CategoryRequest;
import com.example.blog_froum.dto.category.CategoryResponse;
import com.example.blog_froum.entity.Category;
import com.example.blog_froum.repository.CategoryRepository;
import com.example.blog_froum.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
public class CategoryServiceImpl implements CategoryService {

    private static final String DEFAULT_STATUS = "APPROVED";

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public Page<CategoryResponse> getCategories(String status, Pageable pageable) {
        Page<Category> categoryPage;
        if (status == null || status.trim().isEmpty()) {
            categoryPage = categoryRepository.findAllByOrderByDisplayOrderAsc(pageable);
        } else {
            categoryPage = categoryRepository.findByStatusOrderByDisplayOrderAsc(normalizeStatus(status), pageable);
        }

        return categoryPage.map(CategoryResponse::fromEntity);
    }

    @Override
    public List<CategoryResponse> getApprovedCategories() {
        return categoryRepository.findByStatusOrderByDisplayOrder("APPROVED").stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(CategoryResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = normalizeName(request.getName());
        if (categoryRepository.existsByName(name)) {
            throw new RuntimeException("分类名称已存在");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(trimToNull(request.getDescription()));
        category.setIcon(trimToNull(request.getIcon()));
        category.setDisplayOrder(request.getDisplayOrder() == null ? 999 : request.getDisplayOrder());
        category.setStatus(normalizeStatusOrDefault(request.getStatus()));
        category.setArticleCount(0);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        String name = normalizeName(request.getName());
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new RuntimeException("分类名称已存在");
        }

        category.setName(name);
        category.setDescription(trimToNull(request.getDescription()));
        category.setIcon(trimToNull(request.getIcon()));
        category.setDisplayOrder(request.getDisplayOrder() == null ? category.getDisplayOrder() : request.getDisplayOrder());
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            category.setStatus(normalizeStatus(request.getStatus()));
        }
        category.setUpdatedAt(LocalDateTime.now());

        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void approveCategory(Long id) {
        updateStatus(id, "APPROVED");
    }

    @Override
    @Transactional
    public void rejectCategory(Long id) {
        updateStatus(id, "REJECTED");
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        if (category.getArticleCount() != null && category.getArticleCount() > 0) {
            throw new RuntimeException("该分类下还有文章，无法删除");
        }

        categoryRepository.delete(category);
    }

    private void updateStatus(Long id, String status) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        category.setStatus(status);
        category.setUpdatedAt(LocalDateTime.now());
        categoryRepository.save(category);
    }

    private String normalizeStatusOrDefault(String status) {
        return status == null || status.trim().isEmpty() ? DEFAULT_STATUS : normalizeStatus(status);
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"PENDING".equals(normalized) && !"APPROVED".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new RuntimeException("分类状态无效");
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
