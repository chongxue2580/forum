package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.article.ArticleResponse;
import com.example.blog_froum.dto.question.QuestionResponse;
import com.example.blog_froum.dto.search.SearchAllResponse;
import com.example.blog_froum.dto.search.SearchSuggestionsResponse;
import com.example.blog_froum.service.ArticleService;
import com.example.blog_froum.service.QuestionService;
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

import java.util.Arrays;

/**
 * 搜索控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "搜索功能")
public class SearchController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private QuestionService questionService;

    /**
     * 搜索文章
     */
    @GetMapping("/articles")
    @ApiOperation(value = "搜索文章", notes = "根据关键词搜索文章")
    public Result<Page<ArticleResponse>> searchArticles(
            @ApiParam(value = "搜索关键词", required = true) @RequestParam String keyword,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            log.info("搜索文章，关键词: {}, 页码: {}, 每页数量: {}", keyword, page, pageSize);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<ArticleResponse> responsePage = articleService.searchArticles(keyword, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("搜索文章失败", e);
            return Result.error("搜索文章失败: " + e.getMessage());
        }
    }

    /**
     * 搜索问答
     */
    @GetMapping("/questions")
    @ApiOperation(value = "搜索问答", notes = "根据关键词搜索问答")
    public Result<Page<QuestionResponse>> searchQuestions(
            @ApiParam(value = "搜索关键词", required = true) @RequestParam String keyword,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            log.info("搜索问答，关键词: {}, 页码: {}, 每页数量: {}", keyword, page, pageSize);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<QuestionResponse> responsePage = questionService.searchQuestions(keyword, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("搜索问答失败", e);
            return Result.error("搜索问答失败: " + e.getMessage());
        }
    }

    /**
     * 综合搜索
     */
    @GetMapping("/all")
    @ApiOperation(value = "综合搜索", notes = "同时搜索文章和问答")
    public Result<SearchAllResponse> searchAll(
            @ApiParam(value = "搜索关键词", required = true) @RequestParam String keyword,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "5") @RequestParam(defaultValue = "5") int pageSize) {
        try {
            log.info("综合搜索，关键词: {}, 页码: {}, 每页数量: {}", keyword, page, pageSize);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            
            // 搜索文章
            Page<ArticleResponse> articlePage = articleService.searchArticles(keyword, pageable);
            
            // 搜索问答
            Page<QuestionResponse> questionPage = questionService.searchQuestions(keyword, pageable);
            
            SearchAllResponse result = new SearchAllResponse(articlePage, questionPage, keyword);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("综合搜索失败", e);
            return Result.error("综合搜索失败: " + e.getMessage());
        }
    }

    /**
     * 搜索建议
     */
    @GetMapping("/suggestions")
    @ApiOperation(value = "搜索建议", notes = "根据关键词提供搜索建议")
    public Result<SearchSuggestionsResponse> getSearchSuggestions(
            @ApiParam(value = "搜索关键词", required = true) @RequestParam String keyword) {
        try {
            log.info("获取搜索建议，关键词: {}", keyword);
            
            // 这里可以实现更复杂的搜索建议逻辑
            // 比如基于历史搜索、热门搜索等
            SearchSuggestionsResponse suggestions = new SearchSuggestionsResponse(
                    keyword,
                    Arrays.asList(
                            keyword + " 教程",
                            keyword + " 实践",
                            keyword + " 问题",
                            keyword + " 解决方案"));
            
            return Result.success(suggestions);
        } catch (Exception e) {
            log.error("获取搜索建议失败", e);
            return Result.error("获取搜索建议失败: " + e.getMessage());
        }
    }
}
