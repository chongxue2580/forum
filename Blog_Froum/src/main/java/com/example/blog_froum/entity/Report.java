package com.example.blog_froum.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_reports_reporter", columnList = "reporter_id"),
        @Index(name = "idx_reports_target", columnList = "target_type,target_id"),
        @Index(name = "idx_reports_status", columnList = "status"),
        @Index(name = "idx_reports_created", columnList = "created_at")
})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", insertable = false, updatable = false)
    private User reporter;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "target_title", length = 255)
    private String targetTitle;

    @Column(name = "target_owner_id")
    private Long targetOwnerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_owner_id", insertable = false, updatable = false)
    private User targetOwner;

    @Column(nullable = false, length = 120)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "handler_id")
    private Long handlerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handler_id", insertable = false, updatable = false)
    private User handler;

    @Column(name = "handler_note", columnDefinition = "TEXT")
    private String handlerNote;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null || status.trim().isEmpty()) {
            status = "PENDING";
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
