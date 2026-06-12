package com.example.blog_froum.service;

import com.example.blog_froum.dto.category.CategoryRequest;
import com.example.blog_froum.dto.category.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    Page<CategoryResponse> getCategories(String status, Pageable pageable);

    List<CategoryResponse> getApprovedCategories();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void approveCategory(Long id);

    void rejectCategory(Long id);

    void deleteCategory(Long id);
}
