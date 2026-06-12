package com.example.blog_froum.controller.admin;

import com.example.blog_froum.dto.admin.AdminDashboardResponses;
import com.example.blog_froum.dto.statistics.ArticleStatisticsResponse;
import com.example.blog_froum.dto.statistics.CommentStatisticsResponse;
import com.example.blog_froum.dto.statistics.QuestionStatisticsResponse;
import com.example.blog_froum.dto.statistics.UserStatisticsResponse;
import com.example.blog_froum.entity.Article;
import com.example.blog_froum.entity.Comment;
import com.example.blog_froum.entity.Question;
import com.example.blog_froum.entity.User;
import com.example.blog_froum.mapper.UserMapper;
import com.example.blog_froum.repository.ArticleRepository;
import com.example.blog_froum.repository.CommentRepository;
import com.example.blog_froum.repository.QuestionRepository;
import com.example.blog_froum.service.ArticleService;
import com.example.blog_froum.service.CommentService;
import com.example.blog_froum.service.QuestionService;
import com.example.blog_froum.service.UserService;
import com.example.blog_froum.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员仪表板控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "管理员仪表板")
public class AdminDashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CommentRepository commentRepository;

    /**
     * 获取仪表板概览数据
     */
    @GetMapping("/overview")
    @ApiOperation(value = "获取仪表板概览", notes = "获取管理员仪表板的概览统计数据")
    public Result<AdminDashboardResponses.Overview> getDashboardOverview() {
        try {
            log.info("管理员获取仪表板概览数据");

            // 获取各模块统计数据
            UserStatisticsResponse userStats = userService.getUserStatistics();
            ArticleStatisticsResponse articleStats = articleService.getArticleStatistics();
            QuestionStatisticsResponse questionStats = questionService.getQuestionStatistics();
            CommentStatisticsResponse commentStats = commentService.getCommentStatistics();

            AdminDashboardResponses.OverviewSystemInfo systemInfo =
                    new AdminDashboardResponses.OverviewSystemInfo(
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            "1.0.0",
                            "development");
            AdminDashboardResponses.Overview overview =
                    new AdminDashboardResponses.Overview(userStats, articleStats, questionStats, commentStats, systemInfo);

            return Result.success(overview);
        } catch (Exception e) {
            log.error("获取仪表板概览数据失败", e);
            return Result.error("获取概览数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取今日统计数据
     */
    @GetMapping("/today")
    @ApiOperation(value = "获取今日统计", notes = "获取今日的各项统计数据")
    public Result<AdminDashboardResponses.TodayStatistics> getTodayStatistics() {
        try {
            log.info("管理员获取今日统计数据");

            LocalDateTime start = LocalDate.now().atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            AdminDashboardResponses.TodayStatistics todayStats =
                    new AdminDashboardResponses.TodayStatistics(
                            (int) userMapper.countCreatedBetween(start, end),
                            (int) articleRepository.countByCreatedAtBetween(start, end),
                            (int) questionRepository.countByCreatedAtBetween(start, end),
                            (int) commentRepository.countByCreatedAtBetweenAndIsDeletedFalse(start, end),
                            (int) userMapper.countLastLoginBetween(start, end));

            return Result.success(todayStats);
        } catch (Exception e) {
            log.error("获取今日统计数据失败", e);
            return Result.error("获取今日统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近活动
     */
    @GetMapping("/recent-activities")
    @ApiOperation(value = "获取最近活动", notes = "获取系统最近的活动记录")
    public Result<AdminDashboardResponses.RecentActivities> getRecentActivities() {
        try {
            log.info("管理员获取最近活动");

            List<AdminDashboardResponses.Activity> activities = new ArrayList<>();

            userMapper.findRecentUsers(0, 5).forEach(user ->
                    activities.add(new AdminDashboardResponses.Activity(
                            "USER_REGISTER",
                            "注册了账号",
                            displayUserName(user),
                            null,
                            formatActivityTime(user.getCreatedAt()))));

            articleRepository.findTop5ByOrderByCreatedAtDesc().forEach(article ->
                    activities.add(new AdminDashboardResponses.Activity(
                            "ARTICLE_PUBLISH",
                            "发布了文章",
                            displayUserName(article.getAuthorId()),
                            article.getTitle(),
                            formatActivityTime(article.getCreatedAt()))));

            questionRepository.findTop5ByOrderByCreatedAtDesc().forEach(question ->
                    activities.add(new AdminDashboardResponses.Activity(
                            "QUESTION_ASK",
                            "提出了问题",
                            displayUserName(question.getAuthorId()),
                            question.getTitle(),
                            formatActivityTime(question.getCreatedAt()))));

            commentRepository.findTop5ByIsDeletedFalseOrderByCreatedAtDesc().forEach(comment ->
                    activities.add(new AdminDashboardResponses.Activity(
                            "COMMENT_POST",
                            "发表了评论",
                            displayUserName(comment.getUserId()),
                            summarize(comment.getContent()),
                            formatActivityTime(comment.getCreatedAt()))));

            List<AdminDashboardResponses.Activity> activityList = activities.stream()
                    .filter(activity -> activity.getTime() != null)
                    .sorted(Comparator.comparing((AdminDashboardResponses.Activity activity) ->
                            parseActivityTime(activity.getTime())).reversed())
                    .limit(10)
                    .collect(Collectors.toList());

            AdminDashboardResponses.RecentActivities recentActivities =
                    new AdminDashboardResponses.RecentActivities(activityList, activityList.size());

            return Result.success(recentActivities);
        } catch (Exception e) {
            log.error("获取最近活动失败", e);
            return Result.error("获取最近活动失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统状态
     */
    @GetMapping("/system-status")
    @ApiOperation(value = "获取系统状态", notes = "获取系统运行状态信息")
    public Result<AdminDashboardResponses.SystemStatus> getSystemStatus() {
        try {
            log.info("管理员获取系统状态");

            // 获取JVM信息
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();

            AdminDashboardResponses.MemoryInfo memoryInfo =
                    new AdminDashboardResponses.MemoryInfo(
                            totalMemory / 1024 / 1024 + " MB",
                            usedMemory / 1024 / 1024 + " MB",
                            freeMemory / 1024 / 1024 + " MB",
                            maxMemory / 1024 / 1024 + " MB",
                            Math.round((double) usedMemory / totalMemory * 100));

            AdminDashboardResponses.RuntimeInfo systemInfo =
                    new AdminDashboardResponses.RuntimeInfo(
                            System.getProperty("java.version"),
                            System.getProperty("os.name"),
                            System.getProperty("os.version"),
                            runtime.availableProcessors());

            AdminDashboardResponses.ApplicationInfo appInfo =
                    new AdminDashboardResponses.ApplicationInfo("running", "2 days 5 hours", "1.0.0", "development");

            AdminDashboardResponses.SystemStatus systemStatus =
                    new AdminDashboardResponses.SystemStatus(memoryInfo, systemInfo, appInfo);

            return Result.success(systemStatus);
        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            return Result.error("获取系统状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据趋势
     */
    @GetMapping("/trends")
    @ApiOperation(value = "获取数据趋势", notes = "获取各项数据的趋势图表数据")
    public Result<AdminDashboardResponses.DataTrends> getDataTrends() {
        try {
            log.info("管理员获取数据趋势");

            List<AdminDashboardResponses.TrendPoint> userTrend = new ArrayList<>();
            List<AdminDashboardResponses.TrendPoint> articleTrend = new ArrayList<>();
            List<AdminDashboardResponses.TrendPoint> questionTrend = new ArrayList<>();

            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = start.plusDays(1);

                userTrend.add(new AdminDashboardResponses.TrendPoint(
                        date.format(DateTimeFormatter.ofPattern("MM-dd")),
                        (int) userMapper.countCreatedBetween(start, end)));
                articleTrend.add(new AdminDashboardResponses.TrendPoint(
                        date.format(DateTimeFormatter.ofPattern("MM-dd")),
                        (int) articleRepository.countByCreatedAtBetween(start, end)));
                questionTrend.add(new AdminDashboardResponses.TrendPoint(
                        date.format(DateTimeFormatter.ofPattern("MM-dd")),
                        (int) questionRepository.countByCreatedAtBetween(start, end)));
            }

            AdminDashboardResponses.DataTrends trends =
                    new AdminDashboardResponses.DataTrends(userTrend, articleTrend, questionTrend);

            return Result.success(trends);
        } catch (Exception e) {
            log.error("获取数据趋势失败", e);
            return Result.error("获取数据趋势失败: " + e.getMessage());
        }
    }

    private String displayUserName(Long userId) {
        if (userId == null) {
            return "系统";
        }
        User user = userMapper.findById(userId);
        return displayUserName(user);
    }

    private String displayUserName(User user) {
        if (user == null) {
            return "未知用户";
        }
        if (user.getNickname() != null && !user.getNickname().trim().isEmpty()) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private String summarize(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "评论内容";
        }
        String trimmed = content.trim();
        return trimmed.length() > 40 ? trimmed.substring(0, 40) + "..." : trimmed;
    }

    private String formatActivityTime(LocalDateTime time) {
        return time == null ? null : time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private LocalDateTime parseActivityTime(String value) {
        return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
