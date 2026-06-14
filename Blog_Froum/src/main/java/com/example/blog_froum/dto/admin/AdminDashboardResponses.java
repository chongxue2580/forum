package com.example.blog_froum.dto.admin;

import com.example.blog_froum.dto.statistics.ArticleStatisticsResponse;
import com.example.blog_froum.dto.statistics.CommentStatisticsResponse;
import com.example.blog_froum.dto.statistics.QuestionStatisticsResponse;
import com.example.blog_froum.dto.statistics.UserStatisticsResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理端仪表板响应类型集合，替代动态键值组合响应。
 */
public final class AdminDashboardResponses {

    private AdminDashboardResponses() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overview {
        private UserStatisticsResponse users;
        private ArticleStatisticsResponse articles;
        private QuestionStatisticsResponse questions;
        private CommentStatisticsResponse comments;
        private OverviewSystemInfo system;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewSystemInfo {
        private String currentTime;
        private String version;
        private String environment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayStatistics {
        private int newUsers;
        private int newArticles;
        private int newQuestions;
        private int newComments;
        private int activeUsers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivities {
        private List<Activity> activities;
        private int total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Activity {
        private String type;
        private String description;
        private String user;
        private String title;
        private String time;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemStatus {
        private MemoryInfo memory;
        private RuntimeInfo system;
        private ApplicationInfo application;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryInfo {
        private String total;
        private String used;
        private String free;
        private String max;
        private long usagePercent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuntimeInfo {
        private String javaVersion;
        private String osName;
        private String osVersion;
        private int processors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationInfo {
        private String status;
        private String uptime;
        private String startTime;
        private String version;
        private String environment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataTrends {
        private List<TrendPoint> userRegistrations;
        private List<TrendPoint> articlePublications;
        private List<TrendPoint> questionAsks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String date;
        private int count;
    }
}
