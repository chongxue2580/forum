package com.example.blog_froum.controller.admin;

import com.example.blog_froum.dto.category.CategoryRequest;
import com.example.blog_froum.dto.category.CategoryResponse;
import com.example.blog_froum.service.CategoryService;
import com.example.blog_froum.service.OperationLogService;
import com.example.blog_froum.utils.BaseContext;
import com.example.blog_froum.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 管理员分类管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/categories")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "管理员分类管理")
public class AdminCategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    @ApiOperation(value = "获取分类列表", notes = "分页获取系统中的分类")
    public Result<Page<CategoryResponse>> getAllCategories(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "状态筛选") @RequestParam(required = false) String status) {
        try {
            log.info("管理员获取分类列表，页码: {}, 每页数量: {}, 状态: {}", page, pageSize, status);
            Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize);
            return Result.success(categoryService.getCategories(status, pageable));
        } catch (Exception e) {
            log.error("获取分类列表失败", e);
            return Result.error("获取分类列表失败: " + e.getMessage());
        }
    }

    @PostMapping
    @ApiOperation(value = "创建分类", notes = "管理员创建分类")
    public Result<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request,
                                                   HttpServletRequest httpRequest) {
        try {
            log.info("管理员创建分类: {}", request.getName());
            CategoryResponse response = categoryService.createCategory(request);
            operationLogService.record(BaseContext.getCurrentId(), "CATEGORY_MANAGEMENT", "create",
                    "创建分类：" + response.getName(), "CATEGORY", response.getId(), response.getName(), httpRequest);
            return Result.success(response);
        } catch (Exception e) {
            log.error("创建分类失败", e);
            return Result.error("创建分类失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "更新分类", notes = "管理员更新分类")
    public Result<CategoryResponse> updateCategory(
            @ApiParam(value = "分类ID", required = true) @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("管理员更新分类，ID: {}", id);
            CategoryResponse response = categoryService.updateCategory(id, request);
            operationLogService.record(BaseContext.getCurrentId(), "CATEGORY_MANAGEMENT", "update",
                    "更新分类：" + response.getName(), "CATEGORY", id, response.getName(), httpRequest);
            return Result.success(response);
        } catch (Exception e) {
            log.error("更新分类失败", e);
            return Result.error("更新分类失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/approve")
    @ApiOperation(value = "审核通过分类", notes = "管理员审核通过分类")
    public Result<Void> approveCategory(@ApiParam(value = "分类ID", required = true) @PathVariable Long id,
                                        HttpServletRequest request) {
        try {
            log.info("管理员审核通过分类，ID: {}", id);
            categoryService.approveCategory(id);
            operationLogService.record(BaseContext.getCurrentId(), "CATEGORY_MANAGEMENT", "approve",
                    "审核通过分类：" + id, "CATEGORY", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("审核通过分类失败", e);
            return Result.error("审核通过失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    @ApiOperation(value = "审核拒绝分类", notes = "管理员审核拒绝分类")
    public Result<Void> rejectCategory(@ApiParam(value = "分类ID", required = true) @PathVariable Long id,
                                       HttpServletRequest request) {
        try {
            log.info("管理员审核拒绝分类，ID: {}", id);
            categoryService.rejectCategory(id);
            operationLogService.record(BaseContext.getCurrentId(), "CATEGORY_MANAGEMENT", "reject",
                    "审核拒绝分类：" + id, "CATEGORY", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("审核拒绝分类失败", e);
            return Result.error("审核拒绝失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除分类", notes = "管理员删除分类")
    public Result<Void> deleteCategory(@ApiParam(value = "分类ID", required = true) @PathVariable Long id,
                                       HttpServletRequest request) {
        try {
            log.info("管理员删除分类，ID: {}", id);
            categoryService.deleteCategory(id);
            operationLogService.record(BaseContext.getCurrentId(), "CATEGORY_MANAGEMENT", "delete",
                    "删除分类：" + id, "CATEGORY", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("删除分类失败", e);
            return Result.error("删除分类失败: " + e.getMessage());
        }
    }
}
