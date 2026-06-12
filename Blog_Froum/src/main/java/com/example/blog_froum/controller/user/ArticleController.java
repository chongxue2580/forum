package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.article.ArticleCreateRequest;
import com.example.blog_froum.dto.article.ArticleResponse;
import com.example.blog_froum.dto.article.ArticleUpdateRequest;
import com.example.blog_froum.service.ArticleService;
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

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/articles")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:5190", "http://localhost:8081"})
@Api(tags = "文章管理")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    /**
     * 获取文章列表
     */
    @GetMapping
    @ApiOperation(value = "获取文章列表", notes = "分页获取文章列表")
    public Result<Page<ArticleResponse>> getArticles(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "分类ID") @RequestParam(required = false) Long categoryId,
            @ApiParam(value = "标签ID") @RequestParam(required = false) Long tagId,
            @ApiParam(value = "关键词") @RequestParam(required = false) String keyword,
            @ApiParam(value = "排序：newest/hottest/mostComments/mostLiked") @RequestParam(defaultValue = "newest") String sort) {
        try {
            log.info("获取文章列表，页码: {}, 每页数量: {}, 分类: {}, 标签: {}, 关键词: {}, 排序: {}",
                    page, pageSize, categoryId, tagId, keyword, sort);
            Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));
            Page<ArticleResponse> responsePage = articleService.searchPublicArticles(categoryId, tagId, keyword, sort, pageable);

            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取文章列表失败", e);
            return Result.error("获取文章列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取文章详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "获取文章详情", notes = "根据ID获取文章详情")
    public Result<ArticleResponse> getArticleById(
            @ApiParam(value = "文章ID", required = true) @PathVariable Long id) {
        try {
            log.info("获取文章详情，ID: {}", id);
            return Result.success(articleService.getArticleById(id));
        } catch (Exception e) {
            log.error("获取文章详情失败", e);
            return Result.error("获取文章详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门文章
     */
    @GetMapping("/popular")
    @ApiOperation(value = "获取热门文章", notes = "获取浏览量最高的文章")
    public Result<List<ArticleResponse>> getPopularArticles(
            @ApiParam(value = "数量限制", defaultValue = "5") @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("获取热门文章，数量: {}", limit);
            List<ArticleResponse> responseList = articleService.getPopularArticles(limit);
            return Result.success(responseList);
        } catch (Exception e) {
            log.error("获取热门文章失败", e);
            return Result.error("获取热门文章失败: " + e.getMessage());
        }
    }

    /**
     * 获取推荐文章
     */
    @GetMapping("/featured")
    @ApiOperation(value = "获取推荐文章", notes = "获取精选或点赞数最高的文章")
    public Result<List<ArticleResponse>> getFeaturedArticles(
            @ApiParam(value = "数量限制", defaultValue = "5") @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("获取推荐文章，数量: {}", limit);
            List<ArticleResponse> responseList = articleService.getFeaturedArticles(limit);
            return Result.success(responseList);
        } catch (Exception e) {
            log.error("获取推荐文章失败", e);
            return Result.error("获取推荐文章失败: " + e.getMessage());
        }
    }

    /**
     * 创建文章
     */
    @PostMapping
    @ApiOperation(value = "创建文章", notes = "用户创建新文章")
    public Result<ArticleResponse> createArticle(
            @Valid @RequestBody ArticleCreateRequest request) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 创建文章: {}", userId, request.getTitle());
            ArticleResponse response = articleService.createArticle(userId, request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("创建文章失败", e);
            return Result.error("创建文章失败: " + e.getMessage());
        }
    }

    /**
     * 更新文章
     */
    @PutMapping("/{id}")
    @ApiOperation(value = "更新文章", notes = "用户更新自己的文章")
    public Result<ArticleResponse> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleUpdateRequest request) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 更新文章 {}", userId, id);
            ArticleResponse response = articleService.updateArticle(id, userId, request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("更新文章失败", e);
            return Result.error("更新文章失败: " + e.getMessage());
        }
    }

    /**
     * 删除文章
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除文章", notes = "用户删除自己的文章")
    public Result<Void> deleteArticle(
            @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 删除文章 {}", userId, id);
            articleService.deleteArticle(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除文章失败", e);
            return Result.error("删除文章失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的文章列表
     */
    @GetMapping("/my")
    @ApiOperation(value = "获取我的文章", notes = "获取当前用户的文章列表")
    public Result<Page<ArticleResponse>> getMyArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("获取用户 {} 的文章列表", userId);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<ArticleResponse> responsePage = articleService.getUserArticles(userId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取用户文章列表失败", e);
            return Result.error("获取用户文章列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定用户的文章列表
     */
    @GetMapping("/user/{userId}")
    @ApiOperation(value = "获取指定用户的文章", notes = "获取指定用户发布的文章列表")
    public Result<Page<ArticleResponse>> getUserArticles(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("获取用户 {} 的文章列表，页码: {}, 大小: {}", userId, page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<ArticleResponse> responsePage = articleService.getUserArticles(userId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取用户文章列表失败", e);
            return Result.error("获取用户文章列表失败: " + e.getMessage());
        }
    }

    /**
     * 点赞文章
     */
    @PostMapping("/{id}/like")
    @ApiOperation(value = "点赞文章", notes = "用户点赞文章")
    public Result<Void> likeArticle(
            @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 点赞文章 {}", userId, id);
            articleService.likeArticle(id, userId);
            return Result.success("点赞成功");
        } catch (Exception e) {
            log.error("点赞文章失败", e);
            return Result.error("点赞失败: " + e.getMessage());
        }
    }

    /**
     * 取消点赞文章
     */
    @DeleteMapping("/{id}/like")
    @ApiOperation(value = "取消点赞文章", notes = "用户取消点赞文章")
    public Result<Void> unlikeArticle(
            @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 取消点赞文章 {}", userId, id);
            articleService.unlikeArticle(id, userId);
            return Result.success("取消点赞成功");
        } catch (Exception e) {
            log.error("取消点赞文章失败", e);
            return Result.error("取消点赞失败: " + e.getMessage());
        }
    }
}
