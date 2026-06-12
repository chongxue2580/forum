package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.tag.TagResponse;
import com.example.blog_froum.service.TagService;
import com.example.blog_froum.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tags")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "标签管理")
public class TagController {

    @Autowired
    private TagService tagService;

    /**
     * 获取所有标签
     */
    @GetMapping
    @ApiOperation(value = "获取所有标签", notes = "获取所有已批准的标签列表")
    public Result<List<TagResponse>> getAllTags() {
        try {
            log.info("获取所有标签");
            List<TagResponse> responseList = tagService.getApprovedTags();
            return Result.success(responseList);
        } catch (Exception e) {
            log.error("获取标签列表失败", e);
            return Result.error("获取标签列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门标签
     */
    @GetMapping("/popular")
    @ApiOperation(value = "获取热门标签", notes = "获取使用次数最多的标签")
    public Result<List<TagResponse>> getPopularTags(
            @ApiParam(value = "数量限制", defaultValue = "10") @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("获取热门标签，数量: {}", limit);
            List<TagResponse> responseList = tagService.getPopularTags(limit);
            return Result.success(responseList);
        } catch (Exception e) {
            log.error("获取热门标签失败", e);
            return Result.error("获取热门标签失败: " + e.getMessage());
        }
    }

    /**
     * 获取标签详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "获取标签详情", notes = "根据ID获取标签详情")
    public Result<TagResponse> getTagById(@PathVariable Long id) {
        try {
            log.info("获取标签详情，ID: {}", id);
            return Result.success(tagService.getTagById(id));
        } catch (Exception e) {
            log.error("获取标签详情失败", e);
            return Result.error("获取标签详情失败: " + e.getMessage());
        }
    }
} 
