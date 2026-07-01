package com.example.blog_froum.dto.report;

import com.example.blog_froum.entity.Report;
import com.example.blog_froum.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private Long id;
    private Long reporterId;
    private String reporterName;
    private String targetType;
    private Long targetId;
    private String targetTitle;
    private Long targetOwnerId;
    private String reason;
    private String description;
    private String status;
    private Long handlerId;
    private String handlerName;
    private String handlerNote;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReportResponse fromEntity(Report report) {
        Objects.requireNonNull(report, "report");

        return ReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporterId())
                .reporterName(resolveDisplayName(report.getReporter()))
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .targetTitle(report.getTargetTitle())
                .targetOwnerId(report.getTargetOwnerId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .handlerId(report.getHandlerId())
                .handlerName(resolveDisplayName(report.getHandler()))
                .handlerNote(report.getHandlerNote())
                .handledAt(report.getHandledAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private static String resolveDisplayName(User user) {
        if (user == null) {
            return null;
        }
        if (user.getNickname() != null && !user.getNickname().trim().isEmpty()) {
            return user.getNickname();
        }
        return user.getUsername();
    }
}
