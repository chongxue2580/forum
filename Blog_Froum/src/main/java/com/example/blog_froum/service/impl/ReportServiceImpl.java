package com.example.blog_froum.service.impl;

import com.example.blog_froum.dto.report.ReportCreateRequest;
import com.example.blog_froum.dto.report.ReportHandleRequest;
import com.example.blog_froum.dto.report.ReportResponse;
import com.example.blog_froum.dto.report.ReportStatisticsResponse;
import com.example.blog_froum.entity.Article;
import com.example.blog_froum.entity.Comment;
import com.example.blog_froum.entity.Question;
import com.example.blog_froum.entity.Report;
import com.example.blog_froum.entity.User;
import com.example.blog_froum.mapper.UserMapper;
import com.example.blog_froum.repository.ArticleRepository;
import com.example.blog_froum.repository.CommentRepository;
import com.example.blog_froum.repository.QuestionRepository;
import com.example.blog_froum.repository.ReportRepository;
import com.example.blog_froum.service.ReportService;
import com.example.blog_froum.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final Set<String> SUPPORTED_TARGET_TYPES = new HashSet<>(
            Arrays.asList("ARTICLE", "QUESTION", "COMMENT", "USER"));
    private static final Set<String> SUPPORTED_STATUSES = new HashSet<>(
            Arrays.asList(STATUS_PENDING, STATUS_RESOLVED, STATUS_REJECTED));
    private static final Set<String> HANDLE_STATUSES = new HashSet<>(
            Arrays.asList(STATUS_RESOLVED, STATUS_REJECTED));

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Override
    public ReportResponse createReport(Long reporterId, ReportCreateRequest request) {
        log.info("用户 {} 提交举报，目标类型: {}, 目标ID: {}", reporterId, request.getTargetType(), request.getTargetId());

        User reporter = userMapper.findById(reporterId);
        if (reporter == null) {
            throw new RuntimeException("举报用户不存在");
        }
        userService.assertCanUseAccount(reporterId);

        String targetType = normalizeTargetType(request.getTargetType());
        TargetSnapshot target = resolveTarget(targetType, request.getTargetId());

        if (target.getOwnerId() != null && target.getOwnerId().equals(reporterId)) {
            throw new RuntimeException("不能举报自己的内容");
        }

        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporterId, targetType, request.getTargetId(), STATUS_PENDING)) {
            throw new RuntimeException("该目标已有待处理举报，请勿重复提交");
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(request.getTargetId());
        report.setTargetTitle(truncate(target.getTitle(), 255));
        report.setTargetOwnerId(target.getOwnerId());
        report.setReason(trimToLength(request.getReason(), 120));
        report.setDescription(trimToNull(request.getDescription()));
        report.setStatus(STATUS_PENDING);

        Report savedReport = reportRepository.save(report);
        savedReport.setReporter(reporter);
        log.info("举报提交成功，举报ID: {}", savedReport.getId());
        return ReportResponse.fromEntity(savedReport);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getUserReports(Long reporterId, String status, Pageable pageable) {
        String normalizedStatus = normalizeStatusFilter(status);
        return reportRepository.findByReporterForUser(reporterId, normalizedStatus, pageable)
                .map(ReportResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getUserReport(Long reporterId, Long reportId) {
        Report report = reportRepository.findByIdWithUsers(reportId)
                .orElseThrow(() -> new RuntimeException("举报记录不存在"));
        if (!reporterId.equals(report.getReporterId())) {
            throw new RuntimeException("无权限查看该举报记录");
        }
        return ReportResponse.fromEntity(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> searchReportsForAdmin(String targetType,
                                                       String status,
                                                       String keyword,
                                                       LocalDateTime startTime,
                                                       LocalDateTime endTime,
                                                       Pageable pageable) {
        String normalizedTargetType = normalizeTargetTypeFilter(targetType);
        String normalizedStatus = normalizeStatusFilter(status);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return reportRepository
                .searchForAdmin(normalizedTargetType, normalizedStatus, normalizedKeyword, startTime, endTime, pageable)
                .map(ReportResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportForAdmin(Long reportId) {
        Report report = reportRepository.findByIdWithUsers(reportId)
                .orElseThrow(() -> new RuntimeException("举报记录不存在"));
        return ReportResponse.fromEntity(report);
    }

    @Override
    public ReportResponse handleReport(Long reportId, Long handlerId, ReportHandleRequest request) {
        Report report = reportRepository.findByIdWithUsers(reportId)
                .orElseThrow(() -> new RuntimeException("举报记录不存在"));

        User handler = userMapper.findById(handlerId);
        if (handler == null) {
            throw new RuntimeException("处理人不存在");
        }
        userService.assertCanUseAccount(handlerId);

        String status = normalizeHandleStatus(request.getStatus());
        report.setStatus(status);
        report.setHandlerId(handlerId);
        report.setHandler(handler);
        report.setHandlerNote(trimToNull(request.getHandlerNote()));
        report.setHandledAt(LocalDateTime.now());

        Report savedReport = reportRepository.save(report);
        log.info("举报处理完成，举报ID: {}, 状态: {}", savedReport.getId(), savedReport.getStatus());
        return ReportResponse.fromEntity(savedReport);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportStatisticsResponse getReportStatistics() {
        return new ReportStatisticsResponse(
                reportRepository.count(),
                reportRepository.countByStatus(STATUS_PENDING),
                reportRepository.countByStatus(STATUS_RESOLVED),
                reportRepository.countByStatus(STATUS_REJECTED));
    }

    private TargetSnapshot resolveTarget(String targetType, Long targetId) {
        if ("ARTICLE".equals(targetType)) {
            Article article = articleRepository.findById(targetId)
                    .orElseThrow(() -> new RuntimeException("文章不存在"));
            return new TargetSnapshot(article.getTitle(), article.getAuthorId());
        }

        if ("QUESTION".equals(targetType)) {
            Question question = questionRepository.findById(targetId)
                    .orElseThrow(() -> new RuntimeException("问答不存在"));
            return new TargetSnapshot(question.getTitle(), question.getAuthorId());
        }

        if ("COMMENT".equals(targetType)) {
            Comment comment = commentRepository.findById(targetId)
                    .orElseThrow(() -> new RuntimeException("评论不存在"));
            if (Boolean.TRUE.equals(comment.getIsDeleted())) {
                throw new RuntimeException("评论不存在");
            }
            return new TargetSnapshot(buildCommentTitle(comment), comment.getUserId());
        }

        if ("USER".equals(targetType)) {
            User user = userMapper.findById(targetId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            return new TargetSnapshot(resolveUserDisplayName(user), user.getId());
        }

        throw new RuntimeException("不支持的举报目标类型");
    }

    private String normalizeTargetType(String targetType) {
        String normalized = StringUtils.hasText(targetType) ? targetType.trim().toUpperCase() : null;
        if (!SUPPORTED_TARGET_TYPES.contains(normalized)) {
            throw new RuntimeException("不支持的举报目标类型");
        }
        return normalized;
    }

    private String normalizeTargetTypeFilter(String targetType) {
        if (!StringUtils.hasText(targetType)) {
            return null;
        }
        return normalizeTargetType(targetType);
    }

    private String normalizeStatusFilter(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!SUPPORTED_STATUSES.contains(normalized)) {
            throw new RuntimeException("不支持的举报状态");
        }
        return normalized;
    }

    private String normalizeHandleStatus(String status) {
        String normalized = StringUtils.hasText(status) ? status.trim().toUpperCase() : null;
        if (!HANDLE_STATUSES.contains(normalized)) {
            throw new RuntimeException("处理状态只能为 RESOLVED 或 REJECTED");
        }
        return normalized;
    }

    private String buildCommentTitle(Comment comment) {
        String content = comment.getContent() == null ? "" : comment.getContent().replaceAll("\\s+", " ").trim();
        if (content.isEmpty()) {
            return "评论 #" + comment.getId();
        }
        return truncate(content, 80);
    }

    private String resolveUserDisplayName(User user) {
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        return user.getUsername();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimToLength(String value, int maxLength) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return truncate(trimmed, maxLength);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    @Data
    @AllArgsConstructor
    private static class TargetSnapshot {
        private String title;
        private Long ownerId;
    }
}
