package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.question.QuestionCreateRequest;
import com.example.blog_froum.dto.question.QuestionResponse;
import com.example.blog_froum.dto.question.QuestionUpdateRequest;
import com.example.blog_froum.service.QuestionService;
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

/**
 * 问答控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "问答管理")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    /**
     * 创建问答
     */
    @PostMapping
    @ApiOperation(value = "创建问答", notes = "用户创建新的问答")
    public Result<QuestionResponse> createQuestion(
            @Valid @RequestBody QuestionCreateRequest request) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 创建问答", userId);
            QuestionResponse response = questionService.createQuestion(userId, request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("创建问答失败", e);
            return Result.error("创建问答失败: " + e.getMessage());
        }
    }

    /**
     * 更新问答
     */
    @PutMapping("/{id}")
    @ApiOperation(value = "更新问答", notes = "用户更新自己的问答")
    public Result<QuestionResponse> updateQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id,
            @Valid @RequestBody QuestionUpdateRequest request) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 更新问答 {}", userId, id);
            QuestionResponse response = questionService.updateQuestion(id, userId, request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("更新问答失败", e);
            return Result.error("更新问答失败: " + e.getMessage());
        }
    }

    /**
     * 删除问答
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除问答", notes = "用户删除自己的问答")
    public Result<Void> deleteQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 删除问答 {}", userId, id);
            questionService.deleteQuestion(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除问答失败", e);
            return Result.error("删除问答失败: " + e.getMessage());
        }
    }

    /**
     * 获取问答详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "获取问答详情", notes = "根据ID获取问答详情")
    public Result<QuestionResponse> getQuestionById(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id) {
        try {
            log.info("获取问答详情，ID: {}", id);
            
            // 增加浏览量
            questionService.incrementViewCount(id);
            
            QuestionResponse response = questionService.getQuestionById(id);
            return Result.success(response);
        } catch (Exception e) {
            log.error("获取问答详情失败", e);
            return Result.error("获取问答详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取问答列表
     */
    @GetMapping
    @ApiOperation(value = "获取问答列表", notes = "分页获取已批准的问答列表")
    public Result<Page<QuestionResponse>> getQuestions(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "标签ID") @RequestParam(required = false) Long tagId,
            @ApiParam(value = "是否已解决") @RequestParam(required = false) Boolean solved,
            @ApiParam(value = "关键词") @RequestParam(required = false) String keyword,
            @ApiParam(value = "排序：newest/hottest/mostAnswers/mostLiked") @RequestParam(defaultValue = "newest") String sort) {
        try {
            log.info("获取问答列表，页码: {}, 每页数量: {}, 标签: {}, 已解决: {}, 关键词: {}, 排序: {}",
                    page, pageSize, tagId, solved, keyword, sort);
            Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));
            Page<QuestionResponse> responsePage = questionService.searchPublicQuestions(tagId, solved, keyword, sort, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取问答列表失败", e);
            return Result.error("获取问答列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门问答
     */
    @GetMapping("/popular")
    @ApiOperation(value = "获取热门问答", notes = "按点赞数排序获取热门问答")
    public Result<Page<QuestionResponse>> getPopularQuestions(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            log.info("获取热门问答，页码: {}, 每页数量: {}", page, pageSize);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<QuestionResponse> responsePage = questionService.getPopularQuestions(pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取热门问答失败", e);
            return Result.error("获取热门问答失败: " + e.getMessage());
        }
    }

    /**
     * 获取已解决的问答
     */
    @GetMapping("/solved")
    @ApiOperation(value = "获取已解决问答", notes = "获取已解决的问答列表")
    public Result<Page<QuestionResponse>> getSolvedQuestions(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            log.info("获取已解决问答，页码: {}, 每页数量: {}", page, pageSize);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<QuestionResponse> responsePage = questionService.getSolvedQuestions(pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取已解决问答失败", e);
            return Result.error("获取已解决问答失败: " + e.getMessage());
        }
    }

    /**
     * 获取未解决的问答
     */
    @GetMapping("/unsolved")
    @ApiOperation(value = "获取未解决问答", notes = "获取未解决的问答列表")
    public Result<Page<QuestionResponse>> getUnsolvedQuestions(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            log.info("获取未解决问答，页码: {}, 每页数量: {}", page, pageSize);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<QuestionResponse> responsePage = questionService.getUnsolvedQuestions(pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取未解决问答失败", e);
            return Result.error("获取未解决问答失败: " + e.getMessage());
        }
    }

    /**
     * 获取置顶问答
     */
    @GetMapping("/pinned")
    @ApiOperation(value = "获取置顶问答", notes = "获取置顶的问答列表")
    public Result<List<QuestionResponse>> getPinnedQuestions() {
        try {
            log.info("获取置顶问答");
            List<QuestionResponse> responseList = questionService.getPinnedQuestions();
            return Result.success(responseList);
        } catch (Exception e) {
            log.error("获取置顶问答失败", e);
            return Result.error("获取置顶问答失败: " + e.getMessage());
        }
    }

    /**
     * 获取精选问答
     */
    @GetMapping("/featured")
    @ApiOperation(value = "获取精选问答", notes = "获取精选的问答列表")
    public Result<Page<QuestionResponse>> getFeaturedQuestions(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            log.info("获取精选问答，页码: {}, 每页数量: {}", page, pageSize);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<QuestionResponse> responsePage = questionService.getFeaturedQuestions(pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取精选问答失败", e);
            return Result.error("获取精选问答失败: " + e.getMessage());
        }
    }

    /**
     * 搜索问答
     */
    @GetMapping("/search")
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
     * 点赞问答
     */
    @PostMapping("/{id}/like")
    @ApiOperation(value = "点赞问答", notes = "用户点赞问答")
    public Result<Void> likeQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 点赞问答 {}", userId, id);
            questionService.likeQuestion(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("点赞问答失败", e);
            return Result.error("点赞失败: " + e.getMessage());
        }
    }

    /**
     * 取消点赞问答
     */
    @DeleteMapping("/{id}/like")
    @ApiOperation(value = "取消点赞问答", notes = "用户取消点赞问答")
    public Result<Void> unlikeQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 取消点赞问答 {}", userId, id);
            questionService.unlikeQuestion(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消点赞问答失败", e);
            return Result.error("取消点赞失败: " + e.getMessage());
        }
    }

    /**
     * 关注问答
     */
    @PostMapping("/{id}/follow")
    @ApiOperation(value = "关注问答", notes = "用户关注问答")
    public Result<Void> followQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 关注问答 {}", userId, id);
            questionService.followQuestion(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("关注问答失败", e);
            return Result.error("关注失败: " + e.getMessage());
        }
    }

    /**
     * 取消关注问答
     */
    @DeleteMapping("/{id}/follow")
    @ApiOperation(value = "取消关注问答", notes = "用户取消关注问答")
    public Result<Void> unfollowQuestion(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 取消关注问答 {}", userId, id);
            questionService.unfollowQuestion(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消关注问答失败", e);
            return Result.error("取消关注失败: " + e.getMessage());
        }
    }

    /**
     * 标记问答为已解决
     */
    @PostMapping("/{id}/solve")
    @ApiOperation(value = "标记问答为已解决", notes = "问答作者标记问答为已解决")
    public Result<Void> markQuestionAsSolved(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 标记问答 {} 为已解决", userId, id);
            questionService.markQuestionAsSolved(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("标记问答为已解决失败", e);
            return Result.error("标记失败: " + e.getMessage());
        }
    }

    /**
     * 标记问答为未解决
     */
    @PostMapping("/{id}/unsolve")
    @ApiOperation(value = "标记问答为未解决", notes = "问答作者标记问答为未解决")
    public Result<Void> markQuestionAsUnsolved(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 标记问答 {} 为未解决", userId, id);
            questionService.markQuestionAsUnsolved(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("标记问答为未解决失败", e);
            return Result.error("标记失败: " + e.getMessage());
        }
    }

    /**
     * 设置最佳答案
     */
    @PostMapping("/{id}/best-answer/{commentId}")
    @ApiOperation(value = "设置最佳答案", notes = "问答作者将一个顶层回答设置为最佳答案")
    public Result<QuestionResponse> setBestAnswer(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id,
            @ApiParam(value = "回答ID", required = true) @PathVariable Long commentId) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 将回答 {} 设置为问答 {} 的最佳答案", userId, commentId, id);
            QuestionResponse response = questionService.setBestAnswer(id, commentId, userId);
            return Result.success(response);
        } catch (Exception e) {
            log.error("设置最佳答案失败", e);
            return Result.error("设置最佳答案失败: " + e.getMessage());
        }
    }

    /**
     * 取消最佳答案
     */
    @DeleteMapping("/{id}/best-answer")
    @ApiOperation(value = "取消最佳答案", notes = "问答作者取消当前最佳答案")
    public Result<QuestionResponse> unsetBestAnswer(
            @ApiParam(value = "问答ID", required = true) @PathVariable Long id) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("用户 {} 取消问答 {} 的最佳答案", userId, id);
            QuestionResponse response = questionService.unsetBestAnswer(id, userId);
            return Result.success(response);
        } catch (Exception e) {
            log.error("取消最佳答案失败", e);
            return Result.error("取消最佳答案失败: " + e.getMessage());
        }
    }

    /**
     * 获取我的问答
     */
    @GetMapping("/my")
    @ApiOperation(value = "获取我的问答", notes = "获取当前用户的问答列表")
    public Result<Page<QuestionResponse>> getMyQuestions(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("获取用户 {} 的问答列表", userId);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<QuestionResponse> responsePage = questionService.getUserQuestions(userId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取用户问答列表失败", e);
            return Result.error("获取用户问答列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取我关注的问答
     */
    @GetMapping("/followed")
    @ApiOperation(value = "获取我关注的问答", notes = "获取当前用户关注的问答列表")
    public Result<Page<QuestionResponse>> getFollowedQuestions(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            log.info("获取用户 {} 关注的问答列表", userId);
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<QuestionResponse> responsePage = questionService.getFollowedQuestions(userId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取用户关注的问答列表失败", e);
            return Result.error("获取用户关注的问答列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定用户的问答列表
     */
    @GetMapping("/user/{userId}")
    @ApiOperation(value = "获取指定用户的问答", notes = "获取指定用户发布的问答列表")
    public Result<Page<QuestionResponse>> getUserQuestions(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("获取用户 {} 的问答列表，页码: {}, 大小: {}", userId, page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<QuestionResponse> responsePage = questionService.getUserQuestions(userId, pageable);
            return Result.success(responsePage);
        } catch (Exception e) {
            log.error("获取用户问答列表失败", e);
            return Result.error("获取用户问答列表失败: " + e.getMessage());
        }
    }
}
