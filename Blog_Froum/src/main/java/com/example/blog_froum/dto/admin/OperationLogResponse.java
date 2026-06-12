package com.example.blog_froum.dto.admin;

import com.example.blog_froum.entity.OperationLog;
import com.example.blog_froum.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OperationLogResponse {
    private Long id;
    private Operator operator;
    private String type;
    private String action;
    private Target target;
    private String detail;
    private String ip;
    private String userAgent;
    private LocalDateTime createTime;

    public static OperationLogResponse fromEntity(OperationLog log) {
        User user = log.getUser();
        String fallbackName = log.getUserId() == null ? "系统" : "用户" + log.getUserId();

        return OperationLogResponse.builder()
                .id(log.getId())
                .operator(Operator.builder()
                        .id(log.getUserId())
                        .name(user == null ? fallbackName : firstNonBlank(user.getNickname(), user.getUsername(), fallbackName))
                        .username(user == null ? null : user.getUsername())
                        .build())
                .type(log.getOperationType())
                .action(firstNonBlank(log.getOperationAction(), inferAction(log.getOperationType())))
                .target(Target.builder()
                        .type(log.getTargetType())
                        .id(log.getTargetId())
                        .name(firstNonBlank(log.getTargetName(), log.getTargetId() == null ? null : String.valueOf(log.getTargetId())))
                        .build())
                .detail(log.getOperationDesc())
                .ip(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createTime(log.getCreatedAt())
                .build();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static String inferAction(String type) {
        if (type == null) {
            return "other";
        }
        String normalized = type.toLowerCase();
        if (normalized.contains("approve")) return "approve";
        if (normalized.contains("reject")) return "reject";
        if (normalized.contains("delete")) return "delete";
        if (normalized.contains("create")) return "create";
        if (normalized.contains("update")) return "update";
        if (normalized.contains("login")) return "login";
        return "other";
    }

    @Data
    @Builder
    public static class Operator {
        private Long id;
        private String name;
        private String username;
    }

    @Data
    @Builder
    public static class Target {
        private String type;
        private Long id;
        private String name;
    }
}
