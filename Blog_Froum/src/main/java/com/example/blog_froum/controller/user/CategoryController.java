package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.category.CategoryResponse;
import com.example.blog_froum.service.CategoryService;
import com.example.blog_froum.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "分类管理")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 获取所有分类
     */
    @GetMapping
    @ApiOperation(value = "获取所有分类", notes = "获取所有已批准的分类列表")
    public Result<List<CategoryResponse>> getAllCategories() {
        try {
            log.info("获取所有分类");
            List<CategoryResponse> responseList = categoryService.getApprovedCategories();
            return Result.success(responseList);
        } catch (Exception e) {
            log.error("获取分类列表失败", e);
            return Result.error("获取分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "获取分类详情", notes = "根据ID获取分类详情")
    public Result<CategoryResponse> getCategoryById(@PathVariable Long id) {
        try {
            log.info("获取分类详情，ID: {}", id);
            return Result.success(categoryService.getCategoryById(id));
        } catch (Exception e) {
            log.error("获取分类详情失败", e);
            return Result.error("获取分类详情失败: " + e.getMessage());
        }
    }
} 
