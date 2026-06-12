package com.example.blog_froum.controller.admin;

import com.example.blog_froum.dto.tag.TagRequest;
import com.example.blog_froum.dto.tag.TagResponse;
import com.example.blog_froum.service.OperationLogService;
import com.example.blog_froum.service.TagService;
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
 * 管理员标签管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/tags")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "管理员标签管理")
public class AdminTagController {

    @Autowired
    private TagService tagService;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    @ApiOperation(value = "获取标签列表", notes = "分页获取系统中的标签")
    public Result<Page<TagResponse>> getAllTags(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "状态筛选") @RequestParam(required = false) String status) {
        try {
            log.info("管理员获取标签列表，页码: {}, 每页数量: {}, 状态: {}", page, pageSize, status);
            Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize);
            return Result.success(tagService.getTags(status, pageable));
        } catch (Exception e) {
            log.error("获取标签列表失败", e);
            return Result.error("获取标签列表失败: " + e.getMessage());
        }
    }

    @PostMapping
    @ApiOperation(value = "创建标签", notes = "管理员创建标签")
    public Result<TagResponse> createTag(@Valid @RequestBody TagRequest request,
                                         HttpServletRequest httpRequest) {
        try {
            log.info("管理员创建标签: {}", request.getName());
            TagResponse response = tagService.createTag(request);
            operationLogService.record(BaseContext.getCurrentId(), "TAG_MANAGEMENT", "create",
                    "创建标签：" + response.getName(), "TAG", response.getId(), response.getName(), httpRequest);
            return Result.success(response);
        } catch (Exception e) {
            log.error("创建标签失败", e);
            return Result.error("创建标签失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "更新标签", notes = "管理员更新标签")
    public Result<TagResponse> updateTag(
            @ApiParam(value = "标签ID", required = true) @PathVariable Long id,
            @Valid @RequestBody TagRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("管理员更新标签，ID: {}", id);
            TagResponse response = tagService.updateTag(id, request);
            operationLogService.record(BaseContext.getCurrentId(), "TAG_MANAGEMENT", "update",
                    "更新标签：" + response.getName(), "TAG", id, response.getName(), httpRequest);
            return Result.success(response);
        } catch (Exception e) {
            log.error("更新标签失败", e);
            return Result.error("更新标签失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/approve")
    @ApiOperation(value = "审核通过标签", notes = "管理员审核通过标签")
    public Result<Void> approveTag(@ApiParam(value = "标签ID", required = true) @PathVariable Long id,
                                   HttpServletRequest request) {
        try {
            log.info("管理员审核通过标签，ID: {}", id);
            tagService.approveTag(id);
            operationLogService.record(BaseContext.getCurrentId(), "TAG_MANAGEMENT", "approve",
                    "审核通过标签：" + id, "TAG", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("审核通过标签失败", e);
            return Result.error("审核通过失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    @ApiOperation(value = "审核拒绝标签", notes = "管理员审核拒绝标签")
    public Result<Void> rejectTag(@ApiParam(value = "标签ID", required = true) @PathVariable Long id,
                                  HttpServletRequest request) {
        try {
            log.info("管理员审核拒绝标签，ID: {}", id);
            tagService.rejectTag(id);
            operationLogService.record(BaseContext.getCurrentId(), "TAG_MANAGEMENT", "reject",
                    "审核拒绝标签：" + id, "TAG", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("审核拒绝标签失败", e);
            return Result.error("审核拒绝失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除标签", notes = "管理员删除标签")
    public Result<Void> deleteTag(@ApiParam(value = "标签ID", required = true) @PathVariable Long id,
                                  HttpServletRequest request) {
        try {
            log.info("管理员删除标签，ID: {}", id);
            tagService.deleteTag(id);
            operationLogService.record(BaseContext.getCurrentId(), "TAG_MANAGEMENT", "delete",
                    "删除标签：" + id, "TAG", id, null, request);
            return Result.success();
        } catch (Exception e) {
            log.error("删除标签失败", e);
            return Result.error("删除标签失败: " + e.getMessage());
        }
    }
}
